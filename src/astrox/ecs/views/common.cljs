(ns astrox.ecs.views.common
  (:require [tails.pixi.core :as px]
            [tails.math.core :as math]))


(defn- compute-size-aabb
  "Computes the axis-aligned bounding box for a given sprite. Returns size as a 2d vector."
  [^js width height rotation]
  ;; Sprite rotation is not "wrapped" to a range but continuously accumulated, thus we have to wrap it manually to [-PI/2 PI/2] angle range.
  (let [angle (js/Math.abs (math/wrap-min-max rotation (/ js/Math.PI -2) (/ js/Math.PI 2)))]
    ;; Compute AABB size based on the sprite orientation in the world coordinates.
    ;; Reference: https://stackoverflow.com/a/6657768
    (if (> angle math/approx-eq-epsilon)
      (let [sin   (js/Math.sin angle)
            cos   (js/Math.cos angle)]
        {:x (+ (* height sin) (* width cos))
         :y (+ (* width sin) (* height cos))})
      {:x width :y height})))

(defn infer-collider
  "Creates collider definition for a given sprite. Uses size of the sprite as a collider size.
   Returns a map with collider definition."
  [^js sprite]
  ;; use local bounds to get the compound sprite dimensions (e.g. including it's children)
  (let [local-bounds (.getLocalBounds sprite)
        width  (.-width local-bounds)
        height (.-height local-bounds)
        ratio  (/ (js/Math.max width height) (js/Math.min width height))]

    ;; based on the sprite dimensions, infer most appropriate collider shape and size
    (if (<= ratio 1.2)
      {:shape :circle, :radius (/ (js/Math.max width height) 2)}
      (let [parent-rotation (.-rotation (or (.-parent sprite) sprite))
            size (compute-size-aabb width height parent-rotation)]
        {:shape :rectangle, :size size}))))


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

(defn axis-align-hitbox
  "Aligns the hitbox sprite with the world up vector."
  [hitbox-sprite]
  (when (.-visible hitbox-sprite)
    ;; compute hitbox angle relative to the world matrix
    (let [world-matrix (.. hitbox-sprite -parent -worldTransform)
          world-angle (js/Math.atan2 (.-b world-matrix) (.-a world-matrix))
          world-up    (- js/Math.PI)]
      (set! (.-rotation hitbox-sprite) (- world-up world-angle)))))

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
