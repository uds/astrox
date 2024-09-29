(ns tails.rinc.core
  "Simple reactive programming model inspired by re-frame and ratom"
  (:require [tails.rinc.reaction :as r]))


;; Global application state defined as a reactive atom. DO NOT USE it directly in the application code.
(def !app-db (r/ratom :app-db {}))

;; The registry is a map of 'query-id' to [signal-fn query-fn] vector.
(def ^:private !sub-registry (atom {}))

;; Cache of the subscriptions as nested map of 'query-id' to 'query-v' to 'subscription reactions'
(def ^:private !sub-cache (atom {}))

;; The registry is a map of 'event-id' to 'handler-fn'.
(def ^:private !event-registry (atom {}))

;; Event FIFO queue.
(def ^:private !event-queue (atom #queue []))


(defn clear-app-db []
  (reset! !app-db {}))

(defn clear-subscription-cache
  "Clear subscriptions cache. Every subscription reaction in the cache will be disposed."
  []
  (doseq [[_ subs] @!sub-cache
          [_ sub] subs]
    (r/dispose! sub))
  (reset! !sub-cache {}))

(defn clear-event-queue []
  (reset! !event-queue #queue []))

(defn clear-all
  "Clear all internal states, including app-db, event and subscription registries, event-queue and views."
  []
  (clear-subscription-cache)

  ;; clear db before clearing internal registries (e.g. subscriptions) so references to registry while clearing things will still work.
  (clear-app-db)

  (clear-event-queue)
  (reset! !event-registry {})
  (reset! !sub-registry {}))


;; --------------------------------------------------------------------------------
;;  Subscriptions 

;; Retrieves subscription (reaction) from the cache
(defn- get-cached-sub [query]
  (get-in @!sub-cache [(first query) query]))

;; Write subscription (reaction) to the cache
(defn- cache-sub [query sub]
  (swap! !sub-cache assoc-in [(first query) query] sub))

;; Disposes all cached subscription (reaction) for the given query-id and removes them all from cache
(defn- dispose-cached-subs [query-id]
  (when-let [subs (get @!sub-cache query-id)]
    (swap! !sub-cache dissoc query-id)
    (doseq [[_ sub] subs]
      (r/dispose! sub))))


(defn app-db-signal
  "Standard subscription signal that returns app-db ratom"
  [_]
  !app-db)

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
   (swap! !sub-registry assoc query-id [signal-fn query-fn]))
  ([query-id query-fn]
   (reg-sub query-id
            app-db-signal
            query-fn)))

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


;; --------------------------------------------------------------------------------
;;  Events


(defn reg-event
  "Registers the given event handler for the given event ID.
   The 'handler-fn' is a function: (db event) -> db, where 'event' is a vector as [event-id, args..]"
  [event-id handler-fn]
  (when (get @!event-registry event-id)
    (js/console.warn "Event" event-id "was already registered."))
  (swap! !event-registry assoc event-id handler-fn))

(defn dispatch
  "Queue 'event' vector for processing.
   The first element of the event vector is 'event-id' followed by optional parameters, i.e. [event-id, args..].
   The even queue is processed in FIFO order on every step of the game loop."
  [event]
  (when-not event
    (throw (js/Error. "dispatch argument is nil")))
  (swap! !event-queue conj event)
  nil)

(declare ^:private process-events)

(defn dispatch-sync
  "Processes even and immediately updates app database with new state. 
   Should be only used to execute the very first (initialization) event in the app.
   The first element of the event vector is 'event-id' followed by optional parameters, i.e. [event-id, args..].
   The even queue is processed in FIFO order on every step of the game loop."
  [event]
  (when-not event
    (throw (js/Error. "dispatch argument is nil")))
  (process-events [event] nil)
  nil)


(defn- process-event
  "Returns updated db as a result of the event handler execution."
  [db event]
  (let [event-id (first event)]
    (if-let [handler-fn (get @!event-registry event-id)]
      (let [db (handler-fn db event)]
        db)
      (throw (js/Error. (str "Unregistered event: " event-id))))))

(defn- process-events
  "Process collection of events and updates app db with the new state. Returns new state or nil"
  [events validate-db-fn]
  (let [db @!app-db
        new-db (reduce process-event db events)]
    (when-not (identical? db new-db)  ;; just little optimization for the case when event processing does not change db at all
      (when validate-db-fn
        (validate-db-fn new-db events))
      (reset! !app-db new-db))))

;; Defines how many times the queue will be processed during a single run.
;; Processing events may generate another events that are placed into the queue. 
;; WARNING: 
;; Too many repeats during the single run may consume all time alloted for the tick. 
;; It's also possible to get an infinite loop if new events are created during each run.
(def ^:private max-event-queue-run-repeats 3)

(defn run-event-queue
  "Process all the events in the queue. 
   Will repeat while the queue is not empty - new events might have been added into queue during the processing.
   Uses 'validate-db-fn' function (if provided) to validate the new db state. 
   The 'validate-db-fn' is defined as '(db events) -> nil' and it will throw an exception in case of invalid db state.
   WARNING: 
   Do not try to modify app database outside of the event handlers! 
   Capturing db state outside of the event processing loop will lead to stale db data and strange db overrides.
   WARNING: 
   Changes to the app-db are applied only after processing of all _current_ events in the queue. 
   Also the reactive signal graph will by updated only after the app-db is updated with new db value. 
   Thus any changes to subscription will only be seen in the next queue processing run."
  ([]
   (run-event-queue nil))
  ([validate-db-fn]
   (loop [events @!event-queue, count 0]
     (when (>= count max-event-queue-run-repeats)
       (js/console.warn (str "Too many repeats (" count ") during a singe event queue processing run.")))
     (when (seq events)
       (reset! !event-queue #queue [])
       (process-events events validate-db-fn)
       ;; repeat while the queue is not empty - to consume all events that might have been generated while processing the events
       (recur @!event-queue (inc count))))))
