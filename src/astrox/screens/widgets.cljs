(ns astrox.screens.widgets
  "Common UI widgets used on the game screen."
  (:require [tails.pixi.core :as px]
            [tails.pixi.keyboard :as kbd]
            [tails.rinc.rtree :as rt]
            [tails.pixi.rtree]))


(defn with-hotkeys
  "Creates an rtree component with lifecycle methods used to register and unregister hotkey handlers."
  [comp hotkey-handlers]
  (let [on-key-pressed (fn [key]
                         (when-let [handler (get hotkey-handlers key)] (handler)))]
    (reify rt/Component
      (did-mount [_this parent]
        (kbd/on-pressed on-key-pressed)
        (when (rt/component? comp) (rt/did-mount comp parent)))

      (will-unmount [_this parent]
        (kbd/remove-pressed on-key-pressed)
        (when (rt/component? comp) (rt/will-unmount comp parent)))

      (scene-node [_this]
        (if (rt/component? comp) (rt/scene-node comp) comp)))))

(defn background
  "Returns the screen background layout."
  [bg]
  (px/layout {:id      (px/new-layout-id :background)
              :styles  {:background bg
                        :position   "center"
                        :minWidth   "100%"
                        :minHeight  "100%"}}))

(defn icon-with-shadow [asset]
  (-> (px/sprite asset)
      (px/add-drop-shadow-filter {:offset {:x 0 :y 0}})))

(defn- close-dialog-button [on-click]
  {:content (px/base-button {:defaultView "red_circle.png"
                             :hoverView   "yellow_circle.png"
                             :pressedView "red_circle.png"
                             :icon        (icon-with-shadow "cross.png")
                             :iconOffset  {:x 0
                                           :y -1}
                             :padding     5}
                            on-click)
   :styles  {:position    "rightTop"
             :marginTop   -16
             :marginRight -16}})

(defn dialog-base
  "Returns the layout of the basic dialog."
  [title body actions on-close]
  (let [dialog (px/layout {:id      :dialog-base
                           :content {:title     (px/text-layout title {:marginTop  7
                                                                       :marginLeft 20})
                                     :close-btn (close-dialog-button on-close)
                                     :body      body
                                     :actions   (if actions
                                                  {:content actions
                                                   :styles  {:position     "centerBottom"
                                                             :marginBottom 30}}
                                                  {})}
                           :styles  {:background (px/sprite "dialog_panel.png")
                                     :position   "center"
                                     :maxWidth   "90%"
                                     :maxHeight  "90%"}})]

    ;; any other mode will make the dialog "transparent" for clicks, allowing to click on underlying parent's buttons.
    (set! (.-eventMode dialog) "static")

    ;; wraps dialog into the screen overlay that will dim and lock access to the underlying screen
    (-> dialog
        (px/as-modal)
        (with-hotkeys {kbd/key-code.P on-close
                       kbd/key-code.ESCAPE on-close}))))

(defn banner-panel
  "Shows a banner panel (e.g., 'loading...') in the center of the screen."
  [text]
  (px/layout {:id      (px/new-layout-id :banner)
              :content (px/text-layout text {:fontSize 32
                                             :position "center"
                                             :marginLeft 18})
              :styles  {:background (px/sprite :loading_panel)
                        :position   "center"
                        :maxWidth   "90%"
                        :maxHeight  "90%"}}))
