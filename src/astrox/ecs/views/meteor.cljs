(ns astrox.ecs.views.meteor
  (:require [pixi.js :refer (Container)]
            [tails.math.core :as math]
            [tails.pixi.core :as px]
            [astrox.ecs.views.protocols :as prot]
            [astrox.ecs.views.common :as cmn]))

(deftype
 ^{:doc "A view data type is a container that holds multiple sprites representing different aspects of the game object.
          It is mutable to allow for changing the sprites during the game object's life cycle.
          It exposes methods to manipulate the view's state."}
 Meteor [root-sprite
         meteor-sprite
         hitbox-sprite
         ^:mutable collider]

  prot/GameObject

  (root-sprite [_this] root-sprite)
  (destroy [_this] (px/destroy-cascade root-sprite))

  (get-collider [_this] collider)

  (set-position [_this pos] (px/set-pos root-sprite pos))

  (set-orientation [_this angle] (set! (.-rotation root-sprite) angle))

  prot/Debuggable

  (show-collider [this]
    (->> (prot/get-collider this)
         (cmn/draw-collider-hitbox hitbox-sprite)))

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
  (let [root   (Container.)
        meteor (px/sprite (random-meteor-image))
        hitbox (px/graphics)
        ;; collider is never changes and can be inferred from the meteor sprite once
        collider (cmn/infer-collider meteor)]
    (.addChild root meteor)
    (.addChild root hitbox)
    (.. meteor -anchor (set 0.5))
    (->Meteor root meteor hitbox collider)))
