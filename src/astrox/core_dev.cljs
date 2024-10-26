(ns astrox.core-dev
  "Special DEV entry point for the shadow-cljs hot-loader"
  (:require [orchestra-cljs.spec.test :as st]
            [astrox.core :as core]))

;; application start entry point 
(defn ^:dev/after-load start []
  (core/start)
  ;; NOTE that full SPECS instrumentation will dramatically slow down the game loop
  (if false 
    (js/console.log "SPECS instrumented: " (st/instrument))
    (js/console.warn "WARNING: SPECS are NOT instrumented!")))

;; called by the shadow-cljs hot-loader every time when application about to be reloaded
(defn ^:dev/before-load-async stop [done]
  (core/stop)
  (done))
