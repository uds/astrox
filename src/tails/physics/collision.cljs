(ns tails.physics.collision
  "Collision detection and resolution."
  (:require [tails.math.vector2d :as v]))

(defn circle-vs-circle?
  "Detects collision between two circles and returns collision info with penetration depth and normal vector."
  [pos1 radius1 pos2 radius2]
  (let [vector (v/sub pos2 pos1)
        radius-sum (+ radius1 radius2)]
    (if (< (v/length-squared vector) (* radius-sum radius-sum))
      (let [distance (v/length vector)
            normal (if (zero? distance) (v/vec2d 1 0) (v/normalize vector))]
        {:penetration (- radius-sum distance)
         :normal normal})
      nil)))
