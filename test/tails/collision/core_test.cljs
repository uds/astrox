(ns tails.collision.core-test
  (:require [clojure.test :refer :all]
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

(deftest test-broad-phase
  (testing "Broad phase collision detection"
    (let [world {}] ;; Mock world
      (is (empty? (collision/broad-phase world))))))

(deftest test-narrow-phase
  (testing "Narrow phase collision detection"
    (let [world {} ;; Mock world
          potential-collisions []]
      (is (empty? (collision/narrow-phase world potential-collisions))))))

(deftest test-calculate-collision-depth
  (testing "Calculate collision depth"
    (let [collider1 (->Collider (v/vec2d 0 0) (v/vec2d 4 4) :rectangle)
          collider2 (->Collider (v/vec2d 2 2) (v/vec2d 4 4) :rectangle)]
      (is (= (v/vec2d 0 0) (collision/calculate-collision-depth collider1 collider2))))))

(deftest test-calculate-repulsion
  (testing "Calculate repulsion"
    (let [entity1 (random-uuid)
          entity2 (random-uuid)
          collision-depth (v/vec2d 1 1)]
      (is (nil? (collision/calculate-repulsion entity1 entity2 collision-depth))))))

(deftest test-resolve-collisions
  (testing "Resolve collisions"
    (let [world {} ;; Mock world
          collisions []]
      (is (nil? (collision/resolve-collisions world collisions))))))
