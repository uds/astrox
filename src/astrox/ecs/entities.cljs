(ns astrox.ecs.entities
  "ECS entities."
  (:require [clojure.spec.alpha :as s]
            [tails.ecs.core :as ecs]
            [astrox.ecs.components :as c]
            [astrox.ecs.views.player-ship :as pv]
            [astrox.ecs.views.meteor :as mv]))


;;-----------------------------------------------------------------------------------------------
;; Player's ship


(s/fdef create-player-ship :args (s/cat :fields (s/keys :req-un [::c/position ::c/orientation])) :ret ::ecs/ext-entity)

(defn create-player-ship
  "Creates a player ship entity.
   Returns entity data as a vector: [EntityID, [ComponentInstance]]."
  [fields]
  (let [view (pv/create-player-ship)
        max-health 100
        phys-props {:linear-damping  0.3
                    :angular-damping 0.5
                    :inverse-mass    (/ 1 1)}
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
  (let [view (mv/create-meteor)
        max-health 20
        phys-props {:inverse-mass    (/ 1 1)}
        eid        (ecs/create-entity)]
    [eid [(c/->View view)
          (c/->Health max-health max-health)
          (c/new-rigid-body (merge phys-props fields))]]))
