(ns astrox.ecs.entities
  "ECS entities."
  (:require [clojure.spec.alpha :as s]
            [tails.ecs.core :as ecs]
            [astrox.ecs.components :as c]
            [astrox.ecs.views :as vw]))


(defn- infer-collider
  "Creates collider definition for a given entity. Uses size of the sprite as a collider size.
   Returns a map with collider definition."
  [view]
  (let [sprite (vw/root-sprite view)
        width  (.-width sprite)
        height (.-height sprite)
        ratio  (/ (js/Math.max width height) (js/Math.min width height))]
    ;; based on the sprite dimensions, infer most appropriate collider shape and size
    (if (<= ratio 1.2)
      {:shape :circle, :radius (/ (js/Math.max width height) 2)}
      {:shape :rectangle, :size {:x width :y height} :size-aabb {:x width :y height}})))


;;-----------------------------------------------------------------------------------------------
;; Player's ship


(s/fdef create-player-ship :args (s/cat :fields (s/keys :req-un [::c/position ::c/orientation])) :ret ::ecs/ext-entity)

(defn create-player-ship
  "Creates a player ship entity.
   Returns entity data as a vector: [EntityID, [ComponentInstance]]."
  [fields]
  (let [view (vw/create-player-ship)
        max-health 100
        phys-props {:linear-damping  0.3
                    :angular-damping 0.5
                    :collider        (infer-collider view)}
        eid        (ecs/create-entity)]
    [eid [(c/->Player)
          (c/->View view)
          (c/->Health max-health max-health)
          (c/new-rigid-body (merge phys-props fields))]]))


;; ------------------------------------------------------------------------------------------------
;; Meteors


(s/fdef create-meteor :args (s/cat :fields (s/keys :req-un [::c/position ::c/force ::c/torque])) :ret ::ecs/ext-entity)

(defn create-meteor
  "Creates a meteor entity with given props.
   Returns entity data as a vector: [EntityID, [[ComponentInstance]]."
  [fields]
  (let [view (vw/create-meteor)
        max-health 20
        phys-props {:collider (infer-collider view)}
        eid        (ecs/create-entity)]
    [eid [(c/->View view)
          (c/->Health max-health max-health)
          (c/new-rigid-body (merge phys-props fields))]]))
