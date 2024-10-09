(ns astrox.ecs.systems
  "ECS systems"
  (:require [tails.ecs.core :as ecs]
            [tails.math.physics :as p]
            [tails.pixi.core :as px]
            [astrox.ecs.components :as c]
            [tails.collision.core :as collision]))

(defn- render-new-entity-system
  "Renders entity's view once an entity with the View component is created. 
   Returns updated view object."
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
  "Executes physics integration step for the RigidBody component of an entity and updates its View.
   Returns updated rigid-body object."
  [rigid-body eid world delta-time]
  (let [rigid-body (p/integration-step rigid-body delta-time)
        view       (-> (ecs/component world eid c/View) .-view)]
    (px/set-pos view (.-position rigid-body))
    (set! (.-rotation view) (.-orientation rigid-body))
    rigid-body))


;; FIXME: this is not how system is defined. Collision detection step should probably done as a last step of the game loop
;; Is physics computation should be done in game loop as well? E.g. to compute not only changed rigid-bodies?
(defn- collision-detection-system
  "Detects and resolves collisions between entities."
  [world]
  (let [potential-collisions (collision/broad-phase world)
        actual-collisions (collision/narrow-phase world potential-collisions)]
    (collision/resolve-collisions world actual-collisions)))

(defn all-systems
  "Returns a collection of 'system' functions that will be executed on each game loop tick.
   Each 'system' function handles a specific component of the entity and is defined as (fn [component eid world delta-time delta-frame])."
  [scene]
  {c/View      [(partial render-new-entity-system scene)]
   c/RigidBody [physics-system]
   ;; Collision detection should happen after all movements were computed and updated
   c/Collider  [collision-detection-system]})

