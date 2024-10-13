(ns tails.rinc.subscription
  "Simple signal graph subscription model inspired by re-frame and ratom"
  (:require [tails.rinc.reaction :as r]))


;; Subscription registry and cache are global atoms. 
;; This lets us have a single source of truth for all subscriptions.
;; The !sub-registry is a map of 'query-id' to [signal-fn query-fn] vector.
;; The !sub-cache of the subscriptions is a nested map of 'query-id' to 'query-v' to 'subscription reactions'.
(def ^:private !sub-registry (atom {}))
(def ^:private !sub-cache (atom {}))


(defn clear-subscription-cache
  "Clear subscriptions cache. Every subscription reaction in the cache will be disposed."
  []
  (doseq [[_ subs] @!sub-cache
          [_ sub] subs]
    (r/dispose! sub))
  (reset! !sub-cache {}))

(defn clear-all
  "Clear all internal states, e.g. subscription cache and registry"
  []
  (clear-subscription-cache)
  (reset! !sub-registry {}))


(defn- get-cached-sub 
  "Retrieves subscription (reaction) from the cache"
  [query]
  (get-in @!sub-cache [(first query) query]))

(defn- cache-sub 
  "Write subscription (reaction) to the cache"
  [query sub]
  (swap! !sub-cache assoc-in [(first query) query] sub))

(defn- dispose-cached-subs 
  "Disposes all cached subscription (reaction) for the given query-id and removes them all from cache"
  [query-id]
  (when-let [subs (get @!sub-cache query-id)]
    (swap! !sub-cache dissoc query-id)
    (doseq [[_ sub] subs]
      (r/dispose! sub))))


(defn reg-sub
  "Register subscription function under the given key.
   The 'signal-fn' is a function '(query-v) -> signal(s)'
   On subscribe it will produce 'input-signals' that will be then passed to the 'query-fn'.
   The 'query-fn' is a function '(input-signals query-v) -> reaction', 
   where 'input-signals' is a list of signals (reactive atoms, reactions) produced by 'signal-fn' function,
   and 'query-v' is subscription query vector. 
   The 'query-fn' should return a subscription reactive atom."
  ([query-id signal-fn query-fn]
   (when (get @!sub-registry query-id)
     (dispose-cached-subs query-id)
     (js/console.warn "Subscription" query-id "was already registered."))
   (swap! !sub-registry assoc query-id [signal-fn query-fn])))

(defn- subscribe* [query]
  (let [query-id (first query)]
    (if-let [[signal-fn query-fn] (get @!sub-registry query-id)]
      (let [signals (signal-fn query)]
        (r/reaction (str query)
                    (query-fn signals query)))
      (throw (js/Error. (str "Unregistered subscription query: " query-id))))))

(defn subscribe
  "Subscribe to specified event. 
   Returns a 'reaction' (e.g. reactive atom with the function computing it's state) which is 'watching' a part of 
   the app-db described by registered subscription under query-id.
   The 'query' is a vector where first item is an ID of registered query and the rest are arguments of the event.
   Note that 'reaction' of the subscription is cached by the 'query-id' key and will be reused if subscribed again. 
   Also the 'reaction' is not automatically destroyed and stays active until the stop of the application."
  [query]
  (if-let [sub (get-cached-sub query)]
    sub
    (let [sub (subscribe* query)]
      (cache-sub query sub)
      sub)))

