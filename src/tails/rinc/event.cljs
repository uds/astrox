(ns tails.rinc.event
  "A simple event registration and dispatching inspired by re-frame."
  (:require [clojure.spec.alpha :as s]))


(s/def ::event-id keyword?)
(s/def ::event (s/spec (s/cat :event-id ::event-id, :args (s/* any?))))
(s/def ::atom #(satisfies? IAtom %))


;; The registry is a map of event-id to [!state handler-fn] vector.
(def ^:private !event-registry (atom {}))


(defn clear
  "Clears the event registry."
  []
  (reset! !event-registry {}))


(s/fdef reg-event :args (s/cat :event-id ::event-id, :state ::atom, :handler-fn fn?), :ret nil?)

(defn reg-event
  "Registers the given event handler for the given event ID.
   '!state' is an atom that holds the state to be update by 'handler-fn' as a result of dispatching of an event with 'event-id'.
   'handler-fn' is a function: (state event) -> state, where 'event' is a [event-id, args..] vector."
  [event-id !state handler-fn]
  (when (get @!event-registry event-id)
    (js/console.warn "Event" event-id "was already registered."))
  (swap! !event-registry assoc event-id [!state handler-fn])
  nil)


(defn- process-event
  "Returns updated state as a result of the event handler execution."
  [event]
  (let [event-id (first event)]
    (if-let [[!state handler-fn] (get @!event-registry event-id)]
      (swap! !state #(handler-fn % event))
      (throw (js/Error. (str "Unregistered event: " (if event-id event-id "nil")))))))


(s/fdef dispatch :args (s/cat :event ::event), :ret nil?)

(defn dispatch
  "Processes even and immediately updates the root state. 
   The first element of the event vector is 'event-id' followed by optional parameters, i.e. [event-id, args..]."
  [event]
  (process-event event)
  nil)
