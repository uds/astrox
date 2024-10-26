(ns tails.ecs.core
  "Simple Entity Component System (ECS) implementation."
  (:require [clojure.spec.alpha :as s]))


;;
;; ECS world is described by following maps (inspired by https://github.com/markmandel/brute):
;; 
;; 'components' map is { ComponentType -> { EntityID -> ComponentInstance } }
;; 'component-types' map is { EntityID -> #{ ComponentType } }
;; 'updated-components' map is {ComponentType -> {EntityID -> ComponentInstance}}
;; 'removed-components' map is {ComponentType -> {EntityID -> ComponentInstance}}
;;
;; 'components' map is used to find instance of the given ComponentType for the given EntityID
;; 'component-types' map is a definition of the entity, it lists all component types entity contains.
;; 'updated-components' map holds references to components that has been updated in this frame. It has to be cleared at the end of each frame.
;; 'removed-components' map holds references to components that has been removed in this frame. It has to be cleared at the end of each frame.
;; 
;; Entity can be created and destroyed at any time. Entity is just an unique id (UUID).
;; Destroying entity will remove entity record from 'component-types' map and 
;; remove all component instances of the entity from the 'components' and 'updated-components' map.
;; 
;; Component is represented by a record that is created via defrecord operation.
;; Component can be added to entity or removed from it at any time.
;; 

(s/def ::entity-id uuid?)

(s/def ::component-type any?)       ;; component type can be anything, e.g. (type C1) or keyword, etc
(s/def ::component-instance map?)   ;; component is a map or defrecord

(s/def ::components (s/map-of ::component-type (s/map-of ::entity-id ::component-instance)))
(s/def ::updated-components ::components)
(s/def ::removed-components ::components)
(s/def ::component-types (s/map-of ::entity-id (s/coll-of ::component-type :kind set?)))
(s/def ::world (s/keys :opt-un [::components ::updated-components ::removed-components ::component-types]))

;; An "external" definition of the entity as [EntityID, [ComponentInstance]]
;; Such entity representation is useful to send entity around before it gets committed into the ECS world.
(s/def ::ext-entity (s/tuple ::entity-id, (s/coll-of ::component-instance)))


;;----------------------------------------------------------------
;; Entity construction


(s/fdef create-entity :ret uuid?)

(defn create-entity
  "Returns new entity ID. Entity in ECS is represented by its unique ID."
  []
  (random-uuid))


(defn component-type
  "Returns unique type value of the given component instance."
  [component]
  (type component))


(s/fdef add-component 
  :args (s/cat :world ::world, :entity-id ::entity-id, :component ::component-instance) 
  :ret ::world)

(defn add-component
  "Adds component to the entity. Returns updated version of the world."
  [world entity-id component]
  (let [comp-type (component-type component)]
    (-> world
        (assoc-in [:components comp-type entity-id] component)
        (assoc-in [:updated-components comp-type entity-id] component)    ;; added components will appear in the "updated"" list
        (update-in [:component-types entity-id] conj comp-type))))


;; copied from https://stackoverflow.com/a/14488425
(defn- dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as _keys]]
  (if ks
    (if-let [next-map (get m k)]
      (let [new-map (dissoc-in next-map ks)]
        (if (seq new-map)
          (assoc m k new-map)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn- remove-component-type [world entity-id component-type]
  (if-let [types (get-in world [:component-types entity-id])]
    (if-let [new-types (not-empty (disj types component-type))]
      (assoc-in world [:component-types entity-id] new-types)
      (dissoc-in world [:component-types entity-id]))
    world))


(defn- assoc-removed-component
  "Update :removed-components entry with about to be removed component"
  [world entity-id component-type]
  (if-let [component (get-in world [:components component-type entity-id])]
    (assoc-in world [:removed-components component-type entity-id] component)
    world))


(s/fdef remove-component 
  :args (s/cat :world ::world, :entity-id ::entity-id, :component-type ::component-type)
  :ret ::world)

(defn remove-component
  "Removes component of the given type from the entity. Returns updated version of the world."
  [world entity-id component-type]
  (-> world
      (assoc-removed-component entity-id component-type)
      (dissoc-in [:components component-type entity-id])
      (dissoc-in [:updated-components component-type entity-id])          ;; removed component is also removed from the "updated" list
      (remove-component-type entity-id component-type)))


(s/fdef add-entity
  :args (s/alt :args1 (s/cat :world ::world, :entity-id ::entity-id, :components (s/nilable (s/coll-of ::component-instance)))
               :args1 (s/cat :world ::world, :components (s/tuple ::entity-id (s/nilable (s/coll-of ::component-instance)))))
  :ret ::world)

(defn add-entity
  "Adds an entity to the ECS world. Provided component instances are attached to the entity.
   Returns updated version of the world."
  ([world entity-id components]
   (let [world (assoc-in world [:component-types entity-id] #{})]
     (reduce #(add-component %1 entity-id %2) world components)))
  ([world [entity-id components]]
   (add-entity world entity-id components)))


(s/fdef remove-entity
  :args (s/cat :world ::world, :entity-id ::entity-id)
  :ret ::world)

(defn remove-entity
  "Removes entity and all of its components from the ECS world.
   Returns updated version of the world."
  [world entity-id]
  (let [comp-types (get-in world [:component-types entity-id])]
    ;; the :component-types key will be removed as well by the remove-component function once all components are removed
    (reduce #(remove-component %1 entity-id %2) world comp-types)))


;;----------------------------------------------------------------
;; Updates 

(defn- assoc-component
  "Associate component with the given entity.
   Returns updated version of the world."
  [world entity-id component]
  (let [comp-type (component-type component)]
    (-> world
        (assoc-in [:components comp-type entity-id] component)
        (assoc-in [:updated-components comp-type entity-id] component))))


(s/fdef update-component
  :args (s/cat :world ::world, :entity-id ::entity-id, :component ::component-instance, :update-fn fn?, :args (s/* any?))
  :ret ::world)

(defn update-component
  "Update component by calling update-fn on it.
   Returns updated version of the world."
  [world entity-id component update-fn & args]
  (let [upd-component (apply update-fn component args)]
    ;; modify :updated-component only if the component has been changed.
    ;; this way, only changed components will be processed by the system' tick function if they will use reactive subscriptions based on :updated-component as selector.
    (if (= component upd-component)
      world
      (assoc-component world entity-id upd-component))))


(s/fdef clear-updated-components :args (s/cat :world ::world), :ret ::world)

(defn clear-updated-components
  "Clears :updated-components and :removed-components keys in the the ECS world.
   Should be called et the end of the frame to ensure that next frame will capture updates correctly.
   Returns updated version of the world."
  [world]
  (dissoc world :updated-components :removed-components))


;;----------------------------------------------------------------
;; Entity and component queries


(s/fdef component
  :args (s/cat :world ::world, :entity-id ::entity-id, :component-type ::component-type)
  :ret (s/nilable ::component-instance))

(defn component
  "Returns a component instance of the given entity by its type or nil."
  [world entity-id component-type]
  (get-in world [:components component-type entity-id]))


(s/fdef components-of-type
  :args (s/cat :world ::world, :component-type ::component-type)
  :ret (s/nilable (s/coll-of ::component-instance)))

(defn components-of-type
  "Returns a collection of all components of the given type. 
   May return nil if no components of the given type are found."
  [world component-type]
  (vals (get-in world [:components component-type])))


(s/fdef entity-components
  :args (s/cat :world ::world, :entity-id ::entity-id),
  :ret (s/coll-of ::component-instance))

(defn entity-components
  "Returns all components of the given entity."
  [world entity-id]
  (let [types (get-in world [:component-types entity-id])]
    (map (fn [t] (get-in world [:components t entity-id])) types)))


(s/fdef select-components
  :args (s/cat :world ::world, :entity-id ::entity-id, :component-types (s/coll-of ::component-type))
  :ret (s/coll-of (s/nilable ::component-instance)))

(defn select-components
  "Returns a collection of component instances of the given entity selected by specified types."
  [world entity-id component-types]
  (map #(component world entity-id %) component-types))


(s/fdef entities-with-component
  :args (s/cat :world ::world, :component-type ::component-type),
  :ret (s/nilable (s/coll-of ::entity-id)))

(defn entities-with-component
  "Returns a collection of entity-id's for all entities that have a given component type. 
   May return nil if no entities with the given component type are found."
  [world component-type]
  (keys (get-in world [:components component-type])))


;;----------------------------------------------------------------
;; Systems execution


(defn- update-component-by-system
  "Applies each 'tick' system-fn from the input sequence to the component and returns updated component.
   Returns updated component."
  [component system-fn-seq & args]
  (reduce (fn [comp system-fn]
            (apply system-fn comp args))
          component system-fn-seq))

(defn- update-components-by-system
  "Update components by calling system-fn-seq on all components of the input map { EntityId -> ComponentInstance }.
   The system-fn is a function with [component eid & args] arguments.
   Returns only changed components as a map { EntityId -> ComponentInstance }."
  [components-map system-fn-seq & args]
  (reduce-kv (fn [result eid component]
               (let [upd-component (apply update-component-by-system component system-fn-seq eid args)]
                 ;; adds only changed components to the result list
                 (if (= component upd-component)
                   result
                   (assoc result eid upd-component))))
             {} components-map))

(defn- merge-updated-components
  "Merges a map of updated components into the world. The component map is { EntityId -> ComponentInstance }.
   Returns updated world."
  [world component-type components-map]
  (if-let [components-map (not-empty components-map)]
    (-> world
        (update-in [:components component-type] merge components-map)
        (update-in [:updated-components component-type] merge components-map))
    world))

(defn- update-components-in-world
  "Update components by calling system-fn-seq on all components of the input map { EntityId -> ComponentInstance }.
   The system-fn is a function with [component eid & args] arguments.
   Returns updated version of the world."
  [world component-type components-map system-fn-seq & args]
  (->> (apply update-components-by-system components-map system-fn-seq args)
       (merge-updated-components world component-type)))


(s/fdef systems-tick
  :args (s/cat :world ::world, :systems (s/map-of ::component-type (s/coll-of fn?)), :delta-time number?, :delta-frame number?)
  :ret ::world)

(defn systems-tick
  "Executes given systems (functions) for updated components and returns an updated world.
   The 'systems' argument is a map of a list of system tick functions keyed by the component type:
   { ComponentType -> [ (fn system-fn [component eid world delta delta-frame] ...) ] }
   'system-fn' returns an (possibly updated) component instance.
   Whenever a component of the given type has changed since the last frame, associated with that component system tick functions
   are called. The system tick function returns an updated component instance.
   Returns updated version of the world."
  [world systems delta-time delta-frame]
  (if-let [upd-components (not-empty (:updated-components world))]
    ;; clear a list of all updated and removed components
    (let [world (clear-updated-components world)]
      ;; iterate over a map of updated components { ComponentType -> { EntityId -> ComponentInstance } }
      (reduce-kv (fn [world comp-type components]
                   (let [system-fn-seq (get systems comp-type)]
                     (update-components-in-world world comp-type components system-fn-seq world delta-time delta-frame)))
                 world upd-components))
    world))


(defn systems-by-component-tick
  "Executes given systems for _all_ components of the type associated with each system and returns an updated world.
   One of the usages is to scan all rigid body components and compute collisions between them.
   The 'systems' argument is a map of system tick functions keyed by the component type:
   { ComponentType -> (fn system-fn [components-map world delta delta-frame] ...) },
   where 'components-map' is a map of all components by entity ID: { EntityId -> ComponentInstance }.
   'system-fn' returns only changed components as a map { EntityId -> ComponentInstance }.
   Returns updated version of the world."
  [world systems delta-time delta-frame]
  (reduce-kv (fn [world comp-type system-fn]
               (if-let [comp-map (get-in world [:components comp-type])]
                 (->> (system-fn comp-map world delta-time delta-frame)
                      (merge-updated-components world comp-type))
                 world))
             world systems))
  