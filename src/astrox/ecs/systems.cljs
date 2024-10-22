(ns astrox.ecs.systems
  "ECS systems."
  (:require [tails.ecs.core :as ecs]
            [tails.math.physics :as p]
            [tails.math.vector2d :as v]
            [tails.math.core :as math]
            [astrox.ecs.components :as c]
            [astrox.ecs.views.protocols :as vp]))

(def ^:private debug-show-colliders true)

(defn- render-new-entity-system
  "Renders entity's view once an entity with the View component is created. 
   Returns updated view object."
  [^js scene view eid world]
  (let [rigid-body                  (ecs/component world eid c/RigidBody)
        {position    :position
         orientation :orientation}  rigid-body
        view-obj                    (:view view)]
    (.addChild scene (vp/root-sprite view-obj))

    (vp/set-position view-obj position)
    (vp/set-orientation view-obj orientation)
    
    ;; FIXME: make it polymorphic
    (when (and debug-show-colliders (satisfies? vp/Debuggable view-obj))
      (vp/show-collider view-obj))
    
    view))


(defn- physics-system
  "Executes physics integration step for the RigidBody component of an entity and updates its View.
   Returns updated rigid-body object."
  [rigid-body eid world delta-time]
  (let [view       (-> (ecs/component world eid c/View) :view)
        ;; the view is responsible to create and manage collider; collider may change during the entity's life cycle
        rigid-body (-> (assoc rigid-body :collider (vp/get-collider view))
                       (p/integration-step delta-time))]
    (vp/set-position view (:position rigid-body))
    (vp/set-orientation view (:orientation rigid-body))

    ;; FIXME: make it polymorphic
    ;; update view speed if the entity is self-propelled
    (when (satisfies? vp/SelfPropelled view)
      (let [max-speed 300
            speed     (math/clamp01 (/ (v/length (:velocity rigid-body)) max-speed))]
        (vp/set-speed view speed)))

    rigid-body))


(defn all-systems
  "Returns a collection of 'system' functions that will be executed on each game loop tick.
   Each 'system' function handles a specific component of the entity and is defined as (fn [component eid world delta-time delta-frame])."
  [scene]
  {c/View      [(partial render-new-entity-system scene)]
   c/RigidBody [physics-system]})

