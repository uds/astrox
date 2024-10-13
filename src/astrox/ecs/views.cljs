(ns astrox.ecs.views
  "Game object views. A view is a visualization of the ECS entity."
  (:require [pixi.js :refer (Container)]
            [tails.math.core :as math]
            [tails.pixi.core :as px]))

(defprotocol View
  "A view is a container that holds multiple sprites representing different aspects of the game object.
   It is mutable to allow for changing the sprites during the game object's life cycle.
   It exposes methods to manipulate the view's state."
  (set-pos [this pos] "Sets the view's position."))

(defprotocol ShipView
  (set-health [this health] "Sets the view's health level as a value in [0..1] range.")
  (set-shield [this shield] "Sets the view's shield level as a value in [0..1] range.")
  (set-thrust [this thrust] "Sets the view's thrust level as a value in [0..1] range."))

(deftype PlayerShip [sprite health shield thrust]
  View
  (set-pos [_this pos] (px/set-pos sprite pos))
  
  ShipView
  (set-health [_this health] )
  (set-shield [_this shield] )
  (set-thrust [_this thrust] ))



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
  (let [ship   (Container.)
        hull   (px/sprite "playerShip1_orange.png")
        damage (px/sprite "playerShip1_damage2.png")
        shield (px/sprite "shield1.png")
        flame  (px/sprite "fire17.png")]
    (.addChild ship shield)
    (.addChild ship hull)
    (.addChild ship damage)
    (.addChild ship flame)
    (.. shield -anchor (set 0.5 (mul-aspect shield 0.5)))
    (.. hull -anchor (set 0.5))
    (.. damage -anchor (set 0.5))
    (.. flame -anchor (set 0.5 0))
    (.. flame -position (set 0 40))
    (draw-bounding-box ship)
    ship))


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
