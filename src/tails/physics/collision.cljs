(ns tails.physics.collision
  "Collision detection and resolution."
  (:require [clojure.spec.alpha :as s]
            [tails.math.vector2d :as v]
            [tails.physics.common :as c]
            [tails.math.vector2d :as v]))

(defrecord QuadTree [boundary capacity points divided? nw ne sw se])

(defn- make-quad-tree [boundary capacity]
  (->QuadTree boundary capacity [] false nil nil nil nil))

(defn- subdivide [qt]
  (let [{:keys [boundary capacity]} qt
        {:keys [x y w h]} boundary
        half-w (/ w 2)
        half-h (/ h 2)
        nw-boundary {:x x :y y :w half-w :h half-h}
        ne-boundary {:x (+ x half-w) :y y :w half-w :h half-h}
        sw-boundary {:x x :y (+ y half-h) :w half-w :h half-h}
        se-boundary {:x (+ x half-w) :y (+ y half-h) :w half-w :h half-h}]
    (assoc qt
           :divided? true
           :nw (make-quad-tree nw-boundary capacity)
           :ne (make-quad-tree ne-boundary capacity)
           :sw (make-quad-tree sw-boundary capacity)
           :se (make-quad-tree se-boundary capacity))))

(defn- insert [qt point]
  (let [{:keys [boundary capacity points divided?]} qt
        {:keys [x y w h]} boundary
        {:keys [px py]} point]
    (if (or (< px x) (> px (+ x w)) (< py y) (> py (+ y h)))
      qt
      (if (< (count points) capacity)
        (assoc qt :points (conj points point))
        (if-not divided?
          (let [subdivided (subdivide qt)]
            (reduce insert subdivided (conj points point)))
          (-> qt
              (update :nw insert point)
              (update :ne insert point)
              (update :sw insert point)
              (update :se insert point)))))))

(defn- query-range [qt range found]
  (let [{:keys [boundary points divided? nw ne sw se]} qt
        {:keys [x y w h]} boundary
        {:keys [rx ry rw rh]} range]
    (if (or (< (+ x w) rx) (> x (+ rx rw)) (< (+ y h) ry) (> y (+ ry rh)))
      found
      (let [found (reduce (fn [acc p]
                            (let [{:keys [px py]} p]
                              (if (and (>= px rx) (<= px (+ rx rw)) (>= py ry) (<= py (+ ry rh)))
                                (conj acc p)
                                acc)))
                          found
                          points)]
        (if-not divided?
          found
          (-> found
              (query-range nw range)
              (query-range ne range)
              (query-range sw range)
              (query-range se range)))))))

(defn- broad-phase-optimized
  "Returns a sequence of pairs of entities that are potentially colliding using a quad tree."
  [entities]
  (let [boundary {:x 0 :y 0 :w 1000 :h 1000}
        capacity 4
        qt (reduce insert (make-quad-tree boundary capacity) entities)]
    (for [entity entities
          :let [range {:x (:x (:position entity)) :y (:y (:position entity)) :w 1 :h 1}]
          other (query-range qt range [])]
      [entity other])))


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
  ;; converting to vector makes it a bit faster, probably because of random access
  (let [indexed (vec (map-indexed vector entities))]
    (for [[i entity1] indexed
          [_ entity2] (drop (inc i) indexed)]
      [entity1 entity2])))

;; Benchmark for broad-phase function
(comment
  ;; for nested loop           - 500 -> 5000ms
  ;; for map-indexed           - 1000 -> 360ms
  ;; for map-indexed #2        - 1000 -> 210ms
  ;; for map-indexed drop/vec  - 1000 -> 160ms
  
  (let [num-entities 1000
        entities (for [_ (range num-entities)]
                   {:position (v/vec2d (rand-int 1000) (rand-int 1000))
                    :collider {:shape :circle :radius (rand-int 10)}})]
    (time (do (doall (broad-phase entities)) nil)))
  )
