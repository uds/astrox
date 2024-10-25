(ns tails.physics.collision-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [tails.physics.collision :as c]
            [tails.math.vector2d :as v]))

(deftest test-circle-vs-circle?
  (testing "No collision when circles are apart"
    (is (nil? (#'c/circle-vs-circle? (v/vec2d 0 0) 1 (v/vec2d 3 0) 1))))

  (testing "Collision when circles overlap"
    (let [collision-info (#'c/circle-vs-circle? (v/vec2d 0 0) 2 (v/vec2d 3 0) 2)]
      (is (not (nil? collision-info)))
      (is (= (:penetration collision-info) 1))
      (is (= (:normal collision-info) (v/normalize (v/vec2d 3 0))))))

  (testing "Collision when circles are at the same position"
    (let [collision-info (#'c/circle-vs-circle? (v/vec2d 0 0) 1 (v/vec2d 0 0) 1)]
      (is (not (nil? collision-info)))
      (is (= (:penetration collision-info) 1))
      (is (= (:normal collision-info) v/right)))))

(deftest test-collides?
  (testing "Circle colliders collision detection"
    (let [circle-collider1 {:shape :circle :radius 1}
          circle-collider2 {:shape :circle :radius 1}]
      (is (nil? (#'c/collides? (v/vec2d 0 0) circle-collider1 (v/vec2d 3 0) circle-collider2)))
      (is (not (nil? (#'c/collides? (v/vec2d 0 0) circle-collider1 (v/vec2d 1 0) circle-collider2))))))

  (testing "Unsupported collider type throws exception"
    (let [circle-collider {:shape :circle :radius 1}
          rectangle-collider {:shape :rectangle :size (v/vec2d 2 2)}]
      (is (thrown? ExceptionInfo (#'c/collides? (v/vec2d 0 0) circle-collider (v/vec2d 0 0) rectangle-collider))))))
