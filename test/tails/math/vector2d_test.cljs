(ns tails.math.vector2d-test
  (:require [cljs.test :refer [deftest is]]
            [tails.math.vector2d :as v]
            [tails.math.core :as math]
            [tails.init-specs]))

(deftest eq
  (is (v/eq (v/vec2d 0 0) 0))
  (is (v/eq (v/vec2d 1 1) 1))
  (is (v/eq (v/vec2d 0.000001 0.000001) 0))
  (is (not (v/eq (v/vec2d 0.01 0.01) 0)))
  (is (v/eq (v/vec2d 0.000001 0.000001) (v/vec2d 0.00000123 0.00000123)))
  (is (not (v/eq (v/vec2d 0.000001 0.000001) (v/vec2d 0.001 0.001)))))

(deftest add
  (is (= (v/vec2d 4 6) (v/add (v/vec2d 1 2) (v/vec2d 3 4))))
  (is (= (v/vec2d 4 5) (v/add (v/vec2d 1 2) 3))))

(deftest sub
  (is (= (v/vec2d -2 -2) (v/sub (v/vec2d 1 2) (v/vec2d 3 4))))
  (is (= (v/vec2d -2 -1) (v/sub (v/vec2d 1 2) 3))))

(deftest mul
  (is (= (v/vec2d 3 8) (v/mul (v/vec2d 1 2) (v/vec2d 3 4))))
  (is (= (v/vec2d 3 6) (v/mul (v/vec2d 1 2) 3))))

(deftest div
  (is (= (v/vec2d 1 2) (v/div (v/vec2d 2 6) (v/vec2d 2 3))))
  (is (= (v/vec2d 1 3) (v/div (v/vec2d 2 6) 2))))

(deftest length-squared
  (is (= 25 (v/length-squared (v/vec2d 3 4)))))

(deftest length
  (is (= 5 (v/length (v/vec2d 3 4)))))

(deftest distance
  (is (= 5 (v/distance (v/vec2d 1 1) (v/vec2d 4 5))))
  (is (= 5 (v/distance (v/vec2d 4 5) (v/vec2d 1 1)))))

(deftest normalize
  (let [n (v/normalize (v/vec2d 3 4))]
    (is (= (v/vec2d (/ 3 5) (/ 4 5)) n))
    (is (= 1 (v/length n)))))

(deftest clamp
  (let [v1 (v/vec2d 3 4)
        n (v/clamp v1 2)]
    (is (v/eq (-> v1 v/normalize (v/mul 2)) n))
    (is (= 2 (v/length n)))
    (is (= v1 (v/clamp v1 10)))
    (is (= v1 (v/clamp v1 nil)))))

(deftest rotate
  (is (v/eq (v/vec2d 3 4)
            (v/rotate (v/vec2d 3 4) (math/deg->rad 0))))
  (is (v/eq (v/vec2d -4 3)
            (v/rotate (v/vec2d 3 4) (math/deg->rad 90))))
  (is (v/eq (v/vec2d -3 -4)
            (v/rotate (v/vec2d 3 4) (math/deg->rad 180))))
  (is (v/eq (v/vec2d 3 4)
            (v/rotate (v/vec2d 3 4) (math/deg->rad 360)))))


(deftest angle
  (is (= 0 (math/rad->deg (v/angle (v/vec2d 0 0)))))
  (is (= 0 (math/rad->deg (v/angle (v/vec2d 0 5)))))
  (is (= -180 (math/rad->deg (v/angle (v/vec2d 0 -5)))))
  (is (= -90 (math/rad->deg (v/angle (v/vec2d 5 0)))))
  (is (= 90 (math/rad->deg (v/angle (v/vec2d -5 0)))))
  (is (= -45 (math/rad->deg (v/angle (v/vec2d 5 5)))))
  (is (= 45 (math/rad->deg (v/angle (v/vec2d -5 5)))))
  (is (= -135 (math/rad->deg (v/angle (v/vec2d 5 -5)))))
  (is (= 135 (math/rad->deg (v/angle (v/vec2d -5 -5))))))

(deftest vec2d
  (is (= {:x 1, :y 2} (v/vec2d 1 2)))
  (is (= {:x -1, :y -2} (v/vec2d -1 -2))))

(deftest distance-squared
  (is (= 25 (v/distance-squared (v/vec2d 1 1) (v/vec2d 4 5))))
  (is (= 25 (v/distance-squared (v/vec2d 4 5) (v/vec2d 1 1)))))

(deftest to-approx
  (is (= (v/vec2d 1.23457 4.56789) (v/to-approx (v/vec2d 1.2345678 4.5678912))))
  (is (= (v/vec2d 1.0 5.0) (v/to-approx (v/vec2d 1.00001 5.00001)))))

(deftest to-fixed
  ;; Basic rounding
  (is (= (v/vec2d 1.23 4.57) (v/to-fixed (v/vec2d 1.23456 4.56789) 2)))
  (is (= (v/vec2d 1.235 4.568) (v/to-fixed (v/vec2d 1.23456 4.56789) 3)))
  (is (= (v/vec2d 1.2 4.6) (v/to-fixed (v/vec2d 1.23456 4.56789) 1)))
  (is (= (v/vec2d 1.0 5.0) (v/to-fixed (v/vec2d 1.0 5.0) 1)))

  ;; Edge cases
  (is (= (v/vec2d 1.24 4.57) (v/to-fixed (v/vec2d 1.235 4.567) 2)))  ;; Halfway case
  (is (= (v/vec2d 1.24 4.57) (v/to-fixed (v/vec2d 1.2351 4.5671) 2)))
  (is (= (v/vec2d 0.0 0.0) (v/to-fixed (v/vec2d 0.0 0.0) 2)))
  (is (= (v/vec2d -1.23 -4.57) (v/to-fixed (v/vec2d -1.23456 -4.56789) 2)))

  ;; Precision
  (is (= (v/vec2d 1.23456 4.56789) (v/to-fixed (v/vec2d 1.23456 4.56789) 5)))
  (is (= (v/vec2d 1.2346 4.5679) (v/to-fixed (v/vec2d 1.23456 4.56789) 4)))
  (is (= (v/vec2d 1.0 5.0) (v/to-fixed (v/vec2d 1.0 5.0) 0)))
  (is (= (v/vec2d 1.0 5.0) (v/to-fixed (v/vec2d 1.0001 5.0001) 0))))

(deftest rand-in-circle
  (let [c (v/vec2d 2 3)
        r 5]
    (dotimes [_ 100]
      (is (>= r (v/distance (v/rand-in-circle c r) c))))))
