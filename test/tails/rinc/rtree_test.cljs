(ns tails.rinc.rtree-test
  (:require [cljs.test :refer [deftest is]]
            [tails.rinc.reaction :as rn]
            [tails.rinc.rtree :as rt]))


;; An implementation of the mutable mock scene node used in testing.
(deftype ^:private  MockNode [^:mutable data, ^:mutable on-destroy-fns]
  rt/SceneNode
  (add-child! [_this child index]
    (let [count        (count (:children data))
          index        (min (or index count) count)
          [left right] (split-at index (:children data))
          children     (concat left [child] right)]
      (set! data (assoc data :children children))
      index))
  (remove-child! [this index]
    (let [[left right] (split-at index (:children data))
          children     (concat left (rest right))]
      (set! data (assoc data :children children)))
    this)
  (on-destroy! [_this on-destroy]
    (set! on-destroy-fns (conj on-destroy-fns on-destroy)))

  IEquiv
  (-equiv [this other]
    (and (some? other)
         (= (.-data this) (.-data other))))

  IPrintWithWriter
  (-pr-writer [_this writer _] 
    (-write writer (str "#MockNode" data)))

  Object
  (destroy [_this]   
    (doseq [hfn on-destroy-fns] (hfn)))
  (children [_this]  
    (:children data))
  (clear-children! [this]
    (set! data (dissoc data :children))
    this)
  (set-props! [this props]
    (set! data (assoc data :props props))
    this))

(deftype ^:private MockComponent [node, ^:mutable mounts, ^:mutable unmounts]
  rt/Component
  (did-mount [_this _parent] (set! mounts (inc mounts)))
  (will-unmount [_this _parent] (set! unmounts (inc unmounts)))
  (scene-node [_this] node))

(defn- node
  ([tag]
   (MockNode. {:tag tag} nil))
  ([tag props]
   (MockNode. {:tag tag, :props props} nil)))

(defn- component [node]
  (MockComponent. node 0 0))

;; -------------------------------------------------------------------------------------------------------------------------------------------

(deftest add-node-test
  (is (= nil (#'rt/add-node (node :tag1) nil nil)))

  (let [parent (node :p)
        child1 (node :c1)
        child2 (node :c2)
        child3 (component (node :c3))
        child4 (node :c4)
        child5 (component nil)]
    (is (= 0 (#'rt/add-node parent child1 nil)))
    (is (= 1 (#'rt/add-node parent child2 4)))
    (is (= 2 (#'rt/add-node parent child3 nil)))
    (is (= 3 (#'rt/add-node parent child4 nil)))

    ;; nil nodes are not added to parent
    (is (= nil (#'rt/add-node parent child5 nil)))
    (is (= nil (#'rt/add-node parent nil nil)))
    (is (= 1 (.-mounts child5)))   ;; but component even with nil node is mounted

    (is (= [child1 child2 (.-node child3) child4] (.children parent)))

    (is (= 1 (.-mounts child3)))
    (is (= 0 (.-unmounts child3)))))

(deftest remove-node-test
  (let [parent (node :p)
        child1 (node :c1)
        child2 (node :c2)
        child3 (component (node :c3))]
    (is (thrown-with-msg? js/Error #"Assert failed" (#'rt/remove-node parent nil nil)))

    (#'rt/remove-node parent nil 0)
    (is (empty? (.children parent)))

    (#'rt/add-node parent child1 nil)
    (#'rt/add-node parent child2 nil)
    (#'rt/add-node parent child3 nil)
    (is (= [child1 child2 (.-node child3)] (.children parent)))
    (is (= 1 (.-mounts child3)))

    (#'rt/remove-node parent nil 12345)
    (is (= [child1 child2 (.-node child3)] (.children parent)))

    (#'rt/remove-node parent nil 1)
    (is (= [child1 (.-node child3)] (.children parent)))

    (#'rt/remove-node parent child3 1)
    (is (= [child1] (.children parent)))
    (is (= 1 (.-unmounts child3)))

    (#'rt/remove-node parent child1 0)
    (is (empty? (.children parent)))))

(deftest replace-node-test
  (let [parent (node :p)
        child0 (node :c0)
        child1 (node :c1)
        child2 (node :c2)
        child3 (component (node :c3))]
    (is (thrown-with-msg? js/Error #"Assert failed" (#'rt/replace-node parent nil nil nil)))
    (is (thrown-with-msg? js/Error #"Assert failed" (#'rt/replace-node parent nil child2 nil)))
    (is (thrown-with-msg? js/Error #"Assert failed" (#'rt/replace-node parent child1 child2 nil)))

    (is (= 0 (#'rt/replace-node parent nil nil 0)))
    (is (empty? (.children parent)))

    (#'rt/add-node parent child0 nil)
    (#'rt/add-node parent child1 nil)
    (#'rt/add-node parent child3 nil)
    (is (= [child0 child1 (.-node child3)] (.children parent)))

    (is (= 1 (#'rt/replace-node parent nil child2 1)))
    (is (= 2 (#'rt/replace-node parent child3 child1 2)))
    (is (= [child0 child2 child1] (.children parent)))
    (is (= 1 (.-unmounts child3)))))


(deftest update-node-test
  (let [parent (node :p)
        child1 (node :c1)
        child2 (node :c2)
        child3 (component (node :c3))]

    (is (nil? (#'rt/update-node parent nil nil nil)))
    (is (= 0 (#'rt/update-node parent nil nil 0)))

    ;; add node
    (is (= 0 (#'rt/update-node parent nil child1 nil)))
    (is (= 1 (#'rt/update-node parent nil child2 nil)))
    (is (= 2 (#'rt/update-node parent nil child3 nil)))
    (is (= [child1 child2 (.-node child3)] (.children parent)))
    (is (= 1 (.-mounts child3)))

    ;; remove node
    (is (= 0 (#'rt/update-node parent nil nil 0)))     ;; does nothing
    (is (nil? (#'rt/update-node parent child2 nil 1)))
    (is (nil? (#'rt/update-node parent child3 nil 1))) ;; child3 index shifted after previous delete of child2
    (is (= [child1] (.children parent)))
    (is (= 1 (.-unmounts child3)))

    ;; replace
    (is (= 0 (#'rt/update-node parent child1 child3 0)))
    (is (= [(.-node child3)] (.children parent)))
    (is (= 2 (.-mounts child3)))))

(deftest compensate-node-index-test
  (is (= 0 (#'rt/compensate-node-index 0 nil)))
  (is (= 0 (#'rt/compensate-node-index 0 [])))

  ;; initial:     0 1 2 3 4 5 6 7 8 9 10
  ;; deleted:           3   5   7 8
  ;; gaped:       0 1 2   4   6     9 10
  ;; compensated: 0 1 2(3)3(4)4(5 5)5 6
  (let [del-ids [3 5 7 8]]
    (is (= [0 1 2 3 3 4 4 5 5 5 6]
           (map #(#'rt/compensate-node-index % del-ids) (range 11))))))

(deftest deleted-node-indices-test
  (let [indices {:n4 [3 (node :n4)]
                 :n1 [0 (node :n1)]
                 :n3 [2 (node :n3)]
                 :n2 [1 (node :n2)]}]
    (is (empty? (#'rt/deleted-node-indices indices nil)))
    (is (empty? (#'rt/deleted-node-indices indices [])))
    (is (empty? (#'rt/deleted-node-indices indices [:abc :xyz])))

    (is (= [0] (#'rt/deleted-node-indices indices [:n1])))
    (is (= [3] (#'rt/deleted-node-indices indices [:n4])))
    (is (= [1 3] (#'rt/deleted-node-indices indices [:n2 :n4])))
    (is (= [2 0 1] (#'rt/deleted-node-indices indices [:n3 :n1 :n2])))  ;; preserves ordering of del-ids list
   ))

(deftest delete-children-nodes-test
  (let [parent      (node :p)
        child1      (node :c1)
        child2      (node :c2)
        child3      (node :c3)

        indices     {:c1 [0 child1]
                     :c2 [1 child2]
                     :c3 [2 child3]}

        init-parent (fn []
                      (.clear-children! parent)
                      (#'rt/add-node parent child1 nil)
                      (#'rt/add-node parent child2 nil)
                      (#'rt/add-node parent child3 nil))]

    (is (= indices (#'rt/delete-children-nodes parent nil indices)))
    (is (= indices (#'rt/delete-children-nodes parent [] indices)))
    (is (= indices (#'rt/delete-children-nodes parent [:abc] indices)))

    ;; indices of remaining children are recomputed
    (init-parent)
    (is (= {:c2 [0 child2] :c3 [1 child3]} (#'rt/delete-children-nodes parent [:c1] indices)))
    (is (= [child2 child3] (.children parent)))

    (init-parent)
    (is (= {:c1 [0 child1] :c3 [1 child3]} (#'rt/delete-children-nodes parent [:c2] indices)))
    (is (= [child1 child3] (.children parent)))

    (init-parent)
    (is (= {:c1 [0 child1] :c2 [1 child2]} (#'rt/delete-children-nodes parent [:c3] indices)))
    (is (= [child1 child2] (.children parent)))

    (init-parent)
    (is (= {:c2 [0 child2]} (#'rt/delete-children-nodes parent [:c3 :c1] indices)))
    (is (= [child2] (.children parent)))

    (init-parent)
    (is (= {:c1 [0 child1]} (#'rt/delete-children-nodes parent [:c2 :c3] indices)))
    (is (= [child1] (.children parent)))

    (init-parent)
    (is (empty? (#'rt/delete-children-nodes parent [:c1 :c2 :c3] indices)))
    (is (empty? (.children parent)))))

(deftest update-children-nodes-test
  (let [parent (node :p)
        child1 (node :c1)
        child2 (node :c2)
        child3 (component (node :c3))
        child4 (node :c4)]

    (is (nil? (#'rt/update-children-nodes parent nil nil nil)))
    (is (empty? (.children parent)))

    (let [indices {:c1 [0 child1], :c3 [1 child3]}]
      (is (= indices (#'rt/update-children-nodes parent nil {:c1 child1, :c3 child3} nil)))
      (is (= [child1 (.-node child3)] (.children parent)))

      (is (= {:c1 [0 child4], :c2 [1 child2]} (#'rt/update-children-nodes parent indices {:c2 child2, :c1 child4} [:c3])))
      (is (= [child4 child2] (.children parent))))))


(deftest !element-test
  (is (= (node :n1) (rt/!element (fn [_old] (node :n1)))))

  (let [!r1 (rn/ratom 123)
        !changes-count (atom 0)
        !e1 (rn/reaction :rn1
                         (rt/!element (fn [old-node]
                                        (if-not old-node
                                          (node :n1 {:r1 @!r1})
                                          (.set-props! ^clj old-node {:r1 @!r1})))))]

    ;; detect changes in the wrapped reaction
    (add-watch !e1 :e1-watch (fn [_ _ _ _] (swap! !changes-count inc)))

    (is (= (node :n1 {:r1 123}) @!e1))
    (reset! !r1 456)
    (is (= (node :n1 {:r1 456}) @!e1))

    ;; wrapping reaction should not detect any changes once the element was created
    (is (= 0 @!changes-count))))

(deftest !child-test
  (let [parent (node :p1)
        !e0 (rt/!child parent (fn [_old] (node :n1)))]
    (is (rn/reaction? !e0))
    (is (= 0 (.-index @!e0)))
    (is (= [(node :n1)] (.children parent)))

    (let [!open? (rn/ratom false)
          !changes-count (atom 0)
          !e1 (rn/reaction :rn1
                           (rt/!child parent (fn [_old] (when @!open? (node :n2)))))]

      ;; detect changes in the wrapping reaction
      (add-watch !e1 :e1-watch (fn [_ _ _ _] (swap! !changes-count inc)))

      (is (nil? (.-index @@!e1)))
      (is (= [(node :n1)] (.children parent)))

      (reset! !open? true)
      (is (= 1 (.-index @@!e1)))
      (is (= [(node :n1) (node :n2)] (.children parent)))

      (reset! !open? false)
      (is (nil? (.-index @@!e1)))
      (is (= [(node :n1)] (.children parent)))

      ;; wrapping reaction should not detect any changes once the element was created
      (is (= 0 @!changes-count)))))

(deftest !child-destroy-test
  (let [parent (node :p1)
        !open? (rn/ratom false)
        !e1 (rn/reaction :rn1
                         (rt/!child parent (fn [_old] (when @!open? (node :n2)))))]

    (is (seq (.-observables ^js @!e1)))
    (is (seq (.-watches ^js @!e1)))

    ;; simulate destruction of the node that disposes the parent node: the reaction is disposed, loosing links to its observables and watches.
    (.destroy parent)
    (is (empty? (.-observables ^js @!e1)))
    (is (empty? (.-watches ^js @!e1)))))

(deftest !children-test
  (let [parent (node :p1)
        child1 (node :c1)
        child2 (node :c2)
        child3 (node :c3)
        child4 (node :c4)

        !step (rn/ratom 0)
        !e1 (rn/reaction :rn1
                         (rt/!children parent
                                       (fn [_old]
                                         (case @!step
                                           0  [{:c1 child1, :c2 child2}, nil]
                                           1  [{:c3 child3, :c1 child4}, [:c2]]))))]
    (is (rn/reaction? !e1))
    (is (rn/reaction? @!e1))

    ;; step 0
    (is (= {:c1 [0 child1], :c2 [1 child2]} (.-indices @@!e1)))
    (is (= [child1 child2] (.children parent)))

    ;; step 1
    (reset! !step 1)
    (is (= {:c1 [0 child4], :c3 [1 child3]} (.-indices @@!e1)))
    (is (= [child4 child3] (.children parent)))))
