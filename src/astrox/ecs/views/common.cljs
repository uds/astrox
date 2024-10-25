(ns astrox.ecs.views.common
  (:require [tails.pixi.core :as px]))


(defn infer-collider
  "Creates collider definition for a given sprite. Uses size of the sprite as a collider size.
   Returns a map with collider definition."
  [^js sprite]
  ;; use local bounds to get the compound sprite dimensions (e.g. including it's children)
  (let [local-bounds (.getLocalBounds sprite)
        width      (.-width local-bounds)
        height     (.-height local-bounds)]

    ;; for now we support only circle shape
    {:shape :circle, :radius (/ (+ width height) 4)}

    ;; based on the sprite dimensions, infer most appropriate collider shape and size
    ;; (let [ratio (/ (js/Math.max width height) (js/Math.min width height))])
    ;;   (if (<= ratio 1.2)
    ;;     {:shape :circle, :radius (/ (js/Math.max width height) 2)}
    ;;     {:shape :rectangle, :size {:x width :y height}}))
    ))

(defn draw-collider-hitbox
  [graphics collider]
  (set! (.-visible graphics) true)
  (let [shape (:shape collider)]
    (case shape
      :circle     (let [radius (:radius collider)]
                    (px/draw-hollow-circle graphics 0 0 radius 0x00FF00))
      :rectangle  (let [{width :x height :y} (:size collider)]
                    (px/draw-frame graphics (/ width -2) (/ height -2) width height 0x00FF00))
      (throw (js/Error. "Unsupported collider shape.")))))

(defn mul-aspect
  "Computes aspect by dividing the object's width by it's height and multiplies resulting aspect by value 'k'."
  [obj k]
  (* k (/ (.-width obj) (.-height obj))))

(defn select-image
  "Selects an image from a list based on a factor in the range [0..1]."
  [images factor]
  (let [count (count images)
        index (js/Math.ceil (* factor (dec count)))]
    (nth images index)))

(defn update-sprite-texture
  "Updates the sprite texture based on a value in [0..1] range using a list of images."
  [^js sprite images factor]
  (->> (select-image images factor)
       (px/set-sprite-texture sprite)))
