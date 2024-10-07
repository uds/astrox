(ns tails.collision.core
  (:require [tails.math.vector2d :as v]
            [astrox.ecs.components :as c]
            [tails.ecs.core :as ecs]))

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

(defn broad-phase [world]
  "Identifies potential collisions using spatial partitioning."
  (let [entities (ecs/entities-with-component world c/Collider)]
    ;; Simple broad phase using pairwise comparison
    (for [e1 entities
          e2 entities
          :when (not= e1 e2)]
      [e1 e2])))

(defn narrow-phase [world potential-collisions]
  "Performs detailed collision checks on potential collisions."
  (filter (fn [[e1 e2]]
            (let [collider1 (ecs/component world e1 c/Collider)
                  collider2 (ecs/component world e2 c/Collider)]
              (case [(.-shape collider1) (.-shape collider2)]
                [:circle :circle] (circle-circle-collision? (.-position collider1) (.-size collider1) (.-position collider2) (.-size collider2))
                [:rectangle :rectangle] (rectangle-rectangle-collision? (.-position collider1) (.-size collider1) (.-position collider2) (.-size collider2))
                ;; Add more cases as needed
                false)))
          potential-collisions))

(defn resolve-collisions [world collisions]
  "Resolves detected collisions by applying repulsion forces."
  (doseq [[e1 e2] collisions]
    (let [collider1 (ecs/component world e1 c/Collider)
          collider2 (ecs/component world e2 c/Collider)
          ;; Calculate collision depth and apply repulsion
          collision-depth 1] ;; Placeholder for actual collision depth calculation
      (calculate-repulsion e1 e2 collision-depth))))

(defn calculate-repulsion [entity1 entity2 collision-depth]
  "Calculates and applies repulsion forces based on collision depth."
  ;; Calculate and apply repulsion forces based on collision depth
  ;; Update the RigidBody components of the entities
  )
