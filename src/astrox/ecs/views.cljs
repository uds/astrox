(ns astrox.ecs.views
  "Game object views. A view is a visualization of the ECS entity."
  ;; View components for rendering.
  (:require [pixi.js :refer (Container)]
            [tails.math.core :as math]
            [tails.pixi.core :as px]))

(defn- mul-aspect
  "Computes aspect by dividing the object's width by it's height and multiplies resulting aspect by value 'k'."
  [obj k]
  (* k (/ (.-width obj) (.-height obj))))
    

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
    (.. hull -anchor (set 0.5 (mul-aspect hull 0.5)))
    (.. damage -anchor (set 0.5 (mul-aspect damage 0.5)))
    (.. flame -anchor (set 0.5 0))
    (.. flame -position (set 0 35))
    ship))

(defn create-meteor
  "Creates a meteor"
  []
  (let [meteors ["meteorBrown_big1.png"
                 "meteorBrown_big2.png"
                 "meteorBrown_big3.png"
                 "meteorBrown_big4.png"]
        idx     (math/floor (math/rand-num 0 (count meteors)))
        meteor  (px/sprite (nth meteors idx))]
    
    (.. meteor -anchor (set 0.5))
    meteor))
