(ns tails.collision.core
  (:require [clojure.spec.alpha :as s]
            [tails.math.vector2d :as v]
            [tails.ecs.core :as ecs]))

(s/def ::position ::v/vector2d)
(s/def ::radius number?)
(s/def ::size ::v/vector2d)
  

(s/fdef circle-circle-collision?
  :args (s/cat :pos1 ::position :radius1 ::radius :pos2 ::position :radius2 ::radius)
  :ret boolean?)

(defn circle-circle-collision?
  "Detects collision between two circles.
   Returns true if a collision occurred."
  [pos1 radius1 pos2 radius2]
  (let [distance (v/distance pos1 pos2)]
    (< distance (+ radius1 radius2))))


(s/fdef rectangle-rectangle-collision?
  :args (s/cat :pos1 ::position :size1 ::size :pos2 ::position :size2 ::size)
  :ret boolean?)

(defn rectangle-rectangle-collision?
  "Detects collision between two rectangles.
   Returns true if a collision occurred."
  [{x1 :x y1 :y :as _pos1} {w1 :x h1 :y :as _size1} {x2 :x y2 :y :as _pos2} {w2 :x h2 :y :as _size2}]
  (and (< (Math/abs (- x1 x2)) (+ (/ w1 2) (/ w2 2)))
       (< (Math/abs (- y1 y2)) (+ (/ h1 2) (/ h2 2)))))


;; (s/fdef circle-rectangle-collision?
;;   :args (s/cat :circle-pos ::position :circle-radius ::radius :rect-pos ::position :rect-size ::size)
;;   :ret boolean?)

;; (defn circle-rectangle-collision?
;;   "Detects collision between a circle and a rectangle.
;;    Returns a boolean indicating if a collision occurred."
;;   [circle-pos circle-radius rect-pos rect-size]
;;   ;; Implement logic for circle-rectangle collision detection
;;   )


;; (s/fdef broad-phase
;;   :args (s/cat :world ::ecs/world)
;;   :ret (s/coll-of (s/tuple ::ecs/entity-id ::ecs/entity-id)))

;; (defn broad-phase
;;   "Identifies potential collisions using spatial partitioning.
;;    Returns a sequence of entity pairs that may collide."
;;   [world]
;;   (let [entities (ecs/entities-with-component world c/Collider)]
;;     ;; FIXME: the nested loop should not collect all objects but only these that are coming after already seen in outer loop.
;;     ;; e.g. oture loop: for(i = 0; ...), inner loop: for(j = i+1, ...) 
;;     ;; Simple broad phase using pairwise comparison
;;     (for [e1 entities
;;           e2 entities
;;           :when (not= e1 e2)]
;;       [e1 e2])))


;; (s/fdef narrow-phase
;;   :args (s/cat :world ::ecs/world :potential-collisions (s/coll-of (s/tuple ::ecs/entity-id ::ecs/entity-id)))
;;   :ret (s/coll-of (s/tuple ::ecs/entity-id ::ecs/entity-id)))

;; (defn narrow-phase
;;   "Performs detailed collision checks on potential collisions.
;;    Returns a sequence of entity pairs that are actually colliding."
;;   [world potential-collisions]
;;   (filter (fn [[e1 e2]]
;;             (let [collider1 (ecs/component world e1 c/Collider)
;;                   collider2 (ecs/component world e2 c/Collider)
;;                   pos1 (.-position collider1)
;;                   size1 (.-size collider1)
;;                   pos2 (.-position collider2)
;;                   size2 (.-size collider2)]
;;               (case [(.-shape collider1) (.-shape collider2)]
;;                 [:circle :circle] (circle-circle-collision? pos1 size1 pos2 size2)
;;                 [:rectangle :rectangle] (rectangle-rectangle-collision? pos1 size1 pos2 size2)
;;                 [:circle :rectangle] (circle-rectangle-collision? pos1 size1 pos2 size2)
;;                 [:rectangle :circle] (circle-rectangle-collision? pos2 size2 pos1 size1)
;;                 false)))
;;           potential-collisions))


;; (s/fdef calculate-collision-depth
;;   :args (s/cat :collider1 ::c/collider :collider2 ::c/collider)
;;   :ret ::v/vector2d)

;; (defn calculate-collision-depth
;;   "Calculates the depth of the collision between two colliders.
;;    Returns a vector representing the collision depth and direction."
;;   [collider1 collider2]
;;   (let [pos1 (.-position collider1)
;;         size1 (.-size collider1)
;;         pos2 (.-position collider2)
;;         size2 (.-size collider2)]
;;     (case [(.-shape collider1) (.-shape collider2)]
;;       [:circle :circle] (let [depth (- (+ (.-radius size1) (.-radius size2)) (v/distance pos1 pos2))
;;                                direction (v/normalize (v/sub pos2 pos1))]
;;                            (v/mul direction depth))
;;       [:rectangle :rectangle] (let [dx (- (Math/abs (- (.-x pos1) (.-x pos2))) (+ (/ (.-x size1) 2) (/ (.-x size2) 2)))
;;                                       dy (- (Math/abs (- (.-y pos1) (.-y pos2))) (+ (/ (.-y size1) 2) (/ (.-y size2) 2)))
;;                                       depth (min dx dy)
;;                                       direction (v/normalize (v/sub pos2 pos1))]
;;                                   (v/mul direction depth))
;;       [:circle :rectangle] ;; Implement logic for circle-rectangle collision depth
;;       [:rectangle :circle] ;; Implement logic for rectangle-circle collision depth
;;       (v/zero)))) ;; Default to zero vector if no collision


;; (s/fdef calculate-repulsion
;;   :args (s/cat :entity1 ::ecs/entity-id :entity2 ::ecs/entity-id :collision-depth ::v/vector2d)
;;   :ret nil?)

;; (defn calculate-repulsion
;;   "Calculates and applies repulsion forces based on collision depth.
;;    Returns nil as it performs side effects on the entities."
;;   [entity1 entity2 collision-depth]
;;   ;; Calculate and apply repulsion forces based on collision depth
;;   ;; Update the RigidBody components of the entities
;;   )


;; (s/fdef resolve-collisions
;;   :args (s/cat :world ::ecs/world :collisions (s/coll-of (s/tuple ::ecs/entity-id ::ecs/entity-id)))
;;   :ret nil?)

;; (defn resolve-collisions
;;   "Resolves detected collisions by applying repulsion forces.
;;    Returns nil as it performs side effects on the world state."
;;   [world collisions]
;;   (doseq [[e1 e2] collisions]
;;     (let [collider1 (ecs/component world e1 c/Collider)
;;           collider2 (ecs/component world e2 c/Collider)
;;           collision-depth (calculate-collision-depth collider1 collider2)]
;;       (calculate-repulsion e1 e2 collision-depth))))


;; ;; FIXME: Collision detection should happen after all movements were computed and updated as a SEPARATE step, after all movement's has been computed.
;; (defn- collision-detection-system
;;   "Detects and resolves collisions between entities."
;;   [world]
;;   (let [potential-collisions (broad-phase world)
;;         actual-collisions (narrow-phase world potential-collisions)]
;;     (resolve-collisions world actual-collisions)))

