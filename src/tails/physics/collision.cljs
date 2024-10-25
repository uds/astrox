(ns tails.physics.collision
  "Collision detection and resolution."
  (:require [clojure.spec.alpha :as s]
            [tails.math.vector2d :as v]
            [tails.physics.core :as p]))


(s/def ::penetration number?)
(s/def ::normal ::v/vector2d)
(s/def ::collision-info (s/keys :req-un [::penetration ::normal]))
(s/def ::entity (s/keys :req-un [::p/position ::p/collider]))


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
  :args (s/cat :pos1 ::v/vector2d, :collider1 ::p/collider, :pos2 ::v/vector2d, :collider2 ::p/collider)
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
  ;; TODO: use sequence partitioning to avoid checking the same pair twice?
  (for [i (range (count entities))
        j (range (inc i) (count entities))
        :let [entity1 (nth entities i)
              entity2 (nth entities j)]
        :when (not (nil? (collides? (:position entity1) (:collider entity1)
                                    (:position entity2) (:collider entity2))))]
    [entity1 entity2]))

;; Benchmark for broad-phase function
(comment
  (let [num-entities 1000
        entities (for [i (range num-entities)]
                   {:position (v/vec2d (rand-int 1000) (rand-int 1000))
                    :collider {:shape :circle :radius (rand-int 10)}})
        start-time (System/nanoTime)
        _ (broad-phase entities)
        end-time (System/nanoTime)]
    (println "Broad-phase benchmark:" (/ (- end-time start-time) 1e6) "ms")))
