(ns tails.pixi.rtree
  "Integration of 'reactive tree' (rtree) package with Pixi components."
  (:require [pixi.js :refer (Container)]
            ["@pixi/layout" :refer (Layout)]
            [tails.pixi.core :as px]
            [tails.rinc.rtree :as rt]))


(defn- on-node-destroyed 
  [node on-destroy]
  (.on node "destroyed" on-destroy))


;; -------------------------------------------------------------------------------------------------------------
;; PIXI Container extension for rtree support

(defn- add-container-child 
  "Adds a child node to the regular PIXI container, at the given position - if specified 
   or as a last child - otherwise.
   Returns an index of a newly added child."
  [^js node child index]
  (let [new-index (or index (count (.-children node)))]
    (if index
      (.addChildAt node child index)
      (.addChild node child))
    new-index))

(defn- remove-container-child 
  "Removes a child node from a parent container by a given index.
   Returns the container."
  [^js node index]
  (when-let [child (.removeChildAt node index)]
    (px/destroy-cascade child))
  node)


;; Extends PIXI Container class with the ScreenNode protocol so the Container can be used by rtree rendering.
(extend-type Container
  rt/SceneNode
  (add-child! [this child index]   (add-container-child this child index))
  (remove-child! [this index]      (remove-container-child this index))
  (on-destroy! [this on-destroy]   (on-node-destroyed this on-destroy)))


;; -------------------------------------------------------------------------------------------------------------
;; PIXI Layout extension for rtree support


(defn- require-layout-child [child]
  (when-not (instance? Layout child)
    (throw (js/Error. (str "A child should be of Layout type: " child)))))

(defn- add-layout-child
  "Adds a child to the layout. The child should be of the Layout type in order to have an ID 
   that can be used to remove this child from the layout later.
   Note that layout does not support adding children at the arbitrary index thus 
   the 'index' argument is ignored. 
   Returns index of the added child"
  [^js layout child _index]
  (let [index (count (.-children layout))]
    (require-layout-child child)
    (.addContent layout child)
    index))

(defn- remove-layout-child [^js layout index]
  ;; get child from the layout's regular container by it's index
  (when-let [child (.getChildAt layout index)]
    (require-layout-child child)
    ;; remove then a child by it's ID from the layout. note that the child should be of a Layout type.
    (.removeChildByID layout (.-id child))
    (px/destroy-cascade child))
  layout)


;; Extends PIXI Layout class with the ScreenNode protocol so the Layout can be used by rtree rendering.
;; Layout is an a special node, designed to group together and layout other Pixi components.
(extend-type Layout
  rt/SceneNode
  (add-child! [this child index]   (add-layout-child this child index))
  (remove-child! [this index]      (remove-layout-child this index))
  (on-destroy! [this on-destroy]   (on-node-destroyed this on-destroy)))
