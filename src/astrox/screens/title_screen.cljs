(ns astrox.screens.title-screen
  "Title screen of the game"
  (:require [tails.pixi.core :as px]
            [tails.rinc.rtree :as rt]
            [astrox.screens.core :refer (load-game-screen)]
            [astrox.screens.widgets :as w]
            [astrox.screens.state :as state]
            [astrox.screens.options-dialog :refer (options-dialog)]))

(defn- game-title []
  (px/text-layout "Astro·X" {:fontSize           156
                             :breakWords         false
                             :wordWrap           false
                             :wordWrapWidth      1000
                             :letterSpacing      40
                             :stroke             0xB00000
                             :strokeThickness    3
                             :dropShadow         true
                             :dropShadowColor    0xB00000
                             :dropShadowDistance 10
                             :dropShadowBlur     4
                             :dropShadowAlpha    0.9

                             :position           "center"
                             :marginTop          -150
                             :maxWidth           "80%"
                             :maxHeight          "80%"}))

(defn- main-menu []
  {:content {:start  {:content (px/button "Start" load-game-screen)
                      :styles  {:position "centerTop"}}
             :option {:content (px/button "Option" #(state/open-options-dialog true))
                      :styles  {:position "center"}}
             :credit {:content (px/button "Credits" #(state/open-credits-dialog true))
                      :styles  {:position "centerBottom"}}}
   :styles  {:position  "center"
             :marginTop 100
             :height    200
             :maxWidth  "100%"
             :maxHeight "100%"}})

(defn- controls-help []
  (px/text-layout (str "Controls:\n"
                       "Movement  -  W, S, A, D\n"
                       "Shooting      -  SPACE")
                  {:fontSize           18
                   :stroke             0xB00000
                   :strokeThickness    1
                   :dropShadow         true
                   :dropShadowColor    0xB00000
                   :dropShadowDistance 5
                   :dropShadowBlur     4
                   :dropShadowAlpha    0.9
                   :position           "bottom"
                   :marginLeft         20
                   :marginBottom       40
                   :maxWidth           "80%"
                   :maxHeight          "80%"}))

(defn- credits-dialog []
  (let [credits  (str "Assets from https://www.kenney.nl\n\n"
                      " - space shooter\n"
                      " - UI pack\n"
                      " - UI space pack\n\n\n"
                      "Utility AI inspired by\n\n"
                      " - Kevin Dill & Dave Mark\n\n\n"
                      "Reactive signal graphs inspired by\n\n"
                      " - day8/re-frame\n\n\n"
                      "ECS system inspired by\n\n"
                      " - Mark Mandel's Brute")
        body     (px/text-layout credits {:fill       0x5D5D5D
                                          :fontSize   16
                                          :dropShadow false
                                          :position   "center"})
        on-close #(state/open-credits-dialog  false)]
    (w/dialog-base "Credits" body nil on-close)))


(defn title-screen
  "Creates a title screen. Returns a PIXI Layout instance"
  []
  (let [screen (px/layout {:id      :title-screen
                           :styles  {:position "center"
                                     :width    "100%"
                                     :height   "100%"}
                           :content {:background (w/background (px/sprite :title-background))
                                     :game-title (game-title)
                                     :help       (controls-help)
                                     :main-menu  (main-menu)}})]
    (rt/!child-when screen state/!options-dialog-open? options-dialog)
    (rt/!child-when screen state/!credits-dialog-open? credits-dialog)
    screen))
