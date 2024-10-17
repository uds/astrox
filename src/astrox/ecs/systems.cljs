(ns astrox.ecs.systems
  "ECS systems."
  (:require [tails.ecs.core :as ecs]
            [tails.math.physics :as p]
            [astrox.ecs.components :as c]
            [astrox.ecs.views.protocols :as vp]))

(def ^:private debug-show-colliders true)

(defn- render-new-entity-system
  "Renders entity's view once an entity with the View component is created. 
   Returns updated view object."
  [^js scene view eid world]
  (let [rigid-body (ecs/component world eid c/RigidBody)
        {position    :position
         orientation :orientation} rigid-body
        view-obj  (:view view)]
    (.addChild scene (vp/root-sprite view-obj))
    (vp/set-position view-obj position)
    (vp/set-orientation view-obj orientation)

    (when debug-show-colliders
      (vp/show-collider view-obj (:collider rigid-body)))
    view))


(defn- physics-system
  "Executes physics integration step for the RigidBody component of an entity and updates its View.
   Returns updated rigid-body object."
  [rigid-body eid world delta-time]
  (let [rigid-body (p/integration-step rigid-body delta-time)
        view       (:view (ecs/component world eid c/View))]
    (vp/set-position view (:position rigid-body))
    (vp/set-orientation view (:orientation rigid-body))
    rigid-body))


(defn all-systems
  "Returns a collection of 'system' functions that will be executed on each game loop tick.
   Each 'system' function handles a specific component of the entity and is defined as (fn [component eid world delta-time delta-frame])."
  [scene]
  {c/View      [(partial render-new-entity-system scene)]
   c/RigidBody [physics-system]})

