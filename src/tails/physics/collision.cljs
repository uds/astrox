(ns tails.physics.collision
  "Collision detection and resolution."
  (:require [tails.math.vector2d :as v]))

(defn circle-vs-circle?
  "Detects collision between two circles and returns collision info with penetration depth and normal vector."
  [{:keys [position radius]} {:keys [position radius] :as circle2}]
  (let [distance-vector (v/subtract (:position circle2) position)
        distance (v/magnitude distance-vector)
        radius-sum (+ radius (:radius circle2))]
    (if (< distance radius-sum)
      {:penetration-depth (- radius-sum distance)
       :normal-vector (v/normalize distance-vector)}
      nil)))
