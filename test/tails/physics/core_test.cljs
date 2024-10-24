(ns tails.physics.core-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [tails.physics.core :refer [integration-step]]
            [tails.math.vector2d :as v]))

(deftest test-integration-step
  (testing "integration-step updates rigid body correctly"
    (let [initial-rigid-body {:position         (v/vector 0 0)
                              :orientation      0
                              :velocity         (v/vector 1 0)
                              :angular-velocity 0
                              :force            (v/vector 0 0)
                              :torque           0
                              :inverse-mass     1
                              :inverse-inertia  1
                              :linear-damping   0.1
                              :angular-damping  0.1}
          delta-time 1
          updated-rigid-body (integration-step initial-rigid-body delta-time)]
      (is (= (:position updated-rigid-body) (v/vector 0.9 0)))
      (is (= (:orientation updated-rigid-body) 0)))))
