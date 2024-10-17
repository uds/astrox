(ns astrox.ecs.views.common
  (:require [tails.pixi.core :as px]))

(defn draw-collider-hitbox
  [graphics collider]
  (set! (.-visible graphics) true)
  (let [shape (:shape collider)]
    (case shape
      :circle     (let [radius (:radius collider)]
                    (px/draw-hollow-circle graphics 0 0 radius 0x00FF00))
      :rectangle  (let [{width :x height :y} (:size-aabb collider)]
                    (px/draw-frame graphics (/ width -2) (/ height -2) width height 0x00FF00))
      (throw (js/Error. "Unsupported collider shape.")))))

(defn mul-aspect
  "Computes aspect by dividing the object's width by it's height and multiplies resulting aspect by value 'k'."
  [obj k]
  (* k (/ (.-width obj) (.-height obj))))
