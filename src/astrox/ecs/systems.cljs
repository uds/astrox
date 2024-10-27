(ns astrox.ecs.systems
  "ECS systems."
  (:require [tails.ecs.core :as ecs]
            [tails.physics.core :as p]
            [tails.math.vector2d :as v]
            [tails.math.core :as math]
            [tails.physics.collision :as cn]
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


(defn- collisions-system
  "Executes collision detection and resolution for the RigidBody components of entities.
   Returns updated rigid-body objects as components map { EntityId -> ComponentInstance }."
  [rigid-bodies-map]
  (let [entities (map (fn [[k v]] (assoc v :eid k)) rigid-bodies-map)
        collisions (cn/detect-and-resolve-collisions entities)]
    (when (seq collisions) 
      (println (map (fn [e] (v/length (:velocity e))) collisions)))
    (into {}
          (map (fn [e] [(:eid e) e]) collisions))))

  ;; (let [collisions (cn/detect-and-resolve-collisions (vals rigid-bodies-map))]
  ;;     (println "collisions:" collisions)
  ;;     rigid-bodies-map)


;;------------------------------------------------------------------------------


(defn all-systems
  "Returns a collection of 'system' functions that will be executed on each game loop tick.
   Each 'system' function handles a specific component of the entity and is defined as (fn system-fn [component eid world delta-time delta-frame]).
   It returns updated component object."
  [scene]
  {c/View      [(partial render-new-entity-system scene)]
   c/RigidBody [physics-system]})


(def all-systems-by-component
  "Returns a collection of 'system' functions that will be executed on each game loop tick.
   Each 'system' function handles a specific component of the entity and is defined as (fn system-fn [components-map world delta-time delta-frame]).
   It returns updated components map."
  {c/RigidBody collisions-system})