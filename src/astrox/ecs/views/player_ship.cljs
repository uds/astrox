(ns astrox.ecs.views.player-ship
  (:require [pixi.js :refer (Container)]
            [tails.pixi.core :as px]
            [astrox.ecs.views.protocols :as prot]
            [astrox.ecs.views.common :as cmn]
            [tails.math.core :as math]))


(defn- update-shield
  "Updates the shield sprite based on the strength value in [0..1] range.
   Returns the shield sprite or nil, if shield strength was depleted."
  [^js shield-sprite strength]
  (let [images [nil "shield1.png" "shield2.png" "shield3.png"]]
    (cmn/update-sprite-texture shield-sprite images strength))
  ;; shift shield a bit to compensate for the shield texture's skewed aspect ratio
  (.. shield-sprite -anchor (set 0.5 (cmn/mul-aspect shield-sprite 0.5))))

(defn- update-damage
  "Updates the damage sprite based on the damage value in [0..1] range.
   Returns the damage sprite or nil, if no damage is visible."
  [^js damage-sprite damage]
  (let [images [nil "playerShip1_damage1.png" "playerShip1_damage2.png" "playerShip1_damage3.png"]]
    (cmn/update-sprite-texture damage-sprite images damage)))

(defn- update-speed
  "Updates the scale of the exhaust sprite based on the speed value in [0..1] range."
  [^js exhaust-sprite speed]
  ;; shift scale so the small exhaust will be shown even when the thrust is low
  (let [scale  (if (> speed 0.1) (+ 0.6 (* 1 speed)) 0)]
    (set! (.-scale.x exhaust-sprite) scale)
    (set! (.-scale.y exhaust-sprite) scale)))


(deftype
 ^{:doc "A view data type is a container that holds multiple sprites representing different aspects of the game object.
          It is mutable to allow for changing the sprites during the game object's life cycle.
          It exposes methods to manipulate the view's state."}
 PlayerShip [root-sprite
             hull-sprite
             damage-sprite
             shield-sprite
             exhaust-sprite
             hitbox-sprite
             ^:mutable collider-cache   ;; cached collider definition, invalidated on game object shape change
             ^:mutable health
             ^:mutable shield
             ^:mutable speed]

  prot/GameObject

  (root-sprite [_this] root-sprite)
  (destroy [_this] (px/destroy-cascade root-sprite))

  (get-collider [_this]
    (when-not collider-cache
      (set! collider-cache (cmn/infer-collider hull-sprite)))
    collider-cache)

  (set-position [_this pos] (px/set-pos root-sprite pos))

  (set-orientation [_this angle] (set! (.-rotation root-sprite) angle))

  prot/Debuggable

  (show-collider [this]
    (->> (prot/get-collider this)
         (cmn/draw-collider-hitbox hitbox-sprite)))

  (hide-collider [_this] (set! (.-visible hitbox-sprite) false))

  prot/Destructible

  (set-health [this health]
    (when (._health-changed? this health)
      (update-damage damage-sprite (- 1 health))))

  (set-shield [this strength]
    (when (._shield-changed? this strength)
      (update-shield shield-sprite strength)
      ;; update collider shape  when the shield changes (e.g. when shield is depleted)
      (._update-collider this)))

  prot/SelfPropelled

  (set-speed [this speed]
    (when (._speed-changed? this speed)
      (update-speed exhaust-sprite speed)))

  Object

  (_update-collider [this]
    ;; invalidate collider data cache
    (set! collider-cache nil)
    ;; re-draw collider if it's visible
    (when (.-visible hitbox-sprite)
      (prot/show-collider this)))

  (_health-changed? [_this new-health]
    (not (math/approx= new-health health 0.01)))

  (_shield-changed? [_this new-shield]
    (not (math/approx= new-shield shield 0.01)))

  (_speed-changed? [_this new-thrust]
    (not (math/approx= new-thrust speed 0.01))))


(defn create-player-ship
  "Creates a ship view"
  []
  (let [root     (Container.)
        hull     (px/sprite "playerShip1_orange.png")
        damage   (px/sprite)
        shield   (px/sprite)
        exhaust  (px/sprite "fire17.png")
        hitbox   (px/graphics)]

    (.addChild root hull)
    (.addChild root exhaust)
    (.addChild root hitbox)
    ;; damage and shield sprites are added to the hull sprite so the hitbox and exhaust sprites are not affecting ship dimensions
    (.addChild hull shield)
    (.addChild hull damage)

    (.. hull -anchor (set 0.5))
    (.. damage -anchor (set 0.5))
    (.. exhaust -anchor (set 0.5 0))
    (.. exhaust -position (set 0 40))

    (update-damage damage 1)
    (update-speed exhaust 0)
    (update-shield shield 0.2)

    (->PlayerShip root hull damage shield exhaust hitbox nil 1 1 0)))
