(ns tails.physics.common-test
  (:require [clojure.test :refer :all]
            [tails.physics.common :refer :all]
            [tails.math.vector2d :as v]))

(deftest apply-impulse-test
  (testing "apply-impulse function"
    (let [rigid-body {:velocity (v/vec2d 0 0)
                      :inverse-mass 1}
          impulse (v/vec2d 10 0)
          updated-body (apply-impulse rigid-body impulse)]
      (is (= (v/vec2d 10 0) (:velocity updated-body))))))
