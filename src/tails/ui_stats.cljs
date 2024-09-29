(ns tails.ui-stats
  "Displays UI stats (e.g. FPS) in the debug mode"
  (:require ["stats-js" :as Stats]
            [tails.debug :refer (debug?)]))

(defn- create-stats []
  (when debug?
    (let [stats (Stats.)]
      (.showPanel stats 0)
      (let [style  (-> stats .-dom .-style)]
        (set! (.-position style) "absolute")
        (set! (.-left style) nil)
        (set! (.-top style) nil)
        (set! (.-right style) 0)
        (set! (.-bottom style) 0))
      (js/document.body.appendChild (.-dom stats))
      stats)))

(defonce ^:private stats (delay (create-stats)))

(defn begin-stats 
 "Record begin of the rendering frame for UI statistics.
  Use together with the debug guard to get this call excluded from the release build:
    (when debug? (begin-stats))"
 [] 
 (.begin @stats))

(defn end-stats
  "Record end of the rendering frame for UI statistics.
     Use together with the debug guard to get this call excluded from the release build:
    (when debug? (begin-stats))"
  []
  (.end @stats))
