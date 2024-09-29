(ns tails.math.core-test
  (:require [cljs.test :refer [deftest is]]
            [tails.math.core :as math]
            [tails.init-specs]))

(deftest rad<->deg
  (is (= 123.45 (math/rad->deg (math/deg->rad 123.45))))
  (is (= 180 (math/rad->deg js/Math.PI)))
  (is (= -180 (math/rad->deg (- js/Math.PI))))
  (is (= 360 (math/rad->deg (* 2 js/Math.PI))))

  (is (= js/Math.PI (math/deg->rad 180)))
  (is (= (* 2 js/Math.PI) (math/deg->rad 360))))

(deftest approx=
  (is (math/approx= 1.2 1.2))
  (is (math/approx= 1.222222 1.222228))   ;; default epsilon is 0.00001
  (is (math/approx= 1.222 1.228, 0.01))

  (is (not (math/approx= 1.2 1.3)))
  (is (not (math/approx= 1.22222 1.22228)))
  (is (not (math/approx= 1.222 1.228, 0.001))))

(deftest clam-abs
  (is (= 2.45 (math/clamp-abs 2.45 2.47)))
  (is (= 2.47 (math/clamp-abs 2.56 2.47)))
  (is (= -2.47 (math/clamp-abs -2.56 2.47))))

(deftest wrap-max
  (is (= 3 (math/wrap-max 3 5)))
  (is (= 1 (math/wrap-max 6 5)))
  (is (= 2 (math/wrap-max -3 5)))
  (is (= 4 (math/wrap-max -6 5)))
  (is (= 0 (math/wrap-max 0 5)))
  (is (= 0 (math/wrap-max 5 5)))
  (is (= 0 (math/wrap-max -5 5))))

(deftest wrap-min-max
  ;; range is [min, max)]
  (is (= -5 (math/wrap-min-max 5 -5 5)))
  (is (= -5 (math/wrap-min-max -5 -5 5)))
  (is (= -4.9 (math/wrap-min-max 5.1 -5 5)))
  (is (= 4.9 (math/wrap-min-max -5.1 -5 5)))

  (is (= 3 (math/wrap-min-max 3 -5 5)))
  (is (= -4 (math/wrap-min-max 6 -5 5)))
  (is (= 1 (math/wrap-min-max 11 -5 5)))
  (is (= -1 (math/wrap-min-max 19 -5 5)))
  (is (= 0 (math/wrap-min-max 20 -5 5)))

  (is (= -3 (math/wrap-min-max -3 -5 5)))
  (is (= 4 (math/wrap-min-max -6 -5 5)))
  (is (= -1 (math/wrap-min-max -11 -5 5)))
  (is (= 1 (math/wrap-min-max -19 -5 5)))
  (is (= 0 (math/wrap-min-max -20 -5 5))))

(deftest wrap-pi
  ;; range is [-PI, +PI)]
  (is (= (- js/Math.PI) (math/wrap-pi js/Math.PI)))
  (is (= (- js/Math.PI) (math/wrap-pi (- js/Math.PI))))

  (is (= (* -0.5 js/Math.PI) (math/wrap-pi (* 1.5 js/Math.PI))))
  (is (= (* -0.5 js/Math.PI) (math/wrap-pi (* 3.5 js/Math.PI))))
  (is (= (* 0.5 js/Math.PI) (math/wrap-pi (* -1.5 js/Math.PI))))
  (is (= (* -0.5 js/Math.PI) (math/wrap-pi (* -4.5 js/Math.PI)))))