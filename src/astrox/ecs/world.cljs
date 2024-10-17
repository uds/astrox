(ns astrox.ecs.world
  "Holds current state of the ECS world" 
  (:require [astrox.ecs.components :as c]
            [astrox.ecs.views.protocols :as vp]))

;; World management for the ECS framework.
;; Holds current state of the ECS world
(def !ecs-world (atom {}))

(defn clear-ecs-world 
  "Destroys all active entity views and resets the ECS world."
  []
  (doseq [view (vals (get-in @!ecs-world [:components c/View]))]
    (vp/destroy (:view view)))
  (reset! !ecs-world {}))
