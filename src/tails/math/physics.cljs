(ns tails.math.physics
  "Simple 2D physics simulation"
  (:require [clojure.spec.alpha :as s]
            [tails.math.core :as math]
            [tails.math.vector2d :as v]))

(s/def ::range01 (fn [n] (and (number? n) (<= 0 n) (<= n 1))))

(s/def ::position ::v/vector2d)
(s/def ::orientation number?)
(s/def ::velocity ::v/vector2d)
(s/def ::angular-velocity number?)
(s/def ::force ::v/vector2d)
(s/def ::torque number?)
(s/def ::inverse-mass number?)
(s/def ::density number?)
(s/def ::restitution number?)
(s/def ::linear-damping ::range01)
(s/def ::angular-damping ::range01)

(s/def ::rigid-body (s/keys :opt-un [::position
                                     ::orientation
                                     ::velocity
                                     ::angular-velocity
                                     ::force
                                     ::torque
                                     ::inverse-mass
                                     ::inverse-inertia
                                     ::density
                                     ::restitution
                                     ::linear-damping
                                     ::angular-damping]))

;; Min dumping coefficient value
(def ^:private min-dumping 0.97)

(defn- clump-dumping
  "Clumps dumping to the range [min-dumping 1]"
  [n]
  (js/Math.pow min-dumping n))


(s/fdef integration-step :args (s/cat :rigid-body ::rigid-body, :delta-time number?, :delta-frame number?) :ret ::rigid-body)

(defn integration-step
  "Updates position and orientation of the rigid body based on current force and torque impulses.
   It accepts two delta arguments: 'delta-time' is a duration of the last frame in seconds and 
   'delta-frame' is a relative change of the frame duration, where value 1 is when frame is exactly 1/FPS"
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
    :as   rigid-body} delta-time delta-frame]
  (let [;; damping coefficient is computed as in https://github.com/jonpena/Cirobb/blob/06e36c514bcfdceb172557f6e1ab41e91752f479/cirobb/Scene.cpp#L103
        linear-damping-k (clump-dumping (* linear-damping delta-frame))
        ang-damping-k    (clump-dumping (* angular-damping delta-frame))

        ;; TODO: use drag instead of dumping? 
        ;; see: https://discussions.unity.com/t/how-drag-is-calculated-by-unity-engine/97622/2

        ;; linear velocity
        acceleration     (v/mul force (* inverse-mass delta-time))
        velocity         (v/mul (v/add velocity acceleration) linear-damping-k)

        ;; angular velocity
        ang-acceleration (* torque inverse-inertia delta-time)
        ang-velocity     (* (+ angular-velocity ang-acceleration) ang-damping-k)

        ;; position & orientation
        position         (v/add position (v/mul velocity delta-time))
        orientation      (+ orientation (* ang-velocity delta-time))]

    ;; FIXME: the approximation does not work reliably - it does not stop quickly and it leaves velocity > 0
    ;; https://gafferongames.com/post/integration_basics/
    ;; TODO: replace to-approx with RigidBody.eq?

    ;; using to-approx when storing the new values to stop recalculations of the bodies that are changing 
    ;; ever so slightly on each step
    (assoc rigid-body
           :position         (v/to-approx position)
           :orientation      (math/to-approx orientation)
           :velocity         (v/to-approx velocity)
           :angular-velocity (math/to-approx ang-velocity)
           ;; this is an impulse based integration, need to reset forces after the computation step
           :force            v/zero
           :torque           0)))
