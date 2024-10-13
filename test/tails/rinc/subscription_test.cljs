(ns tails.rinc.subscription-test
  (:require [cljs.test :refer [deftest is use-fixtures]]
            [clojure.string :as str]
            [tails.rinc.reaction :as r]
            [tails.rinc.subscription :as sub]
            [tails.init-specs]))


(def !state (r/ratom {}))

(use-fixtures :each
  (fn [test-fn]
    (sub/reg-sub :q1
                 (fn root [_] !state)
                 (fn q1 [db] (:item1 @db)))

    (sub/reg-sub :q2
                 (fn root [_] !state)
                 (fn q2 [db] (:item2 @db)))

    (sub/reg-sub :q3
                 (fn q3-signal [_db]
                   [(sub/subscribe [:q1]) (sub/subscribe [:q2])])
                 (fn q3 [[s1 s2]]
                   [(:sub-item1 @s1), (:sub-item1 @s2)]))

    (sub/reg-sub :q4
                 (fn root [_] !state)
                 (fn q4 [_db]
                   (let [v1 @(sub/subscribe [:q1])
                         v2 @(sub/subscribe [:q2])]
                     [(:sub-item1 v1) (:sub-item1 v2)])))

    (test-fn)

    (sub/clear)
    (reset! !state {})))

(defn- named-fn? [f name]
  (and (fn? f)
       (str/includes? (str f) (str "$" name))))


(deftest test-reg-sub
  (let [{[fn1 fn2] :q1} @@#'sub/!sub-registry]
    (is (named-fn? fn1 "root"))
    (is (named-fn? fn2 "q1")))

  (let [{[fn1 fn2] :q3} @@#'sub/!sub-registry]
    (is (named-fn? fn1 "q3_signal"))
    (is (named-fn? fn2 "q3"))))

(deftest test-subscribe-unknown
  (is (thrown-with-msg? js/Error #"Unregistered subscription(.+):dummy" (sub/subscribe [:dummy]))))

(deftest test-subscribe-simple
  (let [q1-sub (sub/subscribe [:q1])]
    (is (nil? @q1-sub))

    (reset! !state {:dummy :abc})
    (is (nil? @q1-sub))

    (reset! !state {:item1 123})
    (is (= 123 @q1-sub))

    (reset! !state {:item1 :abc})
    (is (= :abc @q1-sub))

    (reset! !state {})
    (is (nil? @q1-sub))))

(deftest test-subscribe-complex
  (let [q3-sub (sub/subscribe [:q3])
        q4-sub (sub/subscribe [:q4])]
    (is (= [nil nil] @q3-sub))
    (is (= [nil nil] @q4-sub))

    (swap! !state assoc-in [:item1 :sub-item1] 456)
    (is (= [456 nil] @q3-sub))
    (is (= [456 nil] @q4-sub))

    (swap! !state assoc-in [:item2 :sub-item1] :xyz)
    (is (= [456 :xyz] @q3-sub))
    (is (= [456 :xyz] @q4-sub))))
