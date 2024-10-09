(ns astrox.ecs.world
  "Holds current state of the ECS world" 
  (:require [tails.pixi.core :as px]
            [astrox.ecs.components :as c]))

;; World management for the ECS framework.
;; Holds current state of the ECS world
(def !ecs-world (atom {}))

(defn clear-ecs-world 
  "Destroys all entity views and resets the ECS world."
  []
  (doseq [view (vals (get-in @!ecs-world [:components c/View]))]
    (px/destroy-cascade (.-view view)))
  (reset! !ecs-world {}))
