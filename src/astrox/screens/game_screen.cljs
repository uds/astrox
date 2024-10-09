(ns astrox.screens.game-screen
  "The game screen"
  (:require [tails.pixi.core :as px]
            [tails.pixi.keyboard :as kbd]
            [tails.rinc.rtree :as rt]
            [astrox.ecs.world :as world]
            [astrox.screens.widgets :as w]
            [astrox.screens.state :as state]
            [astrox.screens.options-dialog :refer (options-dialog)]
            [astrox.game.levels :as levels]))


(defn- pause-dialog []
  (let [body {:content {:continue   {:content (px/button "Continue" #(state/pause-game false))
                                     :styles  {:position "centerTop"}}
                        :option     {:content (px/button "Option" #(state/open-options-dialog true))
                                     :styles  {:position "center"}}
                        :title-exit {:content (px/button "Exit" (fn []
                                                                  (world/clear-ecs-world)
                                                                  (state/pause-game false)
                                                                  (state/set-current-screen :title-screen)))
                                     :styles  {:position "centerBottom"}}}
              :styles  {:position  "center"
                        :height    220
                        :marginTop 20}}]
    (w/dialog-base "Pause" body nil #(state/pause-game false))))

(defn- pause-button []
  (px/base-button {:defaultView "yellow_square_btn.png"
                   :hoverView   "yellow_square_hover_btn.png"
                   :pressedView "yellow_square_active_btn.png"
                   :icon        (w/icon-with-shadow "pause_white_icon.png")
                   :iconOffset  {:x 0
                                 :y -1}
                   :padding     5}
                  #(state/pause-game true)))

(defn- background-sprite []
  (let [dim (max js/window.screen.availWidth js/window.screen.availHeight)]
    (px/tiling-sprite-from :game-background dim dim)))

(defn- text [text styles]
  (px/text-layout text (merge {:fontSize        24
                               :stroke          0x000000
                               :strokeThickness 2}
                              styles)))

(defn- life-icon [id]
  (px/layout {:id      id
              :content (px/sprite "playerLife1_orange.png")
              :styles  {:marginRight 10}}))

(defn- score-and-lives [score lives styles]
  (px/layout {:id      :score-and-lives
              :content {:score (text (str "Score: " score) {:position "leftTop"})
                        :lives {:content (map #(life-icon %) (range lives))
                                :styles  {:position "leftBottom"
                                          :width "100%"}}
                        }
              :styles  (merge {:height 60} styles)}))

(defn- high-score [score styles]
  (px/layout {:id      :high-score
              :content {:title (text "High Score:" {:position "centerTop"})
                        :score (text (str score) {:position "centerBottom"})}
              :styles  (merge {:height 60} styles)}))

(defn game-screen
  "Creates a game screen. The 'scene' argument is a PIXI container used to render game objects.
   Returns a PIXI Layout instance."
  [scene]
  (let [screen (px/layout {:id      :game-screen
                           :styles  {:position "center"
                                     :width    "100%"
                                     :height   "100%"}
                           :content {:background   (w/background (background-sprite))
                                     :scene        scene
                                     :score        (score-and-lives 10234 3 {:position   "topLeft"
                                                                             :marginTop  10
                                                                             :marginLeft 15})
                                     :high-score   (high-score 23478905 {:position  "topCenter"
                                                                         :marginTop 10})
                                     :level        (text "Level: 1" {:position    "topRight"
                                                                     :marginTop   10
                                                                     :marginRight 120})
                                     :pause-button {:content (pause-button)
                                                    :styles  {:position    "topRight"
                                                              :marginTop   15
                                                              :marginRight 15}}}})]

    (rt/!child-when screen state/!options-dialog-open? options-dialog)
    (rt/!child-when screen state/!pause? pause-dialog)

    ;; create ECS entities
    (swap! world/!ecs-world levels/create-level01)

    (-> screen
        ;; add handling of the Pause hot key to the game screen 
        (w/with-hotkeys {kbd/key-code.P #(state/pause-game true)}))))
