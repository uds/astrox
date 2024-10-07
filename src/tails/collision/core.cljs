(ns tails.collision.core
  (:require [tails.math.vector2d :as v]))

(defn circle-circle-collision? [pos1 radius1 pos2 radius2]
  "Detects collision between two circles."
  (let [distance (v/distance pos1 pos2)]
    (< distance (+ radius1 radius2))))

(defn rectangle-rectangle-collision? [pos1 size1 pos2 size2]
  "Detects collision between two rectangles."
  (and (< (Math/abs (- (v/x pos1) (v/x pos2))) (+ (/ (v/x size1) 2) (/ (v/x size2) 2)))
       (< (Math/abs (- (v/y pos1) (v/y pos2))) (+ (/ (v/y size1) 2) (/ (v/y size2) 2)))))

(defn circle-rectangle-collision? [circle-pos circle-radius rect-pos rect-size]
  "Detects collision between a circle and a rectangle."
  ;; Implement logic for circle-rectangle collision detection
  )

(defn calculate-repulsion [entity1 entity2 collision-depth]
  "Calculates and applies repulsion forces based on collision depth."
  ;; Calculate and apply repulsion forces based on collision depth
  ;; Update the RigidBody components of the entities
  )
