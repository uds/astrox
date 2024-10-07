(ns tails.collision.core
  (:require [tails.math.vector2d :as v]
            [astrox.ecs.components :as c]
            [tails.ecs.core :as ecs]))

(defn circle-circle-collision?
  "Detects collision between two circles."
  [pos1 radius1 pos2 radius2]
  (let [distance (v/distance pos1 pos2)]
    (< distance (+ radius1 radius2))))

(defn rectangle-rectangle-collision?
  "Detects collision between two rectangles."
  [{x1 :x y1 :y :as _pos1} {w1 :x h1 :y :as _size1} {x2 :x y2 :y :as _pos2} {w2 :x h2 :y :as _size2}]
  (and (< (Math/abs (- x1 x2)) (+ (/ w1 2) (/ w2 2)))
       (< (Math/abs (- y1 y2)) (+ (/ h1 2) (/ h2 2)))))

(defn circle-rectangle-collision?
  "Detects collision between a circle and a rectangle."
  [circle-pos circle-radius rect-pos rect-size]
  ;; Implement logic for circle-rectangle collision detection
  )

(defn broad-phase
  "Identifies potential collisions using spatial partitioning."
  [world]
  (let [entities (ecs/entities-with-component world c/Collider)]
    ;; Simple broad phase using pairwise comparison
    (for [e1 entities
          e2 entities
          :when (not= e1 e2)]
      [e1 e2])))

(defn narrow-phase
  "Performs detailed collision checks on potential collisions."
  [world potential-collisions]
  (filter (fn [[e1 e2]]
            (let [collider1 (ecs/component world e1 c/Collider)
                  collider2 (ecs/component world e2 c/Collider)
                  pos1 (.-position collider1)
                  size1 (.-size collider1)
                  pos2 (.-position collider2)
                  size2 (.-size collider2)]
              (case [(.-shape collider1) (.-shape collider2)]
                [:circle :circle] (circle-circle-collision? pos1 size1 pos2 size2)
                [:rectangle :rectangle] (rectangle-rectangle-collision? pos1 size1 pos2 size2)
                [:circle :rectangle] (circle-rectangle-collision? pos1 size1 pos2 size2)
                [:rectangle :circle] (circle-rectangle-collision? pos2 size2 pos1 size1)
                false)))
          potential-collisions))

(defn calculate-repulsion
  "Calculates and applies repulsion forces based on collision depth."
  [entity1 entity2 collision-depth]
  ;; Calculate and apply repulsion forces based on collision depth
  ;; Update the RigidBody components of the entities
  )

(defn resolve-collisions
  "Resolves detected collisions by applying repulsion forces."
  [world collisions]
  (doseq [[e1 e2] collisions]
    (let [collider1 (ecs/component world e1 c/Collider)
          collider2 (ecs/component world e2 c/Collider)
          ;; Calculate collision depth and apply repulsion
          collision-depth 1] ;; Placeholder for actual collision depth calculation
      (calculate-repulsion e1 e2 collision-depth))))
