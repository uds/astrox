(ns tails.debug)

;; The goog.DEBUG variable is automatically set by the shadow-cljs. It's false for release builds and true for dev builds.
;; The type hint for debug? variable is required in order to allow for DCE to work (see https://clojureverse.org/t/how-to-deal-with-development-code-in-clojurescript/613/2)
(def debug? ^boolean goog.DEBUG)
;;(def debug? false)
