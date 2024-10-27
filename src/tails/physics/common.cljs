(ns tails.physics.common
  (:require [clojure.spec.alpha :as s]
            [tails.math.vector2d :as v]))


(s/def ::position ::v/vector2d)
(s/def ::size ::v/vector2d)
(s/def ::radius number?)
(s/def ::orientation number?)
(s/def ::velocity ::v/vector2d)
(s/def ::angular-velocity number?)
(s/def ::force ::v/vector2d)
(s/def ::torque number?)
(s/def ::inverse-mass number?)
(s/def ::density number?)
(s/def ::restitution number?)

(s/def ::range01 (fn [n] (and (number? n) (<= 0 n) (<= n 1))))
(s/def ::linear-damping ::range01)
(s/def ::angular-damping ::range01)


(s/def :circle/shape #{:circle})
(s/def :rectangle/shape #{:rectangle})

;; Circle collider
(s/def ::circle-collider (s/keys :req-un [:circle/shape ::radius]))

;; Rectangle collider
(s/def ::rectangle-collider (s/keys :req-un [:rectangle/shape ::size]))

(s/def ::collider (s/nilable (s/or :circle ::circle-collider, :rectangle ::rectangle-collider)))

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
                                     ::angular-damping
                                     ::collider]))



(s/fdef apply-impulse 
  :args (s/cat :rigid-body ::rigid-body, :impulse ::v/vector2d)
  :ret ::rigid-body)

(defn apply-impulse
  "Applies impulse to the rigid body. Impulse is a force applied for a single frame."
  [{:keys [velocity inverse-mass] :as rigid-body} impulse]
  (let [velocity (v/add velocity (v/mul impulse inverse-mass))]
    (assoc rigid-body :velocity velocity)))
