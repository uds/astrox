(ns tails.physics.collision
  "Collision detection and resolution."
  (:require [clojure.spec.alpha :as s]
            [tails.math.vector2d :as v]
            [tails.physics.common :as c]))


(s/def ::entity (s/keys :req-un [::c/position
                                 ::c/velocity
                                 ::c/restitution
                                 ::c/restitution]
                        :opt-un [::c/collider]))
(s/def ::collider-pairs (s/coll-of (s/tuple ::entity ::entity)))

(s/def ::entity1 ::entity)
(s/def ::entity2 ::entity)
(s/def ::penetration number?)
(s/def ::normal ::v/vector2d)
(s/def ::collision-info (s/keys :req-un [::entity1
                                         ::entity2
                                         ::penetration
                                         ::normal]))


(s/fdef circle-vs-circle?
  :args (s/cat :pos1 ::v/vector2d, :radius1 number?, :pos2 ::v/vector2d, :radius2 number?)
  :ret (s/nilable ::collision-info))

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


(defmulti ^:private collides? 
  "Determines if a collision occurs between two entities based on their position and collider data.
   Returns [::collision-info] if collision occurs, nil otherwise."
  (fn [entity1 entity2] 
    [(-> entity1 :collider :shape) (-> entity2 :collider :shape)]))

;; Check for collision between two circles
(defmethod collides? [:circle :circle]
  [entity1 entity2] 
  (let [{pos1 :position, {radius1 :radius} :collider} entity1
        {pos2 :position, {radius2 :radius} :collider} entity2]
    (when-let [info (circle-vs-circle? pos1 radius1 pos2 radius2)]
      (assoc info :entity1 entity1 :entity2 entity2))))

(defmethod collides? :default
  [entity1 entity2]
  (throw (ex-info "Unsupported collider type" {:entity1 entity1, :entity2 entity2})))


(s/fdef broad-phase
  :args (s/cat :entities (s/nilable (s/coll-of ::entity)))
  :ret (s/nilable ::collider-pairs))

(defn- broad-phase
  "Returns a sequence of pairs of entities that are potentially colliding."
  [entities]
  ;; Converting to vector makes it a bit faster, because drop on vector is faster then on the list
  ;; But it is still probably requires a quad tree optimisation for large number of entities
  (let [indexed (->> (filter :collider entities)
                     (map-indexed vector)
                     (vec))]
    (for [[i entity1] indexed
          [_ entity2] (drop (inc i) indexed)]
      [entity1 entity2])))


(s/fdef narrow-phase
  :args (s/cat :collider-pairs (s/nilable ::collider-pairs))
  :ret (s/nilable (s/coll-of ::collision-info)))

(defn- narrow-phase
  "Checks each pair of colliders for actual collision and returns a collection of collision-info structures."
  [collider-pairs]
  (keep (fn [[entity1 entity2]] (collides? entity1 entity2))
        collider-pairs))


(s/fdef resolve-collision
  :args (s/cat :collision ::collision-info)
  :ret (s/nilable (s/coll-of ::entity)))

(defn- resolve-collision
  "Resolves collision between two entities by applying collision impulse to them. 
   Returns a list of changed entities.
   Reference: https://code.tutsplus.com/how-to-create-a-custom-2d-physics-engine-the-basics-and-impulse-resolution--gamedev-6331t"
  [{:keys [entity1 entity2 normal] :as _collision}]
  (let [rel-vel           (v/sub (:velocity entity2) (:velocity entity1))          ;; relative velocity of entities
        vel-along-normal  (v/dot rel-vel normal)]                                  ;; velocity along the normal
    ;; do not resolve if entities are moving away from each other
    (when (<= vel-along-normal 0)
      (let [{inverse-mass1 :inverse-mass, restitution1 :restitution} entity1
            {inverse-mass2 :inverse-mass, restitution2 :restitution} entity2]
        
        (when (and (zero? inverse-mass1) (zero? inverse-mass2))
          (throw (ex-info "Collision of entities with  infinite mass" {:entity1 entity1, :entity2 entity2})))

        ;; Note that if relative velocity of overlapped entities is 0, the impulse will also be zero and entity will not move
        ;; Not sure how to handle this case (if needed at all), but it is not a problem for now.
        (let [e        (js/Math.min restitution1 restitution2)   ;; coefficient of restitution
              j        (- (* (+ 1 e) vel-along-normal))
              j        (/ j (+ inverse-mass1 inverse-mass2))     ;; impulse magnitude
              impulse  (v/mul normal j)]
          
          (println "Impulse:" e j impulse vel-along-normal)

          [(c/apply-impulse entity1 (v/negate impulse))
           (c/apply-impulse entity2 impulse)])))))


(s/fdef sum-multi-collisions
  :args (s/cat :collided-entities (s/nilable (s/coll-of ::entity)))
  :ret (s/nilable (s/coll-of ::entity)))

;; FIXME: handle multiple collisions by combining impulses on the same entity ->
;;        sum all impulses and apply them at once on the same entity
;;  - check tha this will fix the issue with the ship squeezing between two asteroids
(defn- sum-multi-collisions
  "Post-processing step that Sum all impulses and apply them at once on the same entity."
  [collided-entities]
  (let [grouped (group-by :eid collided-entities)]
    (map (fn [[_ entities]]
              (reduce (fn [entity1 entity2]
                        (let [velocity (v/add (:velocity entity1) (:velocity entity2))]
                          (assoc entity1 :velocity velocity)))
                      entities))
            grouped)))


(s/fdef detect-and-resolve-collisions
  :args (s/cat :entities (s/nilable (s/coll-of ::entity)))
  :ret (s/nilable (s/coll-of ::entity)))

(defn detect-and-resolve-collisions
  "Detects colliding entities and resolves collisions by applying impulse to colliding entities.
   Returns a list of only changed entities."
  [entities]
  (let [collisions (-> (broad-phase entities)
                       (narrow-phase))]
    (-> (mapcat resolve-collision collisions)
        (sum-multi-collisions))))
  