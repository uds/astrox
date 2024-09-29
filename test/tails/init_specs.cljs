(ns ^:dev/always tails.init-specs
  (:require [orchestra-cljs.spec.test :as st]))

;; This file should be included as :require of all test files that want to enable specs instrumentation
(js/console.log "instrumented: " (st/instrument))
