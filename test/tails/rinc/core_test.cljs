(ns tails.rinc.core-test
  (:require [cljs.test :refer [deftest is use-fixtures]]
            [clojure.string :as str]
            [tails.rinc.core :as ri]
            [tails.init-specs]))


(use-fixtures :each
  (fn [test-fn]
    (ri/reg-sub :q1
                (fn q1 [db] (:item1 @db)))

    (ri/reg-sub :q2
                (fn q2 [db] (:item2 @db)))

    (ri/reg-sub :q3
                (fn q3-signal [_db]
                  [(ri/subscribe [:q1]) (ri/subscribe [:q2])])
                (fn q3 [[s1 s2]]
                  [(:sub-item1 @s1), (:sub-item1 @s2)]))

    ;; NOTE that this form (e.g. without signal-fn) creates subscriptions (reaction objects) in the body function.
    ;; Without subscription caching it will result in new reaction created for each subscription leading to waste of resources.
    (ri/reg-sub :q4
                (fn q4 [_db]
                  (let [v1 @(ri/subscribe [:q1])
                        v2 @(ri/subscribe [:q2])]
                    [(:sub-item1 v1) (:sub-item1 v2)])))

    (ri/reg-event :e1
                  (fn event1 [db event]
                    (update db :item1 conj event)))

    (ri/reg-event :e2
                  (fn event1 [db event]
                    (update db :item2 conj event)))
    
    (test-fn)

    (ri/clear-all)))


(defn named-fn? [f name]
  (and (fn? f) 
       (str/includes? (str f) (str "$" name))))

;; -- subscribe --

(deftest test-reg-sub
  (let [{[fn1 fn2] :q1} @@#'ri/!sub-registry]
    (is (named-fn? fn1 "app_db_signal"))
    (is (named-fn? fn2 "q1")))

  (let [{[fn1 fn2] :q3} @@#'ri/!sub-registry]
    (is (named-fn? fn1 "q3_signal"))
    (is (named-fn? fn2 "q3"))))

(deftest test-subscribe-unknown
  (is (thrown-with-msg? js/Error #"Unregistered subscription(.+):dummy" (ri/subscribe [:dummy]))))

(deftest test-subscribe-simple
  (let [q1-sub (ri/subscribe [:q1])]
    (is (nil? @q1-sub))

    (reset! ri/!app-db {:dummy :abc})
    (is (nil? @q1-sub))

    (reset! ri/!app-db {:item1 123})
    (is (= 123 @q1-sub))

    (reset! ri/!app-db {:item1 :abc})
    (is (= :abc @q1-sub))

    (reset! ri/!app-db {})
    (is (nil? @q1-sub))))

(deftest test-subscribe-complex
  (let [q3-sub (ri/subscribe [:q3])
        q4-sub (ri/subscribe [:q4])]
    (is (= [nil nil] @q3-sub))
    (is (= [nil nil] @q4-sub))

    (swap! ri/!app-db assoc-in [:item1 :sub-item1] 456)
    (is (= [456 nil] @q3-sub))
    (is (= [456 nil] @q4-sub))

    (swap! ri/!app-db assoc-in [:item2 :sub-item1] :xyz)
    (is (= [456 :xyz] @q3-sub))
    (is (= [456 :xyz] @q4-sub))))


;; -- events --


(deftest test-reg-event
  (let [{fn1 :e1} @@#'ri/!event-registry]
    (is (named-fn? fn1 "event1"))))

(deftest test-dispatch-unknown
  (is (thrown-with-msg? js/Error #"dispatch argument is nil" (ri/dispatch nil))))

(deftest test-process-event-unknown
  (is (thrown-with-msg? js/Error #"Unregistered event: :dummy" (#'ri/process-event {} [:dummy]))))

(deftest test-process-event
  (is (= {:item99 12, :item1 '([:e1 123 :abc])}
         (#'ri/process-event {:item99 12} [:e1 123 :abc]))))

(deftest test-run-event-queue
  (ri/dispatch [:e1 3])
  (ri/dispatch [:e1 2])
  (ri/dispatch [:e2 1])

  (is (= [[:e1 3] [:e1 2] [:e2 1]] @@#'ri/!event-queue))
  (is (= {} @ri/!app-db))

  (ri/run-event-queue)

  (is (= [] @@#'ri/!event-queue))
  (is (= {:item1 '([:e1 2] [:e1 3]), :item2 '([:e2 1])} @ri/!app-db)))


(deftest test-run-event-queue->e2e
  (ri/dispatch [:e1 32])
  (ri/dispatch [:e2 48])

  (is (nil? @(ri/subscribe [:q1])))
  (is (nil? @(ri/subscribe [:q2])))

  (ri/run-event-queue)

  (is (= '([:e1 32]) @(ri/subscribe [:q1])))
  (is (= '([:e2 48]) @(ri/subscribe [:q2]))))
