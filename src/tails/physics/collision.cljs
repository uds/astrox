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

(defn- positional-correction
  "Proportional correction of the position of the entities to avoid 'sinking objects' effect.
   It should be applied after the collision resolution.
   Returns list of entities with updated positions.
   Reference: https://code.tutsplus.com/how-to-create-a-custom-2d-physics-engine-the-basics-and-impulse-resolution--gamedev-6331t"
  [entity1 entity2 penetration normal]
  (let [percent 0.5      ;; usually 20% t o 80% is enough
        slop    0.01     ;; usually 0.01 to 0.1 is enough
        inv-mass1 (:inverse-mass entity1)
        inv-mass2 (:inverse-mass entity2)
        pen (js/Math.max (- penetration slop) 0)
        inv-mass-sum (* percent (+ inv-mass1 inv-mass2))
        correction (v/mul normal (/ pen inv-mass-sum))
        apply-pos-correction (fn [pos inv-mass]
                               (v/add pos (v/mul correction inv-mass)))]
    [(update entity1 :position apply-pos-correction (- inv-mass1))
     (update entity2 :position apply-pos-correction inv-mass2)]))

(defn- calculate-velocity-along-normal
  "Calculates velocity of entity along the normal vector. 
   Returns nil if entities are moving away from each other."
  [entity1 entity2 normal]
  (let [rel-vel           (v/sub (:velocity entity2) (:velocity entity1))          ;; relative velocity of entities
        vel-along-normal  (v/dot rel-vel normal)]                                  ;; velocity along the normal
    ;; Do not resolve if entities are moving away from each other or do not move.
    ;; Note that if relative velocity of overlapped entities is 0, the impulse will also be zero and entity will not move
    ;; Not sure how to handle this case (if needed at all), but it is not a problem for now.
    (if (<= vel-along-normal 0) vel-along-normal nil)))

(defn- calculate-collision-impulses
  "Calculates impulse for two colliding entities. Returns an impulse magnitude."
  [entity1 entity2 vel-along-normal]
  (let [{inv-mass1 :inverse-mass, rest1 :restitution} entity1
        {inv-mass2 :inverse-mass, rest2 :restitution} entity2]
    (when (and (zero? inv-mass1) (zero? inv-mass2))
      (throw (ex-info "Collision of entities with  infinite mass" {:entity1 entity1, :entity2 entity2})))

    (let [e        (js/Math.min rest1 rest2)          ;; coefficient of restitution
          j        (- (* (+ 1 e) vel-along-normal))
          j        (/ j (+ inv-mass1 inv-mass2))]     ;; impulse magnitude
      j)))


(s/fdef resolve-collision
  :args (s/cat :collision ::collision-info)
  :ret (s/nilable (s/coll-of ::entity)))

(defn- resolve-collision
  "Resolves collision between two entities by applying collision impulse to them. 
   Returns a list of changed entities or nil.
   Reference: https://code.tutsplus.com/how-to-create-a-custom-2d-physics-engine-the-basics-and-impulse-resolution--gamedev-6331t"
  [{:keys [entity1 entity2 penetration normal] :as _collision}]
  (when-let [vel-along-normal (calculate-velocity-along-normal entity1 entity2 normal)]
    (let [j (calculate-collision-impulses entity1 entity2 vel-along-normal)
          impulse (v/mul normal j)
          e1 (c/apply-impulse entity1 (v/negate impulse))
          e2 (c/apply-impulse entity2 impulse)]
      (positional-correction e1 e2 penetration normal))))


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
  