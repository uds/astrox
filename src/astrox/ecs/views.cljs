(ns astrox.ecs.views
  "Game object views. A view is a visualization of the ECS entity."
  (:require [pixi.js :refer (Container)]
            [tails.math.core :as math]
            [tails.pixi.core :as px]
            [clojure.string :as s]))

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


(defn- draw-collider
  "Draws a collider around the sprite"
  [^js sprite collider]
  (let [collider-sprite (case (:shape collider)
                          :circle     (let [radius (:radius collider)]
                                        (px/draw-hollow-circle 0 0 radius 0x00FF00))
                          :rectangle  (let [{width :x height :y} (:size-aabb collider)]
                                        (px/draw-frame (/ width -2) (/ height -2) width height 0x00FF00))
                          (throw (js/Error. "Unsupported collider shape.")))]
    (.addChild sprite collider-sprite)
    collider-sprite))


;; ---------------------------------------------------------------------------------------------------------
;; Player ship game object view


(deftype
 ^{:doc "A view data type is a container that holds multiple sprites representing different aspects of the game object.
          It is mutable to allow for changing the sprites during the game object's life cycle.
          It exposes methods to manipulate the view's state."}
 PlayerShip [root-sprite
             damage-sprite
             shield-sprite
             exhaust-sprite
             ^:mutable collider-sprite
             ^:mutable health
             ^:mutable shield
             ^:mutable thrust]

  GameObject

  (root-sprite [_this] root-sprite)
  (destroy [_this] (px/destroy-cascade root-sprite))

  (set-position [_this pos] (px/set-pos root-sprite pos))
  (set-orientation [_this angle] (set! (.-rotation root-sprite) angle))

  Debuggable

  (show-collider [this collider]
    (->> (draw-collider root-sprite collider)
         (set! (.-collider-sprite this))))

  (hide-collider [this]
    (.destroy collider-sprite)
    (set! (.-collider-sprite this) nil))

  Destructible
  (set-health [_this health])
  (set-shield [_this shield])

  SelfPropelled
  (set-thrust [_this thrust]))


(defn- mul-aspect
  "Computes aspect by dividing the object's width by it's height and multiplies resulting aspect by value 'k'."
  [obj k]
  (* k (/ (.-width obj) (.-height obj))))

(defn create-player-ship
  "Creates a ship view"
  []
  (let [ship     (Container.)
        hull     (px/sprite "playerShip1_orange.png")
        damage   (px/sprite "playerShip1_damage2.png")
        shield   (px/sprite "shield1.png")
        exhaust  (px/sprite "fire17.png")]
    (.addChild ship shield)
    (.addChild ship hull)
    (.addChild ship damage)
    (.addChild ship exhaust)
    (.. shield -anchor (set 0.5 (mul-aspect shield 0.5)))
    (.. hull -anchor (set 0.5))
    (.. damage -anchor (set 0.5))
    (.. exhaust -anchor (set 0.5 0))
    (.. exhaust -position (set 0 40))

    (->PlayerShip ship damage shield exhaust nil 1 1 0)))


;; ---------------------------------------------------------------------------------------------------------
;; Meteor game object view


(deftype
 ^{:doc "A view data type is a container that holds multiple sprites representing different aspects of the game object.
          It is mutable to allow for changing the sprites during the game object's life cycle.
          It exposes methods to manipulate the view's state."}
 Meteor [root-sprite
         ^:mutable collider-sprite]

  GameObject

  (root-sprite [_this] root-sprite)
  (destroy [_this] (px/destroy-cascade root-sprite))

  (set-position [_this pos] (px/set-pos root-sprite pos))
  (set-orientation [_this angle] (set! (.-rotation root-sprite) angle))

  Debuggable

  (show-collider [this collider]
    (->> (draw-collider root-sprite collider)
         (set! (.-collider-sprite this))))

  (hide-collider [this]
    (.destroy collider-sprite)
    (set! (.-collider-sprite this) nil)))


;; Meteor sprites
(def ^:private meteor-assets
  ["meteorBrown_big1.png"
   "meteorBrown_big2.png"
   "meteorBrown_big3.png"
   "meteorBrown_big4.png"])

(defn- random-meteor-asset
  "Returns a random meteor sprite"
  []
  (let [idx (math/floor (math/rand-num 0 (count meteor-assets)))]
    (nth meteor-assets idx)))

(defn create-meteor
  "Creates a meteor"
  []
  (let [meteor (px/sprite (random-meteor-asset))]
    (.. meteor -anchor (set 0.5))
    (->Meteor meteor nil)))
