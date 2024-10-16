(ns astrox.ecs.views
  "Game object views. A view is a visualization of the ECS entity."
  (:require [pixi.js :refer (Container)]
            [tails.math.core :as math]
            [tails.pixi.core :as px]))

(defprotocol
 ^{:doc "A game object is a view that represents an entity in the game world."}
 GameObject
  (root-sprite [this] "Returns the view's root sprite.")
  (destroy [this] "Destroys the view.")
  (set-position [this pos] "Sets the view's position.")
  (set-orientation [this angle] "Sets the view's orientation."))

(defprotocol Debuggable
  ^{:doc "A debuggable view is a game object that can show its collider."}
  (show-collider [this collider] "Shows the view's collider.")
  (hide-collider [this] "Hides the view's collider."))

(defprotocol
 ^{:doc "A destructible view is a game object that can be damaged."}
 Destructible
  (set-health [this health] "Sets the view's health level as a value in [0..1] range.")
  (set-shield [this shield] "Sets the view's shield level as a value in [0..1] range."))

(defprotocol
 ^{:doc "A self-propelled view is a game object that can move itself."}
 SelfPropelled
  (set-thrust [this thrust] "Sets the view's thrust level as a value in [0..1] range."))


(defn- draw-collider-hitbox
  [graphics collider]
  (set! (.-visible graphics) true)
  (let [shape (:shape collider)]
    (case shape
      :circle     (let [radius (:radius collider)]
                    (px/draw-hollow-circle graphics 0 0 radius 0x00FF00))
      :rectangle  (let [{width :x height :y} (:size-aabb collider)]
                    (px/draw-frame graphics (/ width -2) (/ height -2) width height 0x00FF00))
      (throw (js/Error. "Unsupported collider shape.")))))



;; ---------------------------------------------------------------------------------------------------------
;; Player ship game object view


(defn- mul-aspect
  "Computes aspect by dividing the object's width by it's height and multiplies resulting aspect by value 'k'."
  [obj k]
  (* k (/ (.-width obj) (.-height obj))))

(defn- shield-image
  "Returns a shield image based on the strength value in [0..1] range."
  [strength]
  (let [images [nil             ;; no shield
                "shield1.png"
                "shield2.png"
                "shield3.png"]
        count   (count images)
        index   (js/Math.ceil (* strength (dec count)))]
    (nth images index)))

(defn- update-shield
  "Updates the shield sprite based on the strength value in [0..1] range.
   Returns the shield sprite or nil, if shield strength was depleted."
  [^js shield-sprite strength]
  (px/set-sprite-texture shield-sprite (shield-image strength))
  ;; shift shield a bit to compensate for the shield texture's skewed aspect ratio
  (.. shield-sprite -anchor (set 0.5 (mul-aspect shield-sprite 0.5))))


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

  GameObject

  (root-sprite [_this] root-sprite)
  (destroy [_this] (px/destroy-cascade root-sprite))

  (set-position [_this pos] (px/set-pos root-sprite pos))
  (set-orientation [_this angle] (set! (.-rotation root-sprite) angle))

  Debuggable

  (show-collider [_this collider] (draw-collider-hitbox hitbox-sprite collider))
  (hide-collider [_this] (set! (.-visible hitbox-sprite) false))

  Destructible
  (set-health [_this health])
  (set-shield [_this strength])

  SelfPropelled
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


;; ---------------------------------------------------------------------------------------------------------
;; Meteor game object view


(deftype
 ^{:doc "A view data type is a container that holds multiple sprites representing different aspects of the game object.
          It is mutable to allow for changing the sprites during the game object's life cycle.
          It exposes methods to manipulate the view's state."}
 Meteor [root-sprite
         hitbox-sprite]

  GameObject

  (root-sprite [_this] root-sprite)
  (destroy [_this] (px/destroy-cascade root-sprite))

  (set-position [_this pos] (px/set-pos root-sprite pos))
  (set-orientation [_this angle] (set! (.-rotation root-sprite) angle))

  Debuggable

  (show-collider [_this collider] (draw-collider-hitbox hitbox-sprite collider))
  (hide-collider [_this] (set! (.-visible hitbox-sprite) false)))


;; Meteor sprites
(def ^:private meteor-images
  ["meteorBrown_big1.png"
   "meteorBrown_big2.png"
   "meteorBrown_big3.png"
   "meteorBrown_big4.png"])

(defn- random-meteor-image
  "Returns a random meteor sprite"
  []
  (let [idx (math/floor (math/rand-num 0 (count meteor-images)))]
    (nth meteor-images idx)))

(defn create-meteor
  "Creates a meteor"
  []
  (let [meteor (px/sprite (random-meteor-image))
        hitbox (px/graphics)]
    (.addChild meteor hitbox)
    (.. meteor -anchor (set 0.5))
    (->Meteor meteor hitbox)))
