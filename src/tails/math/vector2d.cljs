(ns tails.math.vector2d
  (:require [clojure.spec.alpha :as s]
            [tails.math.core :as math]))


(s/def ::x number?)
(s/def ::y number?)
(s/def ::vector2d (s/keys :req-un [::x ::y]))
(s/def ::vector2d-or-scalar (s/alt :vector ::vector2d, :scalar number?))


(s/fdef add :args (s/cat :x number?, :y number?), :ret ::vector2d)

(defn vec2d [x y]
  {:x x, :y y})


;; useful constants
(def zero  (vec2d 0 0))
(def up    (vec2d 0 -1))
(def down  (vec2d 0 1))
(def left  (vec2d -1 0))
(def right (vec2d 1 0))


(s/fdef eq :args (s/alt :two-args (s/cat :v1 ::vector2d, :v2 ::vector2d-or-scalar)
                        :three-args (s/cat :v1 ::vector2d, :v2 ::vector2d-or-scalar, :epsilon number?)), :ret boolean?)

(defn eq
  "Returns true if the vectors are approximately equal within the specified tolerance epsilon"
  ([v1 v2]
   (eq v1 v2 math/approx-eq-epsilon))
  ([{x1 :x y1 :y} v2 epsilon]
   (if (number? v2)
     (and (math/approx= x1 v2 epsilon)
          (math/approx= y1 v2 epsilon))
     (let [{x2 :x y2 :y} v2]
       (and (math/approx= x1 x2 epsilon)
            (math/approx= y1 y2 epsilon))))))


(defn- vec2d-op [op v1 v2]
  (let [{x1 :x y1 :y} v1]
    (if (number? v2)
      {:x (op x1 v2), :y (op y1 v2)}
      (let [{x2 :x y2 :y} v2]
        {:x (op x1 x2), :y (op y1 y2)}))))


(s/fdef add :args (s/cat :v1 ::vector2d, :v2 ::vector2d-or-scalar), :ret ::vector2d)

(defn add
  "Adds two vectors or a scalar to the vector"
  [v1, v2]
  (vec2d-op + v1 v2))


(s/fdef sub :args (s/cat :v1 ::vector2d, :v2 ::vector2d-or-scalar), :ret ::vector2d)

(defn sub
  "Subtract two vectors or a scalar to the vector"
  [v1, v2]
  (vec2d-op - v1 v2))


(s/fdef mul :args (s/cat :v1 ::vector2d, :v2 ::vector2d-or-scalar), :ret ::vector2d)

(defn mul
  "Multiply two vectors or a scalar to the vector"
  [v1, v2]
  (vec2d-op * v1 v2))


(s/fdef div :args (s/cat :v1 ::vector2d, :v2 ::vector2d-or-scalar), :ret ::vector2d)

(defn div
  "Divide two vectors or a scalar to the vector"
  [v1, v2]
  (vec2d-op / v1 v2))


(s/fdef length-squared :args (s/cat :v ::vector2d), :ret number?)

(defn length-squared
  "Returns squared magnitude of the vector"
  [{x :x y :y}]
  (+ (* x x) (* y y)))


(s/fdef length :args (s/cat :v ::vector2d), :ret number?)

(defn length
  "Returns magnitude of the vector"
  [{x :x y :y}]
  (Math/sqrt (+ (* x x) (* y y))))


(s/fdef distance-squared :args (s/cat :v1 ::vector2d, :v2 ::vector2d), :ret number?)

(defn distance-squared
  "Returns squared distance between two 2D points"
  [v1 v2]
  (length-squared (sub v2 v1)))


(s/fdef distance :args (s/cat :v1 ::vector2d, :v2 ::vector2d), :ret number?)

(defn distance 
  "Returns distance between two 2D points"
  [v1 v2]
  (length (sub v2 v1)))


(s/fdef normalize :args (s/cat :v ::vector2d), :ret ::vector2d)

(defn normalize
  "Returns an unit vector - a vector with the same direction but magnitude equal 1."
  [v]
  (div v (length v)))


(s/fdef clamp :args (s/cat :v ::vector2d, :max-length (s/nilable number?)), :ret ::vector2d)

(defn clamp
  "Clamps vector magnitude to the specified max-length"
  [v max-length]
  (let [len (length v)]
    (if (and (some? max-length) (> len max-length))
      (mul v (/ max-length len))
      v)))


(s/fdef to-fixed :args (s/cat :v ::vector2d, :b number?), :ret ::vector2d)

(defn to-fixed
  "Rounds vector to fixed number of decimals"
  [{x :x y :y} n]
  (vec2d (math/to-fixed x n) (math/to-fixed y n)))

(defn to-approx [v]
  (to-fixed v math/approx-decimals))


(s/fdef rotate :args (s/cat :v1 ::vector2d, :angle number?), :ret ::vector2d)

(defn rotate
  "Rotate vector by a specified angle. Angle is in radians."
  [{x :x, y :y}, angle]
  (let [cos (Math/cos angle)
        sin (Math/sin angle)]
    {:x (- (* x cos) (* y sin))
     :y (+ (* x sin) (* y cos))}))

(s/fdef angle :args (s/cat :v1 ::vector2d), :ret number?)

(defn angle
  "Returns an angle of the vector in radians.
   The angle is 0 when vector is aligned with -Y axis (pointing down)."
  [{x :x, y :y}]
  (- (js/Math.atan2 x y)))


(s/fdef zero-if-near :args (s/cat :v ::vector2d, :epsilon number?), :ret ::vector2d)

(defn zero-if-near
  "Rounds vector components to zero if they are within the specified epsilon."
  [{x :x y :y} epsilon]
  (vec2d (math/zero-if-near x epsilon) (math/zero-if-near y epsilon)))


(s/fdef rand-in-circle :args (s/alt :1 (s/cat :center ::vector2d, :radius number? :rand-fn fn?)
                                    :2 (s/cat :center ::vector2d, :radius number?))
  :ret ::vector2d)

(defn rand-in-circle
  "Returns a random point in a circle with a given radius and a center.
   See https://stackoverflow.com/a/50746409"
  ([center radius rand-fn]
   (let [r (* radius (js/Math.sqrt (rand-fn)))
         theta (* (rand-fn) 2 js/Math.PI)
         {cx :x, cy :y} center]
     {:x (+ cx (* r (js/Math.cos theta)))
      :y (+ cy (* r (js/Math.sin theta)))}))
  ([center radius]
   (rand-in-circle center radius math/rand-num)))
