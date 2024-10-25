(ns tails.physics.collision
  "Collision detection and resolution."
  (:require [tails.math.vector2d :as v]))

(defn circle-vs-circle?
  "Detects collision between two circles and returns collision info with penetration depth and normal vector."
  [pos1 radius1 pos2 radius2]
  (let [distance-vector (v/subtract pos2 pos1)
        distance (v/magnitude distance-vector)
        radius-sum (+ radius1 radius2)]
    (if (< distance radius-sum)
      {:penetration-depth (- radius-sum distance)
       :normal-vector (v/normalize distance-vector)}
      nil)))
