(ns tails.rinc.reaction
  "Reactive tools based on Reflex' ComputedObservable and Reagent' ratom and reaction."
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s])
  (:require-macros [tails.rinc.reaction]))


;; A set of ratoms or reactions that are captured by capture-derefed function.
(declare ^:dynamic *captured-derefed*)


(s/def ::derefed #(satisfies? IDeref %))
(s/def ::derefed-set (s/coll-of ::derefed :kind set?))
(s/fdef capture-derefed :args (s/cat :f fn?, :state any?), :ret (s/tuple any? ::derefed-set))

(defn- capture-derefed
  "Executes function `f` and records occurrences of dereferences on any RAtom or Reaction on execution path.
   May not capture all deref-ed objects, due to conditional execution, e.g. (if x? (... @a) (... @b)).
   Return tuple containing result of `f` execution and collection of all deref-ed objects."
  [f state]
  (binding [*captured-derefed* (atom #{})]
    (let [result (f state)]
      (when (seq? result) (doall result))   ;; materialize lazy sequence in order to ensure that their elements are evaluated in the *captured-derefed* context.
      [result @*captured-derefed*])))


(s/fdef notify-deref! :args (s/cat :derefed ::derefed), :ret any?)

(defn- notify-deref!
  "Adds `derefed` to a `*captured-derefed*` list. 
   Usually called from `-deref` method of IDeref protocol."
  [derefed]
  (when *captured-derefed*
    (swap! *captured-derefed* #(conj % derefed))))


(s/def ::watchable #(satisfies? IWatchable %))
(s/fdef notify-watches* :args (s/cat :ref ::watchable, :old-val any?, :new-val any?), :ret any?)

(defn- notify-watches*
  "The watch callback function has 4 arguments, as defined in Clojure docs, e.g. (key, ref, old-val, new-val) -> any?"
  [^clj ref, old-val, new-val]
  (doseq [[k f] (.-watches ref)]
    (f k ref old-val new-val)))


(s/fdef add-watch* :args (s/cat :ref ::watchable, :key some?, :f fn?), :ret any?)

(defn- add-watch* [^clj ref, key, f]
  (set! (.-watches ref)
        (assoc (.-watches ref) key f)))


(s/fdef remove-watch* :args (s/cat :iref ::watchable, :key some?), :ret any?)

(defn- remove-watch* [^clj ref, key]
  (set! (.-watches ref)
        (dissoc (.-watches ref) key)))


;;--------------------------------------------------------------------------------
;; RAtom


(defprotocol IRAtom
  (deref-raw [this]
    "Return value of the reference, bypassing capturing of the dereferences. 
     The reference accessed via this function inside the reaction's function will not be watched by it."))


;; An implementation (partial) of the atom that cam be used as observable source by the Reaction.
(deftype ^:private RAtom [id, ^:mutable state, ^:mutable watches]
  IAtom

  IRAtom
  (deref-raw [_this] state)

  IEquiv
  (-equiv [this other]
    (identical? this other))

  IDeref
  (-deref [this]
    (notify-deref! this)
    state)

  IReset
  (-reset! [this new-value]
    (let [old-value state]
      (set! state new-value)
      (when (seq watches)
        (notify-watches* this old-value new-value))
      new-value))

  ISwap
  (-swap! [this f]          (-reset! this (f state)))
  (-swap! [this f x]        (-reset! this (f state x)))
  (-swap! [this f x y]      (-reset! this (f state x y)))
  (-swap! [this f x y more] (-reset! this (apply f state x y more)))

  IWatchable
  (-notify-watches [this old new]  (notify-watches* this old new))
  (-add-watch [this key f]         (add-watch* this key f))
  (-remove-watch [this key]        (remove-watch* this key))

  IHash
  (-hash [this] (goog/getUid this)))


(defn ratom
  "Create atom-like object that is 'reactive' - e.g. keeps track of deref's"
  ([state]
   (->RAtom nil state nil))
  ([id state]
   (->RAtom id state nil)))


;;--------------------------------------------------------------------------------
;; Reaction


;; Protocol for disposable objects
(defprotocol IDisposable
  (dispose! [this]))


;; Definition of observable reaction object. It's similar to atom in that it can be dereferenced and watched.
;; The 'function-fn' should be a function of a single argument, it will be called with a current state of the reaction on every change.
;; It is not used directly but created and returned by the 'reaction' macro.
(deftype ^:private Reaction [id, reaction-fn, ^:mutable state, ^:mutable dirty?, ^:mutable observables, ^:mutable watches]
  IDisposable
  (dispose! [this]
    (doseq [o observables]
      (remove-watch o this))
    (set! observables nil)
    (set! watches nil)
    (set! dirty? false))

  IEquiv
  (-equiv [this other]
    (identical? this other))

  IDeref
  (-deref [this]
    (notify-deref! this)
    (when dirty?
      (._compute-state! this))
    state)

  IWatchable
  (-notify-watches [this old new]
    (notify-watches* this old new))

  (-add-watch [this key f]
    (add-watch* this key f))

  (-remove-watch [this key]
    (when (seq watches)
      (remove-watch* this key)))

  IHash
  (-hash [this] (goog/getUid this))

  Object
  ;; Computes reaction state by calling reaction function. Returns updated state.
  (_compute-state! [this]
    (let [[result derefed] (capture-derefed reaction-fn state)]
      (._update-observables! this derefed)
      (set! dirty? false)
      (set! state result)
      result))

  ;; Computes reaction state and notify watches if state has changed.
  (_compute-and-notify-watches! [this]
    (let [old-state state
          new-state (._compute-state! this)]
      (when (and (seq watches)
                 (not= old-state new-state))
        (notify-watches* this old-state new-state))
      new-state))

  ;; Updates list of observables on the reaction object. Refresh watches when needed.
  (_update-observables! [this, derefed]
    (let [old-observables (set observables)]
      (set! observables derefed)
      ;; Update watches on observable atoms. This is needed because `reaction-fn` can deref different atoms on each call.
      ;; E.g. if function will have multiple execution paths like (if x? (... @a) (... @b))
      (when-not (= old-observables derefed)
        ;; Both observables and derefed are sets, so we can use set operations
        (doseq [observable (set/difference derefed old-observables)]
          (add-watch observable this #(._handle-change! ^clj %1 %3 %4)))  ;; add-watch handler has 4 args: this, _source, old, new
        (doseq [observable (set/difference old-observables derefed)]
          (remove-watch observable this)))))

  ;; Watch handler that is setup on observable and called on observable change.
  (_handle-change! [this, old-val, new-val]
    (when-not (or (= old-val new-val) dirty?)     ;; notify watches only if state has been changed
      (._compute-and-notify-watches! this))))

(defn make-reaction
  "Creates reaction with a given function. 
   The function will be called with a single argument that is a current state of the reaction."
  ([reaction-fn]
   (make-reaction nil reaction-fn))
  ([id reaction-fn]
   ;; set reaction to dirty so it will initialize watchers on first deref
   (->Reaction id reaction-fn nil true nil nil)))

(defn reaction?
  "Returns true if the input argument is a reaction"
  [r]
  (instance? Reaction r))


(defn walk-deps
  "Walks 'r'-s dependencies and calls function 'f' for each element.
   Function 'f' accepts two arguments - parent 'p' and current child 'r'.
   Returns tree representation of the dependencies"
  [f p r]
  (let [res (f p r)
        watches (keys (.-watches ^clj r))]
    (if (empty? watches)
      res
      (conj [res] (mapv #(walk-deps f r %) watches)))))

