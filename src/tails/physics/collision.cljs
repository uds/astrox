(ns tails.physics.collision
  "Collision detection and resolution."
  (:require [tails.math.vector2d :as v]))

(defn collides?
  "Determines if a collision occurs between two objects based on their position and collider data.
   Currently supports only circle colliders."
  [pos1 collider1 pos2 collider2]
  (let [{:keys [shape radius]} collider1
        {:keys [shape radius]} collider2]
    (cond
      (and (= shape :circle) (= shape :circle))
      (circle-vs-circle? pos1 radius pos2 radius)

      :else
      (throw (ex-info "Unsupported collider type" {:collider1 collider1 :collider2 collider2})))))

(defn- circle-vs-circle?
  "Detects collision between two circles and returns collision info with penetration depth and normal vector."
  [pos1 radius1 pos2 radius2]
  (let [vector (v/sub pos2 pos1)
        radius-sum (+ radius1 radius2)]
    (if (< (v/length-squared vector) (* radius-sum radius-sum))
      (let [distance (v/length vector)]
        ;; if circles are on the same position, choose some random but consistent values
        (if (zero? distance)
          {:penetration radius1
           :normal v/right}
          {:penetration (- radius-sum distance)
           :normal (v/normalize vector)}))
      nil)))
