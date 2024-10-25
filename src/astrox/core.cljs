(ns astrox.core
  (:require [tails.pixi.core :as px]
            [tails.pixi.keyboard :as kbd]
            [tails.rinc.reaction :as r]
            [tails.ecs.core :as ecs]
            [tails.physics.collision :as cn]
            [tails.debug :refer (debug?)]
            [tails.ui-stats :as stats]
            [astrox.screens.main-screen :refer (main-screen)]
            [astrox.screens.core :refer (load-title-screen)]
            [astrox.screens.state :as state]
            [astrox.ecs.world :as world]
            [astrox.ecs.components :as c]
            [astrox.ecs.input :refer (process-input)]
            [astrox.ecs.systems :refer (all-systems)]))


(defn- mount-view [view-el]
  (when-let [old-view-el (js/document.getElementById (.-id view-el))]
    (.remove old-view-el))
  (js/document.body.appendChild view-el))

(defn- make-pause-reaction
  "Makes a reaction that will pause game-ticker on change of the !pause? ratom."
  [game-ticker]
  (r/reaction* (px/pause-ticker game-ticker @state/!pause?)))

;; Record to hold the application data
(defrecord ^:private AppData [^js app, ^js game-ticker, !pause-reaction, resize-app-fn])

(defn- create-app
  "Creates and initializes the application.
   Returns AppData record containing the application data."
  []
  (let [app             (px/create-app "app-view" nil)
        game-ticker     (px/create-ticker)
        !pause-reaction (make-pause-reaction game-ticker)
        resize-app-fn   #(px/resize-app app)]
    (px/setup-cursors app {:default "url('images/ui/cursor_pointer3D_shadow.png'),auto"
                           :pointer "url('images/ui/cursor_hand.png'),auto"})

    (mount-view (.-view app))

    ;; initialize pause reaction
    @!pause-reaction

    ;; we do custom resize here instead of using PIXI's {:resizeTo js/window} option.
    ;; This allows us to hook in resizing of the application layout as well.
    (js/window.addEventListener "resize" resize-app-fn)
    (AppData. app game-ticker !pause-reaction resize-app-fn)))

(defn- destroy-app
  "Destroys the application"
  [{:keys [app game-ticker !pause-reaction resize-app-fn] :as _app-data}]
  (js/window.removeEventListener "resize" resize-app-fn)
  (.destroy game-ticker)
  (r/dispose! !pause-reaction)
  (px/destroy-app app))

(defn- update-world
  "Update ECS world by processing player input and executing given systems against the current state of the world."
  [world systems delta-time delta-frame]
  (-> (process-input world)
      (ecs/systems-tick systems delta-time delta-frame)))

(defn- game-loop
  "Game loop function that is called by the Application.ticker.
   The 'delta-frame' value is passed from the PIXI ticker. 
   It's value will be around 1 when the FPS is around 60."
  [systems delta-frame]
  (when debug? (stats/begin-stats))
  ;; execute ECS systems and update the world
  (let [delta-time (px/delta-frame->delta-time delta-frame)]
    (swap! world/!ecs-world update-world systems delta-time delta-frame))
  ;; call collision detection
  (let [rigid-bodies (ecs/components-of-type @world/!ecs-world c/RigidBody)
        collisions (cn/detect-collisions rigid-bodies)]
    (when debug? (println "Collisions:" collisions)))
  (when debug? (stats/end-stats)))

(defn- start-game-loop [scene, ^js ticker]
  (let [systems (all-systems scene)
        loop-fn (partial game-loop systems)]
    (.add ticker loop-fn)))


;; The Application data has to be stored globally so it can be destroyed on application stop
(def ^:private app-data nil)

(defn start
  "Application start entry point"
  []
  (set! app-data (create-app))
  (let [{app         :app
         game-ticker :game-ticker} app-data
        scene                      (px/container) ;; parent container of all game objects in the game
        preload-bundle             {:loading_panel "images/ui/loading_panel.png"}]
    (px/set-root-layout app (main-screen scene))
    (px/load-assets-bundle :preload preload-bundle load-title-screen)
    (start-game-loop scene game-ticker)))

(defn stop
  "Stops the application. 
   Used during the development, e.g. by hot-reload feature."
  []
  (kbd/clear-all)
  (world/clear-ecs-world)
  (destroy-app app-data)
  (set! app-data nil)
  (state/clear))
