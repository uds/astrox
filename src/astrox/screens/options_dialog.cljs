(ns astrox.screens.options-dialog
  (:require [tails.pixi.core :as px]
            [astrox.screens.widgets :as w]
            [astrox.screens.state :as state]))

(defn options-dialog []
  (let [body (px/text-layout "Options will be here." {:fill       0x5D5D5D
                                                      :dropShadow false
                                                      :position   "center"})
        actions (px/button "Apply" #(state/open-options-dialog false))
        on-close #(reset! state/!options-dialog-open? false)]
    (w/dialog-base "Options" body actions on-close)))

