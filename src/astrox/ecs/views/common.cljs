(ns astrox.ecs.views.common
  (:require [tails.pixi.core :as px]))

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
