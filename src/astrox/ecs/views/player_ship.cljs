(ns astrox.ecs.views.player-ship
  (:require [pixi.js :refer (Container)]
            [tails.pixi.core :as px]
            [astrox.ecs.views.protocols :as prot]
            [astrox.ecs.views.common :as cmn]))


(defn- update-shield
  "Updates the shield sprite based on the strength value in [0..1] range.
   Returns the shield sprite or nil, if shield strength was depleted."
  [^js shield-sprite strength]
  (let [images [nil "shield1.png" "shield2.png" "shield3.png"]
        image  (cmn/select-image images strength)]
    (px/set-sprite-texture shield-sprite image))
  ;; shift shield a bit to compensate for the shield texture's skewed aspect ratio
  (.. shield-sprite -anchor (set 0.5 (cmn/mul-aspect shield-sprite 0.5))))

(defn- update-damage
  "Updates the damage sprite based on the damage value in [0..1] range.
   Returns the damage sprite or nil, if no damage is visible."
  [^js damage-sprite damage]
  (let [images [nil "playerShip1_damage1.png" "playerShip1_damage2.png" "playerShip1_damage3.png"]
        image  (cmn/select-image images damage)]
    (px/set-sprite-texture damage-sprite image)))


(deftype
 ^{:doc "A view data type is a container that holds multiple sprites representing different aspects of the game object.
          It is mutable to allow for changing the sprites during the game object's life cycle.
          It exposes methods to manipulate the view's state."}
 PlayerShip [root-sprite
             damage-sprite
             shield-sprite
             exhaust-sprite
             hitbox-sprite
             ^:mutable health
             ^:mutable shield
             ^:mutable thrust]

  prot/GameObject

  (root-sprite [_this] root-sprite)
  (destroy [_this] (px/destroy-cascade root-sprite))

  (set-position [_this pos] (px/set-pos root-sprite pos))
  (set-orientation [_this angle] (set! (.-rotation root-sprite) angle))

  prot/Debuggable

  (show-collider [_this collider] (cmn/draw-collider-hitbox hitbox-sprite collider))
  (hide-collider [_this] (set! (.-visible hitbox-sprite) false))

  prot/Destructible
  (set-health [_this health])
  (set-shield [_this strength])

  prot/SelfPropelled
  (set-thrust [_this thrust]))


(defn create-player-ship
  "Creates a ship view"
  []
  (let [ship     (Container.)
        hull     (px/sprite "playerShip1_orange.png")
        damage   (px/sprite "playerShip1_damage2.png")
        shield   (px/sprite)
        exhaust  (px/sprite "fire17.png")
        hitbox   (px/graphics)]
    (.addChild ship shield)
    (.addChild ship hull)
    (.addChild ship damage)
    (.addChild ship exhaust)
    (.addChild ship hitbox)
    (.. hull -anchor (set 0.5))
    (.. damage -anchor (set 0.5))
    (.. exhaust -anchor (set 0.5 0))
    (.. exhaust -position (set 0 40))

    (update-shield shield 0.1)

    (->PlayerShip ship damage shield exhaust hitbox 1 1 0)))
