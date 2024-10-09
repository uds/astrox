(ns astrox.screens.main-screen
  "Main screen of the application; a starting point that switches between the game screens."
  (:require [tails.rinc.rtree :as rt]
            [tails.pixi.core :as px]
            [astrox.screens.widgets :as w]
            [astrox.screens.title-screen :refer (title-screen)]
            [astrox.screens.game-screen :refer (game-screen)]
            [astrox.screens.state :refer (!current-screen !loading?)]))


(defn main-screen
  "Returns the current game screen layout node.
   The 'scene' argument is a PIXI container used to render game objects."
  [scene]
  (let [root-layout (px/layout {:id     :root-layout
                                :styles {:width  "100%"
                                         :height "100%"}})]
    (rt/!child root-layout
               (fn [_old]
                 (case @!current-screen
                   :title-screen  (title-screen)
                   :game-screen  (game-screen scene)
                   nil            nil)))
    ;; shows "loading..." banner on long operations
    (rt/!child-when root-layout !loading? #(w/banner-panel "Loading..."))
    root-layout))
