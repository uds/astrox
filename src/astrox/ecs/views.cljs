(ns astrox.ecs.views
  "Game object views. A view is a visualization of the ECS entity."
  (:require [pixi.js :refer (Container)]
            [tails.math.core :as math]
            [tails.pixi.core :as px]))

(defprotocol GameObject
  (root-sprite [this] "Returns the view's root sprite.")
  (destroy [this] "Destroys the view.")
  (set-position [this pos] "Sets the view's position.")
  (set-orientation [this angle] "Sets the view's orientation.")
  (show-collider [this collider] "Shows the view's collider.")
  (hide-collider [this] "Hides the view's collider."))

(defprotocol Destructible
  (set-health [this health] "Sets the view's health level as a value in [0..1] range.")
  (set-shield [this shield] "Sets the view's shield level as a value in [0..1] range."))

(defprotocol SelfPropelled
  (set-thrust [this thrust] "Sets the view's thrust level as a value in [0..1] range."))

(deftype
 ^{:doc "A view data type is a container that holds multiple sprites representing different aspects of the game object.
          It is mutable to allow for changing the sprites during the game object's life cycle.
          It exposes methods to manipulate the view's state."}
 PlayerShip [root-sprite
             damage-sprite
             shield-sprite
             exhaust-sprite
             collider-sprite
             ^:mutable health
             ^:mutable shield
             ^:mutable thrust]
  GameObject
  (root-sprite [_this] root-sprite)
  (destroy [_this] (px/destroy-cascade root-sprite))

  (set-position [_this pos] (px/set-pos root-sprite pos))
  (set-orientation [_this angle] (set! (.-rotation root-sprite) angle))

  (show-collider [this collider]
    (println ">>> " collider)
    (let [sprite (px/draw-hollow-circle 0 0 (:radius collider) 0x00FF00)]
      (.addChild ^js root-sprite sprite)
      (set! (.-collider-sprite this) sprite)))
 
  (hide-collider [_this])

  Destructible
  (set-health [_this health])
  (set-shield [_this shield])

  SelfPropelled
  (set-thrust [_this thrust]))


(defn- mul-aspect
  "Computes aspect by dividing the object's width by it's height and multiplies resulting aspect by value 'k'."
  [obj k]
  (* k (/ (.-width obj) (.-height obj))))

(defn- draw-bounding-box
  "Draws a bounding box around the sprite"
  [^js sprite]
  (let [width (.-width sprite)
        height (.-height sprite)]
    (->> ;;(px/draw-frame (/ width -2) (/ height -2) width height 0x00FF00)
     (px/draw-hollow-circle 0 0 (/ (+ width height) 4) 0x00FF00)
     (.addChild sprite))))

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
    (draw-bounding-box meteor)
    meteor))
