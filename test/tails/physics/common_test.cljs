(ns tails.physics.common-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [tails.physics.common :as pc]
            [tails.math.vector2d :as v]))

(deftest apply-impulse-test
  (testing "apply-impulse function"
    (let [rigid-body {:velocity (v/vec2d 0 0)
                      :inverse-mass (/ 1 10)}
          impulse (v/vec2d 10 0)
          updated-body (pc/apply-impulse rigid-body impulse)]
      (is (= (v/vec2d 1 0) (:velocity updated-body))))))
