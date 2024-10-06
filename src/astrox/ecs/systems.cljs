(ns astrox.ecs.systems
  "ECS systems"
  (:require [tails.ecs.core :as ecs]
            [tails.math.physics :as p]
            [tails.pixi.core :as px]
            [astrox.ecs.components :as c]))

(defn- render-new-entity-system
  "Renders entity's view once an entity with the View component is created."
  [^js scene view eid world]
  (let [rigid-body (ecs/component world eid c/RigidBody)
        {position    :position
         orientation :orientation} rigid-body
        view-obj  (.-view view)]
    (.addChild scene view-obj)
    (px/set-pos view-obj position)
    (set! (.-rotation view-obj) orientation)
    view))


(defn- physics-system
  "Updates all entities with updated RigidBody component."
  [rigid-body eid world delta-time]
  (let [rigid-body (p/integration-step rigid-body delta-time)
        view       (-> (ecs/component world eid c/View) .-view)]
    (px/set-pos view (.-position rigid-body))
    (set! (.-rotation view) (.-orientation rigid-body))
    rigid-body))


(defn all-systems
  "Returns a collection of 'system' functions that will be executed on each game loop tick.
   Each 'system' function handles a specific component of the entity and is defined as (fn [component eid world delta-time delta-frame])."
  [scene]
  {c/View      [(partial render-new-entity-system scene)]
   c/RigidBody [physics-system]})
