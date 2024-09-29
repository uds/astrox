(ns tails.rinc.rtree
  "A set of helper functions to render a scene graph as a reactive tree 
   where some nodes are represented as reactions. Each reactive node in a tree
   will re-render itself if any of it's monitored reactions (that are 
   captured as a part of the reaction function body) did change."
  (:require [tails.rinc.reaction :as rn]))


(defprotocol SceneNode
  "A scene node is a widget on the screen and a part of the scene graph."
  (add-child! [this child index]  "Adds a child to the node at the given index or at the end if the index is nil. Returns index of added child.")
  (remove-child! [this index]     "Removes a child from the node. Returns this node.")
  (on-destroy! [this on-destroy]  "Register handler for 'destroyed' event"))


(defprotocol Component
  "Exposes additional lifecycle hooks for the scene node."
  (did-mount [this parent]     "Called when component is mounted to the scene tree.")
  (will-unmount [this parent]  "Called when component is about to be unmounted from the scene tree.")
  (scene-node [this]           "Returns a scene node."))


(defn component? [comp]
  (satisfies? Component comp))

(defn- get-node
  "Check if the node implements Component protocol. If yes, returns a node stored by this 
   component or returns an input node otherwise."
  [node]
  (if (component? node) (scene-node node) node))

(defn- add-node
  "Adds input node to the parent. 
   If the index is not nil, inserts the node at the given index. 
   Else appends the node as last child of the parent.
   Returns an index of the child."
  [^SceneNode parent, ^SceneNode node, index]
  (let [node' (get-node node)
        index' (if node'
                 (add-child! parent node' index)
                 index)]
    ;; invoke a lifecycle hook on the node
    (when (component? node)
      (did-mount node parent))
    index'))

(defn- remove-node
  "Removes a child node at the given index. 
   Either an index or an old-node argument should be defined.
   Returns nil."
  [^SceneNode parent, ^SceneNode old-node, index]
  (assert (or old-node index))
  ;; invoke lifecycle hook on the node
  (when (component? old-node)
    (will-unmount old-node parent))
  (when index
    (remove-child! parent index))
  nil)

(defn- replace-node
  "Replaces a node at the given index. 
   Returns an index of the new node."
  [^SceneNode parent, ^SceneNode old-node, ^SceneNode new-node, index]
  (assert index)
  (remove-node parent old-node index)
  (add-node parent new-node index))

(defn- update-node
  "Depending on the input, does one of the following:
    - adds a new node to the parent, if old-node is nil; returns an index of just added node.
    - removes an old node from the parent, if new-node is nil; returns nil 
    - replaces an old node with a new node, if both old and new nodes are defined; returns the same index."
  [^SceneNode parent, ^SceneNode old-node, ^SceneNode new-node, index]
  (cond
    ;; element did not change, returns index
    (= old-node new-node)        index
    ;; append a new element, returns new index
    (nil? old-node)              (add-node parent new-node nil)
    ;; remove an old element, returns nil
    (nil? new-node)              (remove-node parent old-node index)
    ;; replace an old element with the new one, returns index
    :else                        (replace-node parent old-node new-node index)))

(defn- dispose-on-destroy!
  "Makes reaction to be disposed on destruction of the node."
  [^SceneNode node, !reaction]
  (assert node)
  (on-destroy! node  #(rn/dispose! !reaction)))


;; ---------------------------------------------------------------------------------------------------------------


(defn- compensate-node-index
  "Returns a 'compensated' value of the node index according to already deleted node indices.
   Note that the 'sorted-del-indices' should be a sorted sequence of integers!"
  [index sorted-del-indices]
  ;; could it benefit from further optimization e.g. from binary search?
  (loop [[del & more] sorted-del-indices
         result index]
    (if (and (some? del) (< del index))
      (recur more (dec result))
      result)))

(defn- deleted-node-indices
  "Returns a sorted list of indices of the deleted children nodes selected by the given node id's.
   The input 'del-ids' argument is a sorted set of deleted node ids"
  [indices sorted-del-ids]
  (->> (map (comp first (partial get indices)) sorted-del-ids)
       (remove nil?)))

(defn- delete-children-nodes
  "Removes all deleted nodes from the indices, where
   'del-ids' is a collection of deleted node id's, and
   'old-indices' is map of (node-id -> [index node]).
   Returns updated indices as a map of (node-id -> [index node]). 
   Note that returned map will have gaps in node indexes and should be recomputed to remove these gaps."
  [^SceneNode parent, del-ids, old-indices]
  (if (seq del-ids)
    (let [del-ids-set        (apply sorted-set del-ids)
          sorted-del-indices (deleted-node-indices old-indices del-ids-set)]
      (reduce-kv (fn [acc id [index node]]
                   (let [index* (compensate-node-index index sorted-del-indices)]
                     (if (contains? del-ids-set id)
                       (do 
                         (update-node parent node nil index*) ;; should use compensated index to update parent scene node
                         acc)   
                       (assoc acc id [index* node]))))        ;; should compensate index for remaining entries
                 {} old-indices))
    old-indices))

(defn- upsert-children-nodes
  "Updates existing children nodes and adds new ones to the parent.
   Returns an updated map of (node-id -> [index node])."
  [^SceneNode parent, upd-nodes, old-indices]
  (if (seq upd-nodes)
    (reduce-kv (fn [acc upd-id upd-node]
                 (let [[old-index old-node] (get acc upd-id)
                       new-index            (update-node parent old-node upd-node old-index)]
                   (if (and (= old-index new-index) (= old-node upd-node))
                     acc
                     (assoc acc upd-id [new-index upd-node]))))
               old-indices upd-nodes)
    old-indices))

(defn- update-children-nodes
  "Updates children nodes of the parent node according to the difference between old and new input states:
   The 'old-indices' is a current map of (node-id -> [index node]). 
   The 'upd-nodes' is a map of (node-id -> node), for updated or new nodes.
   The 'del-ids' is a collection of deleted node ids.
   Calls update-node function for each pair of old / new nodes.
   Returns an updated map of (node-id -> [index node])."
  [^SceneNode parent, old-indices, upd-nodes, del-ids]
  (->> old-indices
       (delete-children-nodes parent del-ids)
       (upsert-children-nodes parent upd-nodes)))


;; Use structure that has an 'equiv' that always returns true as a state for 'reaction-fn' function.
;; This way the state changes itself will not trigger reaction.
(deftype ^:private ReactiveChildState [index node]
  IEquiv
  (-equiv [_this, _other]
    ;; this state will always match with any other state, so the reaction that uses this state will never trigger 
    ;; any change in enclosing reaction or listener.
    true))

;; Use structure that has an 'equiv' that always returns true as a state for 'reaction-fn' function.
;; This way the state changes itself will not trigger reaction.
;; The 'indices' is a map of (node-id -> [index node]). 
(deftype ^:private ReactiveChildrenState [indices]
  IEquiv
  (-equiv [_this, _other]
    ;; this state will always match with any other state, so the reaction that uses this state will never trigger 
    ;; any change in enclosing reaction or listener.
    true))


;; ----------------------------------------------------------------------------------------------------------------


(defn !element
  "Creates a standalone reactive element. 
   The element's node is never re-created as a result of the reaction change, only it's internal state can be updated 
   as a result of execution of the 'reaction-fn'.
   Returns created element node"
  [reaction-fn]
  (let [!r (rn/make-reaction (fn [old-node]
                               (let [new-node (reaction-fn old-node)]
                                 (assert (or (nil? old-node) (= old-node new-node)) "Reactive element can't re-create its node")
                                 new-node)))]
    ;; the node created by this reaction "owns" it and is responsible to dispose it
    (dispose-on-destroy! @!r !r)
    @!r))

(defn !child
  "Creates a reactive element as a child of the given parent.
   As a result of the reaction change, 'reaction-fn' is called and the resulting child node 
   is either re-attached to the parent or removed (if it was nil). 
   The 'reaction-fn' takes old SceneNode instance (or nil) as a parameter and returns a new 
   SceneNode instance (or nil).
   Returns created reaction."
  [^SceneNode parent, reaction-fn]
  ;; Reaction uses node index as its state: it allows to remember a last child node position and 
  ;; to use it to locate the old node. The index is also stable: its not going to change between node 
  ;; updates and does not trigger an unnecessary re-computation of the reaction.
  (let [!r (rn/make-reaction (fn [^ReactiveChildState state]
                               (let [old-index (when state (.-index state))
                                     old-node  (when state (.-node state))
                                     new-node  (reaction-fn old-node)
                                     new-index (update-node parent old-node new-node old-index)]
                                 (->ReactiveChildState new-index new-node))))]
    ;; parent "owns" the reaction and is responsible to dispose it
    (dispose-on-destroy! parent !r)
    @!r    ;; create the child node by dereferencing reaction for the first time
    !r))

(defn !child-when
  "Often used form that shows the child when the !visible? reaction is true.
   Returns created reaction."
  [^SceneNode parent, !visible?, reaction-fn]
  (!child parent (fn [old-node]
                   (when @!visible? (reaction-fn old-node)))))

(defn !children
  "Creates a reactive element as a collection of children of the given parent.
   A change in the reaction will trigger the execution of 'reaction-fn' function. 

   The 'reaction-fn' accepts an 'old-indices' argument that is a map of (node-id -> [index node]). 
   It returns a tuple, where the first element is a map of (node-id -> node) holding updated or 
   new children, and the second element is an array of deleted node id's.

   Note that the 'node-id' is an application-specific node identifier that has to be unique inside of 
   the parent container. It is needed so that application can inform us that nodes has been updated or deleted.
   It has to be a stable property of the node - e.g. it can't be based on the node index which can change 
   whenever children are added or removed to the parent node.

   New and old children are compared and parent node is updated according to the difference.
   Returns created reaction."
  [^SceneNode parent, reaction-fn]
  (let [!r (rn/make-reaction (fn [^ReactiveChildrenState state]
                               (let [old-indices         (if state (.-indices state) nil)
                                     [upd-nodes del-ids] (reaction-fn old-indices)
                                     new-indices         (update-children-nodes parent old-indices upd-nodes del-ids)]
                                 (->ReactiveChildrenState new-indices))))]
    ;; parent "owns" the reaction and is responsible to dispose it
    (dispose-on-destroy! parent !r)
    @!r    ;; create children nodes by dereferencing reaction for the first time
    !r))

(defn !effect
  "Create a reactive effect with effect-fn function and attaches it to the given parent.
   Returns created reaction."
  [^SceneNode parent, effect-fn]
  (let [!r (rn/make-reaction effect-fn)]
    ;; parent "owns" the reaction and is responsible to dispose it
    (dispose-on-destroy! parent !r)
    @!r   ;; initialize effect 
    !r))
