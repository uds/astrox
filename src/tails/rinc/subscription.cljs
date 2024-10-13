(ns tails.rinc.subscription
  "A simple signal graph subscription inspired by re-frame and ratom."
  (:require [tails.rinc.reaction :as r]
            [clojure.spec.alpha :as s]))


(s/def ::query-id keyword?)
(s/def ::query (s/spec (s/cat :query-id ::query-id, :args (s/* any?))))
(s/def ::reaction r/reaction?)


;; The subscription registry and cache are global atoms, providing a single source of truth for all subscriptions.
;; !sub-registry maps 'query-id' to a vector [signal-fn query-fn].
;; !sub-cache is a nested map of 'query-id' to 'query-v' to 'subscription reactions'.
(def ^:private !sub-registry (atom {}))
(def ^:private !sub-cache (atom {}))


(defn clear-subscription-cache
  "Clears the subscription cache, disposing of every subscription reaction in the cache."
  []
  (doseq [[_ subs] @!sub-cache
          [_ sub] subs]
    (r/dispose! sub))
  (reset! !sub-cache {}))

(defn clear
  "Clears all internal states, including the subscription cache and registry."
  []
  (clear-subscription-cache)
  (reset! !sub-registry {}))


(defn- get-cached-sub 
  "Retrieves a subscription (reaction) from the cache."
  [query]
  (get-in @!sub-cache [(first query) query]))

(defn- cache-sub 
  "Writes a subscription (reaction) to the cache."
  [query sub]
  (swap! !sub-cache assoc-in [(first query) query] sub))

(defn- dispose-cached-subs 
  "Disposes of all cached subscriptions (reactions) for the given query-id and removes them from the cache."
  [query-id]
  (when-let [subs (get @!sub-cache query-id)]
    (swap! !sub-cache dissoc query-id)
    (doseq [[_ sub] subs]
      (r/dispose! sub))))


(s/fdef reg-sub :args (s/cat :query-id ::query-id, :signal-fn fn?, :query-fn fn?) :ret any?)

(defn reg-sub
  "Registers a subscription function under the given key.
   'signal-fn' is a function '(query-v) -> signal(s)' that produces 'input-signals' when subscribed.
   'query-fn' is a function '(input-signals query-v) -> reaction', where 'input-signals' is a list of signals 
   (e.g. list of reactive atoms or reactions) produced by 'signal-fn', and 'query-v' is the subscription query vector.
   'query-fn' should return a subscription reactive atom."
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


(s/fdef subscribe :args (s/cat :query ::query) :ret ::reaction)

(defn subscribe
  "Subscribes to a specified event.
   Returns a 'reaction' (e.g., a reactive atom with a function computing its state) that observes a part of 
   the map stored in the ratom, described by the registered subscription under query-id.
   'query' is a vector where the first item is the ID of a registered query (query-id) and the rest are event arguments.
   The subscription's 'reaction' is cached by the 'query-id' key and will be reused if subscribed again.
   Note that the subscription's 'reaction' is not automatically destroyed and remains active until the application stops 
   or subscription cache is not cleared."
  [query]
  (if-let [sub (get-cached-sub query)]
    sub
    (let [sub (subscribe* query)]
      (cache-sub query sub)
      sub)))
