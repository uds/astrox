(ns cljs.user
  (:require [tails.rinc.reaction :as rn]
            [tails.ecs.core :as ecs]
            [astrox.ecs.world :as w]))


;; -- RX ----------------------------------------------------------------


(defn rx-deps* [!ratom]
  (println "strict digraph rx_deps {")
  (rn/walk-deps
   (fn [p r]
     (when p
       (println (pr-str (str (.-id p))) "->" (pr-str (str (.-id r)))))
     (.-id r))
   nil !ratom)
  (print "}"))

(defn rx-deps
  "Print reactive dependencies as DOT graph. 
   Uses https://dreampuf.github.io/GraphvizOnline to render the result."
  [!ratom]
  (js/encodeURI
   (str "https://dreampuf.github.io/GraphvizOnline/#" (with-out-str (rx-deps* !ratom)))))


;; -- ECS ----------------------------------------------------------------


(defn world [] 
  w/!ecs-world)

(defn entities* []
  (-> (world) :component-types keys))

(defn ->eid
  "Converts index of entity into the UUID, if necessary"
  [eid]
  (if-not (uuid? eid)
    (nth (entities*) eid)
    eid))

(defn component-types [eid]
  (get-in (world) [:component-types (->eid eid)]))

(defn entities
  ([]
   (map-indexed (fn [i id] [i id (component-types id)])
                (entities*)))
  ([comp-type]
   (filter (fn [[_ eid]] (ecs/component (world) eid comp-type)) (entities))))

(defn entity [eid]
  (ecs/entity-components (world) (->eid eid)))

(defn component [eid comp-type]
  (ecs/component (world) (->eid eid) comp-type))

