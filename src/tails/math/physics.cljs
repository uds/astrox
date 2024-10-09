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
;; It is selected as shown in https://github.com/jonpena/Cirobb/blob/06e36c514bcfdceb172557f6e1ab41e91752f479/cirobb/Scene.cpp#L103
(def ^:private min-dumping 0.97)

(defn- dumping-k
  "Computes dumping coefficient by mapping dumping value n from [0..1] range into [1 min-dumping] range.
   Dumping coefficient is usually a small value between 0.01 and 0.1 and is applied to e.g. velocity as V = V * (1 - k)."
  [n]
  (js/Math.pow min-dumping n))


(s/fdef integration-step :args (s/cat :rigid-body ::rigid-body, :delta-time number?) :ret ::rigid-body)

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

;; TODO: the collision detection works only for axis-aligned AABB rectangles. 
;; The AABB rectangle should be re-computed from the position and size of the collider on each orientation change.
;; see: https://stackoverflow.com/questions/6657479/aabb-of-rotated-sprite
    

    (assoc rigid-body
           :position         position
           :orientation      orientation
           ;; using "round-to-0" when storing the new velocity values to stop recalculations of the bodies that are changing 
           ;; ever so slightly on each step
           :velocity         (v/zero-if-near velocity 1)
           :angular-velocity (math/zero-if-near ang-velocity 0.01)
           ;; this is an impulse based integration, need to reset forces after the computation step
           :force            v/zero
           :torque           0)))
