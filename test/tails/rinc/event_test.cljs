(ns tails.rinc.event-test
  (:require [cljs.test :refer [deftest is use-fixtures]]
            [clojure.string :as str]
            [tails.rinc.event :as event]
            [tails.init-specs]))

(def !state (atom {}))

(use-fixtures :each
  (fn [test-fn]
    (event/reg-event :e1 !state
                  (fn event1 [state event]
                    (update state :item1 conj event)))

    (event/reg-event :e2 !state
                  (fn event1 [state event]
                    (update state :item2 conj event)))

    (test-fn)

    (event/clear)
    (reset! !state {})))

(defn- named-fn? [f name]
  (and (fn? f)
       (str/includes? (str f) (str "$" name))))


(deftest test-reg-event
  (let [{[!s fn1] :e1} @@#'event/!event-registry]
    (is (= !s !state))
    (is (named-fn? fn1 "event1"))))

(deftest test-process-event-nil
  (is (thrown-with-msg? js/Error #"Unregistered event: nil" (#'event/process-event nil))))

(deftest test-process-event-unknown
  (is (thrown-with-msg? js/Error #"Unregistered event: :dummy" (#'event/process-event [:dummy]))))

(deftest test-process-event
  (is (= {:item1 '([:e1 123 :abc])}
         (#'event/process-event [:e1 123 :abc]))))

(deftest test-dispatch
  (event/dispatch [:e1 3])
  (event/dispatch [:e1 2])
  (event/dispatch [:e2 1])

  (is (= {:item1 '([:e1 2] [:e1 3]), :item2 '([:e2 1])} @!state)))
