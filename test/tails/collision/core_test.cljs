(ns tails.collision.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [tails.collision.core :as collision]
            [tails.math.vector2d :as v]))

(deftest test-circle-circle-collision?
  (testing "Circle-circle collision detection"
    (is (collision/circle-circle-collision? (v/vec2d 0 0) 5 (v/vec2d 3 4) 5))
    (is (not (collision/circle-circle-collision? (v/vec2d 0 0) 5 (v/vec2d 10 10) 5)))))

(deftest test-rectangle-rectangle-collision?
  (testing "Rectangle-rectangle collision detection"
    (is (collision/rectangle-rectangle-collision? (v/vec2d 0 0) (v/vec2d 4 4) (v/vec2d 2 2) (v/vec2d 4 4)))
    (is (not (collision/rectangle-rectangle-collision? (v/vec2d 0 0) (v/vec2d 4 4) (v/vec2d 10 10) (v/vec2d 4 4))))))

