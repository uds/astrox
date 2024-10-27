(ns tails.physics.core
  "Simple 2D physics simulation"
  (:require [tails.math.core :as math]
            [tails.math.vector2d :as v]
            [tails.physics.collision]))


;; Min dumping coefficient value
;; It is selected as shown in https://github.com/jonpena/Cirobb/blob/06e36c514bcfdceb172557f6e1ab41e91752f479/cirobb/Scene.cpp#L103
(def ^:private min-dumping 0.97)

(defn- dumping-k
  "Computes dumping coefficient by mapping dumping value n from [0..1] range into [1 min-dumping] range.
   Dumping coefficient is usually a small value between 0.01 and 0.1 and is applied to e.g. velocity as V = V * (1 - k)."
  [n]
  (js/Math.pow min-dumping n))

(defn integration-step
  "Updates position and orientation of the rigid body based on current force and torque impulses.
   It accepts two delta arguments: 'delta-time' is a duration of the last frame in seconds."
  [{:keys [position
           orientation
           velocity
           angular-velocity
           force
           torque
           inverse-mass
           inverse-inertia
           linear-damping
           angular-damping]
    :as   rigid-body} delta-time]
  (let [linear-damping-k (dumping-k linear-damping)
        ang-damping-k    (dumping-k angular-damping)

        ;; using Semi-implicit Euler integration method, as described here: https://gafferongames.com/post/integration_basics/

        ;; linear velocity
        acceleration     (v/mul force (* inverse-mass delta-time))
        velocity         (v/mul (v/add velocity acceleration) linear-damping-k)

        ;; angular velocity
        ang-acceleration (* torque inverse-inertia delta-time)
        ang-velocity     (* (+ angular-velocity ang-acceleration) ang-damping-k)

        ;; position & orientation
        position         (v/add position (v/mul velocity delta-time))
        orientation      (+ orientation (* ang-velocity delta-time))]

    (assoc rigid-body
           :position         position
           :orientation      orientation
           ;; using "round-to-0" when storing the new velocity values to stop recalculations of the bodies that are changing ever so slightly on each step
           :velocity         (v/zero-if-near velocity 1)
           :angular-velocity (math/zero-if-near ang-velocity 0.01)
           ;; this is an impulse based integration, need to reset forces after the computation step
           :force            v/zero
           :torque           0)))
