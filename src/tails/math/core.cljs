(ns tails.math.core
  (:require
   ["./random.js" :as rnd]))

(defn rad->deg 
  "Converts angle from radians to degrees"
  [rad]
  (* rad (/ 180 js/Math.PI)))

(defn deg->rad
  "Converts angle from degrees to radians"
  [deg]
  (* deg (/ js/Math.PI 180)))

(def approx-decimals 5)
(def approx-eq-epsilon 0.00001)

(defn approx=
  "Returns true if the float numbers are approximately equal within tolerance epsilon"
  ([v1 v2 epsilon]
   (< (js/Math.abs (- v1 v2)) epsilon))
  ([vi v2] 
   (approx= vi v2 approx-eq-epsilon)))

(defn floor [n] 
  (js/Math.floor n))

(defn to-fixed
  "Non precise rounding to fixed number of decimals.
   See: https://stackoverflow.com/questions/11832914/how-to-round-to-at-most-2-decimal-places-if-necessary"
  [n decimals]
  (let [nn (+ n js/Number.EPSILON)
        k  (js/Math.pow 10 decimals)]
    (/ (js/Math.round (* nn k)) k)))

(defn to-approx [n]
  (to-fixed n approx-decimals))

(defn clamp-abs
  "Returns value clamped to the absolute maximum value."
  [v max-abs]
  (if (and (some? max-abs) (> (js/Math.abs v) max-abs))
    (* (js/Math.sign v) max-abs)
    v))

(defn wrap-max
  "Wrap x -> [0, max) range."
  [x max]
  ;; js-mod is equivalent to C++ fmod()
  (js-mod (+ max (js-mod x max)) max))

(defn wrap-min-max
  "Wrap x -> [min, max) range."
  [x min max]
  (+ min (wrap-max (- x min) (- max min))))

(defn wrap-pi 
  "Wraps an input angle to the range [+PI, -PI).
   Based on this answer: https://stackoverflow.com/a/29871193"
  [angle]
  (wrap-min-max angle (- js/Math.PI) js/Math.PI))

(defn create-rand-fn
  "Creates random generator function with the given seed"
  [seed-str]
  (rnd/createPrang seed-str))

(def ^:private rand-num*
  "Defines a function that returns pseudo-random number in [0, 1) range."
  (create-rand-fn (-> js/Date.new str)))

(defn rand-num
  "Returns next random number:
   - in a [0, 1) range, if function is called without parameters.
   - in a [0, n) range, if function is called with 'n' parameter.
   - in a [from, to) range, if function is called with 'from' and 'to' parameters."
  ([] (rand-num*))
  ([n] (* (rand-num*) n))
  ([from to] (+ from (* (rand-num*) (- to from)))))

(defn zero-if-near
  "Zeros a number if it is within the specified epsilon."
  [value epsilon]
  (if (<= (js/Math.abs value) epsilon)
    0
    value))
