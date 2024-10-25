(ns tails.physics.collision
  "Collision detection and resolution."
  (:require [clojure.spec.alpha :as s]
            [tails.math.vector2d :as v]
            [tails.physics.common :as c]))


(s/def ::penetration number?)
(s/def ::normal ::v/vector2d)
(s/def ::collision-info (s/keys :req-un [::penetration ::normal]))
(s/def ::entity (s/keys :req-un [::c/position ::c/collider]))


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


(s/fdef collides? 
  :args (s/cat :pos1 ::v/vector2d, :collider1 ::c/collider, :pos2 ::v/vector2d, :collider2 ::c/collider)
  :ret (s/nilable ::collision-info))

(defn- collides?
  "Determines if a collision occurs between two objects based on their position and collider data.
   Currently supports only circle colliders."
  [pos1 collider1 pos2 collider2]
  (let [{shape1 :shape radius1 :radius} collider1
        {shape2 :shape radius2 :radius} collider2]
    (cond
      (and (= shape1 :circle) (= shape2 :circle))
      (circle-vs-circle? pos1 radius1 pos2 radius2)

      :else
      (throw (ex-info "Unsupported collider type" {:collider1 collider1 :collider2 collider2})))))


(s/fdef broad-phase
  :args (s/cat :entities (s/coll-of ::entity))
  :ret (s/coll-of (s/tuple ::entity ::entity)))

(defn- broad-phase
  "Returns a sequence of pairs of entities that are potentially colliding."
  [entities]
  ;; Converting to vector makes it a bit faster, because drop on vector is faster then on the list
  ;; But it is still probably requires a quad tree optimisation for large number of entities
  (let [indexed (vec (map-indexed vector entities))]
    (for [[i entity1] indexed
          [_ entity2] (drop (inc i) indexed)]
      [entity1 entity2])))


(s/fdef narrow-phase
  :args (s/cat :collider-pairs (s/coll-of (s/tuple ::entity ::entity)))
  :ret (s/coll-of ::collision-info))

(s/fdef detect-collisions
  :args (s/cat :entities (s/coll-of ::entity))
  :ret (s/coll-of ::collision-info))

(defn detect-collisions
  "Detects collisions among a collection of entities by combining broad-phase and narrow-phase detection."
  [entities]
  (let [potential-collisions (broad-phase entities)]
    (narrow-phase potential-collisions)))

(defn- narrow-phase
  "Checks each pair of colliders for actual collision and returns a collection of collision-info structures."
  [collider-pairs]
  (keep (fn [[entity1 entity2]]
          (let [{pos1 :position, collider1 :collider} entity1
                {pos2 :position, collider2 :collider} entity2]
            (collides? pos1 collider1 pos2 collider2)))
        collider-pairs))
