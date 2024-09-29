(ns tails.rinc.reaction-test
  (:require [cljs.test :refer [deftest is]]
            [tails.rinc.reaction :as r]
            [tails.init-specs]))


(deftest test-capture-derefed
  (let [a1 (r/ratom nil)
        a2 (r/ratom nil)
        a3 (r/ratom nil)
        f (fn []
            (#'r/notify-deref! a1)
            (#'r/notify-deref! a2)
            (#'r/notify-deref! a3)
            42)
        res (#'r/capture-derefed f nil)]
    (is (= [42 #{a1 a2 a3}] res)))

  (let [a1 (r/ratom nil)
        a2 (r/ratom nil)
        f (fn []
            (#'r/notify-deref! a1)
            ;; capture-derefed resolves lazy sequences
            (lazy-seq (do (#'r/notify-deref! a2) [12])))
        res (#'r/capture-derefed f nil)]
    (is (= ['(12) #{a1 a2}] res))))

(defn- test-equality-and-hash [make-atom]
  (let [r1 (make-atom 10)
        r2 (make-atom 10)]

    (is (= r1 r1))
    (is (not= r1 r2))

    (is (= (hash r1) (hash r1)))
    (is (not= (hash r1) (hash r2)))))

(defn- test-watchable [make-atom]
  (let [^clj r1 (make-atom 10)
        !n1 (atom nil)
        !n2 (atom nil)
        wf1 (fn [_ _ old new] (reset! !n1 [old new]))
        wf2 (fn [_ _ old new] (reset! !n2 [new old]))]

    (add-watch r1 :r1 wf1)
    (add-watch r1 :r1 wf1)
    (add-watch r1 :r2 wf2)

    (is (= {:r1 wf1, :r2 wf2} (.-watches r1)))

    (#'r/notify-watches* r1 2 4)
    (is (= [2 4] @!n1))
    (is (= [4 2] @!n2))

    (remove-watch r1 :r2)

    (#'r/notify-watches* r1 5 1)
    (is (= [5 1] @!n1))
    (is (= [4 2] @!n2))

    (remove-watch r1 :r1)

    (#'r/notify-watches* r1 0 0)
    (is (= [5 1] @!n1))
    (is (= [4 2] @!n2))))


;;--------------------------------------------------------------
;; ratom


(deftest test-ratom-interfaces
  (let [make-atom (fn [state] (r/ratom state))]
    (test-equality-and-hash make-atom)
    (test-watchable make-atom)))

(deftest test-ratom-reset-and-swap
  (let [r1 (r/ratom nil)
        !n1 (atom nil)
        wf1 (fn [_ _ old new] (reset! !n1 [old new]))]

    (add-watch r1 :r1 wf1)

    (reset! r1 12)
    (is (= 12 @r1))
    (is (= [nil 12] @!n1))

    (swap! r1 + 5)
    (is (= 17 @r1))
    (is (= [12 17] @!n1))))

(deftest test-ratom-deref-capture []
  (let [r1 (r/ratom nil)
        r2 (r/ratom nil)]
    (is (= [1 #{}] (#'r/capture-derefed #(when r1 1) nil))) ;; ratom will not be captured if its not de-refed
    (is (= [12 #{r1 r2}] (#'r/capture-derefed #(do @r1 @r2 12) nil)))))


;;--------------------------------------------------------------
;; reaction


(deftest test-reaction-interfaces
  (let [make-atom (fn [state] (#'r/->Reaction :r1 nil state false nil nil))]
    (test-equality-and-hash make-atom)
    (test-watchable make-atom)))

(deftest test-reaction-deref-capture []
  (let [r1 (r/make-reaction :r1 identity)
        r2 (r/make-reaction :r2 identity)]
    (is (= [1 #{}] (#'r/capture-derefed #(when r1 1) nil))) ;; ratom will not be captured if its not de-refed
    (is (= [12 #{r1 r2}] (#'r/capture-derefed #(do @r1 @r2 12) nil)))))

(deftest test-handle-reaction-change
  (let [a1 (r/ratom 5)
        ^js r1 (#'r/->Reaction :r1 #(+ 2 @a1) nil false nil nil)
        !n1 (atom nil)]
    (add-watch r1 :r1 (fn [_ _ old new] (reset! !n1 [old new])))

    ;; identical old/new values & !dirty? -> nothing
    (._handle-change! r1 5 5)
    (is (nil? @!n1))

    ;; different old/new values & !dirty? -> reaction updates 
    (._handle-change! r1 1 9)
    (is (= 7 @r1))
    (is (= [nil 7] @!n1))
    (is (false? (.-dirty? r1)))

    (set! (.-dirty? r1) true)

    ;; different old/new values BUT dirty? is set -> nothing
    (._handle-change! r1 7 12)
    (is (= 7 @r1))
    (is (= [nil 7] @!n1))
    (is (false? (.-dirty? r1)))))

(deftest test-update-reaction-observables!
  (let [a1 (r/ratom nil)
        a2 (r/ratom nil)
        ^js r1 (#'r/->Reaction :r1 identity nil true nil nil)]
    (is (= nil (.-observables r1)))

    (._update-observables! r1 #{})
    (is (= #{} (.-observables r1)))

    (._update-observables! r1 #{a1 a2})
    (is (= #{a1 a2} (.-observables r1)))
    (is (= [r1] (keys (.-watches a1))))
    (is (= [r1] (keys (.-watches a2))))

    ;; same set of observables does not change a setup
    (._update-observables! r1 #{a2 a1})
    (is (= #{a1 a2} (.-observables r1)))
    (is (= [r1] (keys (.-watches a1))))
    (is (= [r1] (keys (.-watches a2))))

    ;; remove one
    (._update-observables! r1 #{a2})
    (is (= #{a2} (.-observables r1)))
    (is (nil? (keys (.-watches a1))))
    (is (= [r1] (keys (.-watches a2))))

    ;; add one, remove one
    (._update-observables! r1 #{a1})
    (is (= #{a1} (.-observables r1)))
    (is (= [r1] (keys (.-watches a1))))
    (is (nil? (keys (.-watches a2))))))

(deftest test-reaction-compute-state
  (let [a1 (r/ratom 4)
        ^js r1 (#'r/->Reaction :r1 #(+ 15 @a1) 10 true nil nil)]
    (is (true? (.-dirty? r1)))
    (is (nil? (.-observables r1)))

    (is (= 19 (._compute-state! r1)))

    (is (= 19 @r1))
    (is (false? (.-dirty? r1)))
    ;; _compute-state is also captures de-refs inside of reaction function
    (is (= #{a1} (.-observables r1)))))

(deftest test-reaction-dispose!
  (let [a1 (r/ratom nil)
        a2 (r/ratom nil)
        ^js r1 (#'r/->Reaction :r1 identity 10 false nil nil)]
    (._update-observables! r1 #{a1 a2})
    (add-watch r1 :r1 identity)

    ;; reaction r1 watches a1 & a2 observables; and there is a watcher on reaction r1
    (is (= #{a1 a2} (.-observables r1)))
    (is (= [r1] (keys (.-watches a1))))
    (is (= [r1] (keys (.-watches a2))))
    (is (= [:r1] (keys (.-watches r1))))

    ;; all watch links and observables are removed by dispose!
    (r/dispose! r1)
    (is (nil? (.-observables r1)))
    (is (nil? (keys (.-watches a1))))
    (is (nil? (keys (.-watches a2))))
    (is (nil? (keys (.-watches r1))))
    (is (false? (.-dirty? r1)))))

(deftest test-reaction-self-dispose!
  (let [a1 (r/ratom 2)
        a2 (r/ratom 3)
        r1 (r/make-reaction :r1 #(+ @a1 @a2))
        r2 (r/make-reaction :r2 #(do @r1))]
    ;; trigger links to observables (a1 & a2)
    (is (= 5 @r2))
    (is (= #{a1 a2} (.-observables r1)))
    (is (= #{r1} (.-observables r2)))
    (is (= [r1] (keys (.-watches a1))))
    (is (= [r1] (keys (.-watches a2))))
    (is (= [r2] (keys (.-watches r1))))
    (is (nil? (keys (.-watches r2))))

    ;; disposing bottom leaf of the graph (r2) will remove all r2's watches and observables, it's will also remove r1's watch on r2
    (r/dispose! r2)
    (is (= #{a1 a2} (.-observables r1)))
    (is (nil? (.-observables r2)))
    (is (= [r1] (keys (.-watches a1))))
    (is (= [r1] (keys (.-watches a2))))
    (is (nil? (keys (.-watches r1))))
    (is (nil? (keys (.-watches r2))))))

(deftest test-reaction-function-old-state
  (let [a1 (r/ratom 0)
        r1 (r/make-reaction :r1 (fn [s] (+ @a1 s)))]
    (is (= 0 @r1))
    (reset! a1 1)
    (is (= 1 @r1))
    (reset! a1 2)
    (is (= 3 @r1))
    (reset! a1 3)
    (is (= 6 @r1))))

(deftest test-reaction-signal-graph
  ;; signal graph:
  ;; a1 -> [r1 r2]
  ;; [r1 a2] -> [r3]
  (let [a1 (r/ratom 0)
        a2 (r/ratom 0)
        r1 (r/make-reaction :r1 #(do @a1))
        r2 (r/make-reaction :r2 #(do @a1))
        r3 (r/make-reaction :r3 #(+ @r1 @a2))]

    (is (= 0 @r1))
    (is (= 0 @r2))
    (is (= 0 @r3))

    (reset! a1 5)
    (is (= 5 @r1))
    (is (= 5 @r2))
    (is (= 5 @r3))

    (reset! a2 10)
    (is (= 5 @r1))
    (is (= 5 @r2))
    (is (= 15 @r3))

    (reset! a1 8)
    (is (= 8 @r1))
    (is (= 8 @r2))
    (is (= 18 @r3))))

(deftest test-reaction-macros
  (let [a1 (r/ratom 0)
        a2 (r/ratom 0)
        r1 (r/reaction :r1
                       (+ @a1 @a2))]

    (reset! a1 5)
    (is (= 5 @r1))

    (reset! a2 10)
    (is (= 15 @r1))))

(deftest test-reaction?
  (is (false? (r/reaction? nil)))
  (is (false? (r/reaction? 123)))
  (is (false? (r/reaction? "abc")))
  (is (false? (r/reaction? (r/ratom 0))))

  (is (true? (r/reaction? (r/reaction :r1 1)))))