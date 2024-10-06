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

(deftest to-fixed
  ;; Basic rounding
  (is (= 1.23 (math/to-fixed 1.23456 2)))
  (is (= 1.235 (math/to-fixed 1.23456 3)))
  (is (= 1.2 (math/to-fixed 1.23456 1)))
  (is (= 1.0 (math/to-fixed 1.0 1)))

  ;; Edge cases
  (is (= 1.24 (math/to-fixed 1.235 2)))  ;; Halfway case
  (is (= 1.24 (math/to-fixed 1.2351 2)))
  (is (= 0.0 (math/to-fixed 0.0 2)))
  (is (= -1.23 (math/to-fixed -1.23456 2)))

  ;; Precision
  (is (= 1.23456 (math/to-fixed 1.23456 5)))
  (is (= 1.2346 (math/to-fixed 1.23456 4)))
  (is (= 1.0 (math/to-fixed 1.0 0)))
  (is (= 1.0 (math/to-fixed 1.0001 0))))

(deftest floor
  (is (= 1 (math/floor 1.9)))
  (is (= -2 (math/floor -1.1)))
  (is (= 0 (math/floor 0.5))))

(deftest to-approx
  (is (= 1.23457 (math/to-approx 1.2345678)))
  (is (= 1.0 (math/to-approx 1.000001))))

(deftest create-rand-fn
  (let [rand-fn (math/create-rand-fn "seed")]
    (is (<= 0 (rand-fn) 1))
    (is (not= (rand-fn) (rand-fn)))))

(deftest rand-num
  (is (<= 0 (math/rand-num) 1))
  (is (<= 0 (math/rand-num 5) 5))
  (is (<= 10 (math/rand-num 10 20) 20)))

(deftest round-to-0
  (is (= 0 (math/round-to-0 0.0001 0.001)))
  (is (= 0.0001 (math/round-to-0 0.0001 0.00005)))
  (is (= 0.1 (math/round-to-0 0.1 0.05)))
  (is (= 0 (math/round-to-0 0.01 0.1)))
  (is (= 0 0 (math/round-to-0 0.00001 0.2))))
