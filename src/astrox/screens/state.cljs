(ns astrox.screens.state
  (:require [tails.rinc.reaction :as rn]))

(def !current-screen (rn/ratom nil))

(def !options-dialog-open? (rn/ratom false))

(def !credits-dialog-open? (rn/ratom false))

(def !pause? (rn/ratom false))

(def !loading? (rn/ratom false))


(defn set-current-screen [screen]
  (reset! !current-screen screen))

(defn open-options-dialog [open?]
  (reset! !options-dialog-open? open?))
  
(defn open-credits-dialog [open?]
  (reset! !credits-dialog-open? open?))

(defn pause-game [pause?]
  (reset! !pause? pause?))

(defn clear 
  "Reset screen's global state. 
   Allows application to reload into the initial state when hot-deploying."
  []
  (reset! !current-screen nil)
  (reset! !options-dialog-open? nil)
  (reset! !credits-dialog-open? nil)
  (reset! !pause? false)
  (reset! !loading? false))