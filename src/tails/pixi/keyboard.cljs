(ns tails.pixi.keyboard
  (:require [tails.pixi.core]  ;; defines PIXI global var that is needed for pixi-keyboard to work.
            [pixi.js :as pixijs]
            [pixi-keyboard]))


;; The keyboard manager is patched into the PIXI namespace by requiring pixi-keyboard dependency.
(def ^:private manager pixijs/keyboardManager)

;; Expose key codes defined in the pixi-keyboard library.
(def key-code pixijs/keyboard.Key)

(defn pressed? 
  "Returns true if the specified key was pressed"
  [key]
  (.isPressed manager key))

(defn on-pressed
  "Register a callback '(fn f [key] ..)' to be called whenever a key is pressed."
  [f]
  (.on manager "pressed" f))

(defn remove-pressed
  "Remove previously registered callback 'f' for the 'pressed' event type"
  [f]
  (.off manager "pressed" f))

(defn clear-all 
  "Remove all key bindings"
  []
  (.removeAllListeners manager))