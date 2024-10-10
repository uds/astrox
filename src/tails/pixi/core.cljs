(ns tails.pixi.core
  "Common Pixi functions"
  (:require [clojure.spec.alpha :as s]
            [goog.object :as obj]
            [pixi.js :as pixijs :refer (Application Container Assets Text TextStyle Sprite TilingSprite BlurFilter Ticker)]
            ["@pixi/layout" :refer (Layout)]
            ["@pixi/ui" :refer (FancyButton)]
            ["@pixi/filter-drop-shadow" :refer (DropShadowFilter)]
            ["./PixiPatches.js"]
            [tails.rinc.rtree :as rt]))


;; Main PIXI object definitions
(s/def ::application (partial instance? pixijs/Application))
(s/def ::asset (partial instance? pixijs/Asset))
(s/def ::assets object?)   ;; resources is js map of resource objects
(s/def ::container (partial instance? pixijs/Container))
(s/def ::object (partial instance? pixijs/DisplayObject))
(s/def ::sprite (partial instance? pixijs/Sprite))
(s/def ::texture (partial instance? pixijs/Texture))


;;----------------------------------------------------------------
;; Application


;; Some old pixi libs (like pixi-keyboard) need 'PIXI' global variable to be defined and pointing to the PIXI namespace.
#_{:clj-kondo/ignore [:unresolved-symbol]}
(set! (.-PIXI js/window) pixijs)


(declare ^:private resize-layout)

(defn resize-app
  "Resize PIXI application to the browser's window size."
  [app]
  (let [width js/window.innerWidth
        height js/window.innerHeight]
    (.. app -renderer (resize width height))
    (resize-layout (.-stage app))))

(defn create-app
  "Creates and initializes PIXI Application instance.
   The Application.ticker is created anew (not the same as sharedTicker) and started automatically."
  [app-html-id options]
  (let [default-opts {:viewId       app-html-id
                      :hello        true
                      :autoStart    true    ;; starts ticker automatically 
                      :sharedTicker false   ;; Application.ticker != Ticker.shared
                      :antialias    true
                      :resolution   js/window.devicePixelRatio
                      ;; automatically updates CSS style of Canvas on resize to match :resolution multiplier. 
                      :autoDensity  true}
        app (Application. (clj->js (merge default-opts options)))]
    ;; resize app once on start
    (resize-app app)
    app))

(defn- destroy-shared-ticker
  "Uses internal implementation knowledge of ticker to destroy current shared ticker instance.
   Needed for clean restart of the application in a dev mode - by destroying the  shared ticker 
   we ensure that all tick handlers were released."
  []
  ;; Note that here we are accessing private Ticker._shared and Ticker._protected variables. This may change in future PIXI releases.
  (when Ticker._shared
    ;; internal '_protected' property has to be set to false to be able to destroy a ticker
    (set! (.-_protected Ticker._shared) false)
    (.destroy Ticker._shared)
    (set! Ticker._shared nil)))

(defn destroy-app
  "Destroys the application"
  [app]
  ;; to prevent animations from updating destroyed objects
  (destroy-shared-ticker)

  ;; remove view canvas from DOM and destroy internal app structures
  (.destroy app true true)

  ;; clean up Assets static data
  (.reset Assets)

  ;; destroy PIXI global texture cache
  ;; NOTE that PIXI utils NPM module (e.g. @pixi/utils) should not be "required" directly:
  ;;      it contains a TextureCache global object which is going to be created second time if required directly 
  ;;      as it's already required by the "pixi.js" NPM module. We should access "utils" module via pixi's exported "utils" variable.
  ;;      (see https://stackoverflow.com/a/26703392)
  (pixijs/utils.destroyTextureCache))

(defn setup-cursors [^js app cursor-urls]
  (let [^js styles (.. app -renderer -events -cursorStyles)]
    (set! (.-default styles) (:default cursor-urls))
    (set! (.-pointer styles) (:pointer cursor-urls))))

(defn- get-stage
  "Returns a stage of the application (e.g. Application->stage)"
  [obj]
  (if-let [parent (.-parent obj)]
    (get-stage parent)
    obj))

;; PIXI has a constant for target frames-per-X in milliseconds, we make an FPS constant from it 
;; It is used to compute the delta-time value, which is in seconds.
(def ^:private target_fps (* Ticker.targetFPMS 1000))

(defn delta-frame->delta-time
  "Converts delta-frame into delta-time value.
   The value of delta-time is in frames, it is 1 when the game runs at target FPS (e.g. 60).
   Returned delta-time value is in seconds, it is 0.0166 when the game runs at 60 FPS.
   The delta-time in seconds is needed for movement and physics computations, as we use KMS measurement system."
  [delta-frame]
  (/ delta-frame target_fps))

(defn create-ticker []
  (doto (Ticker.)
    (.start)))

(defn pause-ticker
  "Pauses the tickers."
  [ticker pause?]
  (if pause? (.stop ticker) (.start ticker)))


;;----------------------------------------------------------------
;; Utils


(defn container
  "Creates a new PIXI container"
  []
  (Container.))

(defn set-pos
  ([obj {x :x y :y}]
   (.. obj -position (set x y))
   obj)
  ([obj x y]
   (.. obj -position (set x y))
   obj))


;;----------------------------------------------------------------
;; Assets


(defn- log-loading-progress [percentage]
  (js/console.log (str "Loading...  " (* 100 percentage) "%")))

(defn load-assets-bundle
  "Loads assets bundle. Calls 'on-loaded' callback with resolved assets as a parameter."
  [bundle-key bundle on-loaded]
  (.addBundle Assets (name bundle-key) (clj->js bundle))
  (-> (.loadBundle Assets (name bundle-key) log-loading-progress)
      (.then on-loaded)                ;; (on-loaded assets)
      (.catch js/console.error)))

(defn asset-bundle-loaded?
  "Returns true if the bundle is already in the Assets cache"
  [bundle-key]
  (.. Assets -resolver (hasBundle (name bundle-key))))

(defn asset
  "Gets the pre-loaded asset by it's id."
  [asset-id]
  (.. Assets -cache (get (name asset-id))))

(defn spritesheet-texture
  "Get a sprite texture from the pre-loaded sprite sheet."
  [^js spritesheet texture-id]
  ;; using google library to get faster access to the object property instead of using the cls->js 
  (obj/get (.-textures spritesheet) (name texture-id)))


;;----------------------------------------------------------------
;; Layout


(defn layout
  "Creates root layout with the given options"
  [options]
  (Layout. (clj->js options)))

(defn- resize-layout
  "Resize layout components that are attached to the app stage."
  [obj]
  (let [layouts (filter #(instance? Layout %) (.-children obj))
        width js/window.innerWidth
        height js/window.innerHeight]
    (when (instance? Layout obj)
      (.resize obj width height))
    (doseq [layout layouts]
      ;; Note that the width and height of the layout should be defined in 'logical' CSS pixels,
      ;; as they can be different from the screen resolution - e.g. if js/window.devicePixelRatio is bigger then 1.
      (.resize layout width height))))

(defn set-root-layout
  "Adds layout to the application stage and resizes it to fit the window"
  [^js app layout]
  (let [stage (.-stage app)]
    (.addChild stage layout)
    ;; need to resize root layout on creation
    (resize-layout stage)))

(defn- get-root-layout [stage]
  (->> (.-children stage)
       (filter (partial instance? Layout))
       (first)))

(defn new-layout-id
  "Every layout require an unique id in order for it to be correctly used in the layout hierarchy. 
   Returns randomly generated layout id."
  [prefix]
  ;; copied from ContentController.js (PIXI Layout lib)
  (str (name prefix) "-" (.toString (.now js/Date) 36)
       (-> (js/Math.random)
           (.toString 36)
           (.substr 2))))


;;----------------------------------------------------------------
;; Graphics elements

(defn draw-frame
  "Draws a rectangular frame"
  [^js x y width height color]
  (let [graphics (pixijs/Graphics.)]
    (doto graphics
      (. lineStyle 1, color)
      (.drawRect x y width height))
    graphics))

(defn draw-hollow-circle
  "Draws a hollow circle"
  [^js x y radius color]
  (let [graphics (pixijs/Graphics.)]
    (doto graphics
      (. lineStyle 1, color)
      (.drawCircle x y radius))
    graphics))


;;----------------------------------------------------------------
;; Effects


(defn- add-filter
  "Adds PIXI filter to the display object"
  [obj filter]
  (if-let [filters (.-filters obj)]
    (aset filters (.-length filters) filter)
    (set! (.-filters obj) #js [filter]))
  filter)

(defn- remove-filter
  "Removes given filter from the PIXI object"
  [obj filter]
  (when-let [filters (.-filters obj)]
    (let [idx (.indexOf filters filter)]
      (when (<= 0 idx)
        (.splice filters idx 1)))))

(defn- has-filter?
  "Returns true if the display object has a filter that matches given predicate."
  [obj pred]
  (when-let [filters (js->clj (.-filters obj))]
    (some pred filters)))

(def ^:private blur-filter? (partial instance? BlurFilter))

(defn add-drop-shadow-filter
  "Adds drop shadow filter to the display object. Returns input object."
  [obj options]
  (let [filter (DropShadowFilter. (clj->js options))]
    (add-filter obj filter)
    obj))


;;----------------------------------------------------------------
;; Widgets

(defn destroy-cascade
  "Destroy given PIXI object, including all it's children but keeping shared textures."
  [^js obj]
  (.destroy obj #js {:children    true
                     :texture     false
                     :baseTexture false}))

(defn sprite
  "Creates sprite from the asset.
   The asset can be either a name (e.g. image name) or a keyword."
  [asset]
  (->> (if (keyword? asset) (name asset) asset)
       (.from Sprite)))

(defn tiling-sprite-from
  "Creates sprite from the asset."
  [asset width height]
  (let [asset' (if (keyword? asset) (name asset) asset)]
    (.from TilingSprite asset' #js {:width  width
                                    :height height})))

(def default-text-style {:fontFamily         "kenvector-future"
                         :fontSize           24
                         :fill               "white"
                         :dropShadow         true
                         :dropShadowDistance 2
                         :dropShadowBlur     2
                         :dropShadowAlpha    0.5})

(defn text-style
  "Creates a new text style instance with default color and font."
  [style]
  (->> (merge default-text-style style)
       (clj->js)
       (TextStyle.)))

(defn text [txt style]
  (Text. txt (text-style style)))

(defn text-layout
  "Creates a text by defining a layout. To be used as part of a layout configuration.
   Note that if pixijs/Text is used directly in the Pixi Layout config, it's style will be overwritten."
  [text styles]
  (layout {:id      (new-layout-id :text)
           :content text
           :styles  (merge default-text-style styles)}))

(defn base-button
  "Creates base button."
  [styles on-click]
  (let [default-styles {:anchor     0.5
                        :animations {:hover   {:props    {:scale {:x 1.03
                                                                  :y 1.03}
                                                          :y     0}
                                               :duration 100}
                                     :pressed {:props    {:scale {:x 0.99
                                                                  :y 0.99}
                                                          :y     2}
                                               :duration 100}}}
        btn            (FancyButton. (clj->js (merge default-styles styles)))
        btn-width      (.-width btn)
        btn-height     (.-height btn)]
    (.. btn -onPress (connect on-click))
    (layout {:id      (new-layout-id :button)
             :content btn
             :styles  {:width       btn-width
                       :height      btn-height
                       :paddingTop  (/ btn-height 2)
                       :paddingLeft (/ btn-width 2)}})))

(defn button
  "Creates button with the given text."
  [txt on-click]
  (base-button {:defaultView "yellow_button.png"
                :hoverView   "yellow_button_hover.png"
                :pressedView "yellow_button_active.png"
                :text        (text txt {:fontSize 24
                                        :fill     "white"})
                :textOffset  {:y -3}}
               on-click))


;;----------------------------------------------------------------
;; Modal


(defn- screen-overlay
  "Non-interactive full screen overlay that is used as a dialog backdrop."
  [content]
  (let [overlay (layout {:id      (new-layout-id :overlay)
                         :content content
                         :styles  {:background "rgba(0, 0, 0, 0.5)"
                                   :position   "center"
                                   :width      "100%"
                                   :height     "100%"}})]
    ;; seems like a bug (in 7.2): "static" is the only mode  that does not pass through events to the underlying "parent" object.
    (set! (.-eventMode overlay) "static")
    overlay))

(defn as-modal
  "Renders the content as a modal dialog, with overlay blocking the backdrop.
   Returns a rtree component node."
  [content]
  (let [overlay     (screen-overlay content)
        blur-filter (BlurFilter. 4)]
    ;; The Component instance with lifecycle methods is returned.
    ;; Attaches overlay (and it's content) directly to the root layout container, while applying blur effect to the 
    ;; would-to-be parent of the overlay. This way the overlay and content will not be blurred, but only the parent will.
    (reify rt/Component
      (did-mount [_this parent]
        (let [stage       ^js (get-stage parent)
              root-layout (get-root-layout stage)]
          (when-not (has-filter? root-layout blur-filter?)
            (add-filter root-layout blur-filter)) ;; blur old root layout
          (.addChild stage overlay) ;; add overlay (with content) as a second root layout
          (resize-layout overlay)))

      (will-unmount [_this parent]
        (let [stage ^js (get-stage parent)]
          (remove-filter (get-root-layout stage) blur-filter)
          (.destroy blur-filter)
          (.removeChild stage overlay)
          (destroy-cascade overlay)))

      ;; returning "nil" so the node will not be attached to the tree but will be managed via mounting and unmounting events.
      (scene-node [_this] nil))))
