(ns astrox.screens.core
  (:require [tails.pixi.core :as px]
            [astrox.screens.state :refer (!loading? !current-screen)]))

(defn- load-screen*
  "Starts loading screen assets and shows the screen once loaded."
  [bundle-key bundle screen-key]
  (reset! !current-screen nil)
  (reset! !loading? true)
  (px/load-assets-bundle bundle-key bundle
                         (fn on-assets-loaded [_assets]
                           (reset! !loading? false)
                           (reset! !current-screen screen-key))))

(defn- load-screen
  "Starts loading screen assets and show the screen once it loaded."
  [bundle-key bundle screen-key]
  (if (px/asset-bundle-loaded? bundle-key)
    (reset! !current-screen screen-key)
    (load-screen* bundle-key bundle screen-key)))

(defn load-title-screen
  "Triggers loading of the title screen."
  []
  (let [bundle {:title-background "images/title-background-2.jpg"
                :ui-assets        "images/ui/ui.json"}]
    (load-screen :title-screen bundle :title-screen)))

(defn load-game-screen
  "Triggers loading of the game screen."
  []
  (let [bundle {:game-background "images/game-background.png"
                :game-assets     "images/game/game.json"}]
    (load-screen :game-screen bundle :game-screen)))
