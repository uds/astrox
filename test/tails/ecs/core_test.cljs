(ns tails.ecs.core-test
  (:require [cljs.test :refer [deftest is]]
            [tails.ecs.core :as ecs]
            [tails.init-specs]))


(defrecord ^:private Comp1 [x])
(defrecord ^:private Comp2 [x])
(defrecord ^:private Comp3 [x])


(deftest create-entity 
  (is (uuid? (ecs/create-entity))))

(deftest test-add-entity
  (let [e1 (ecs/create-entity)
        e2 (ecs/create-entity)
        c1_1 (Comp1. 1)
        c1_2 (Comp1. 2)
        c2_1 (Comp2. 1)
        c1-type (ecs/component-type c1_1)
        c2-type (ecs/component-type c2_1)]
    
    (is (= {:component-types {e1 #{}, e2 #{}}}
           (-> (ecs/add-entity {} e1 nil)
               (ecs/add-entity e2 nil))))

    (is (= {:components {c1-type {e1 c1_2}, c2-type {e1 c2_1}}
            :updated-components {c1-type {e1 c1_2}, c2-type {e1 c2_1}}
            :component-types {e1 #{c1-type, c2-type}}}
           (ecs/add-entity {} e1 [c1_1 c1_2 c2_1])))))

(deftest test-remove-entity
  (let [e1 (ecs/create-entity)
        e2 (ecs/create-entity)
        c1 (Comp1. 1)
        c2 (Comp2. 1)
        c3 (Comp3. 1)
        c1-type (ecs/component-type c1)
        c2-type (ecs/component-type c2)
        c3-type (ecs/component-type c3)
        world (-> (ecs/add-entity {} e1 [c1 c3 c2])
                  (ecs/add-entity e2 [c2])
                  ecs/clear-updated-components)]

    ;; remove e1
    (is (= {:components {c2-type {e2 c2}}
            :removed-components {c1-type  {e1 c1}, c2-type {e1 c2}, c3-type {e1 c3}}
            :component-types {e2 #{c2-type}}}
           (ecs/remove-entity world e1)))

    ;; remove e2
    (is (= {:components {c1-type {e1 c1}, c2-type {e1 c2}, c3-type {e1 c3}}
            :removed-components {c2-type {e2 c2}}
            :component-types {e1 #{c1-type, c2-type, c3-type}}}
           (ecs/remove-entity world e2)))

    ;; remove e1 and e2
    (is (= {:removed-components {c1-type  {e1 c1}, c2-type {e1 c2, e2 c2}, c3-type {e1 c3}}}
           (-> (ecs/remove-entity world e1)
               (ecs/remove-entity e2))))

    ;; remove non-existent entity
    (is (= world (ecs/remove-entity world (ecs/create-entity))))))

(deftest test-add-and-remove-component
  (let [e1 (ecs/create-entity)
        c1_1 (Comp1. 1)
        c1_2 (Comp1. 2)
        c2_1 (Comp2. 1)
        c1-type (ecs/component-type c1_1)
        c2-type (ecs/component-type c2_1)
        c3-type (ecs/component-type (Comp3. 0))
        world (-> (ecs/add-entity {} e1 nil)
                  (ecs/add-component e1 c1_1)
                  (ecs/add-component e1 c1_2)  ;; overrides c1_1 value
                  (ecs/add-component e1 c2_1))]

    ;; add-component c1 and c2
    (is (= {:components {c1-type {e1 c1_2}, c2-type {e1 c2_1}}
            :updated-components {c1-type {e1 c1_2}, c2-type {e1 c2_1}}
            :component-types {e1 #{c1-type, c2-type}}}
           world))
    
    ;; remove-component c2
    (is (= {:components {c1-type {e1 c1_2}}
            :updated-components {c1-type {e1 c1_2}}
            :removed-components {c2-type {e1 c2_1}}
            :component-types {e1 #{c1-type}}}
           (ecs/remove-component world e1 c2-type)))

    ;; remove-component c1 and c2
    (is (= {:removed-components {c2-type {e1 c2_1}, c1-type {e1 c1_2}}}
           (-> (ecs/remove-component world e1 c2-type)
               (ecs/remove-component e1 c1-type))))
    
    ;; remove-component c2 -> no updates
    (is (= {:components {c1-type {e1 c1_2}}
            :removed-components {c2-type {e1 c2_1}}
            :component-types {e1 #{c1-type}}}
           (-> (ecs/clear-updated-components world)
               (ecs/remove-component e1 c2-type))))
    
    ;; remove-component c3 -> non-existent entity or component
    (is (= world (ecs/remove-component world (ecs/create-entity) c1-type)))
    (is (= world (ecs/remove-component world e1 c3-type)))))


(deftest test-update-component
  (let [e1 (ecs/create-entity)
        c1 (Comp1. 23)
        c1-type (ecs/component-type c1)
        world (->
               (ecs/add-entity {} e1 [c1])
               ecs/clear-updated-components)           ;; remove updates from the previous add-entity command
        plus-fn (fn [c n] (Comp1. (+ n (.-x c))))]

    (is (identical? world (ecs/update-component world e1 c1 identity)))
    (is (= {:components {c1-type {e1 (Comp1. 28)}}
            :updated-components {c1-type {e1 (Comp1. 28)}}
            :component-types {e1 #{c1-type}}}
           (ecs/update-component world e1 c1 plus-fn 5)))))

(deftest test-clear-updated-components
  (let [e1 (ecs/create-entity)
        c1 (Comp1. 23)
        c1-type (ecs/component-type c1)
        world (ecs/add-entity {} e1 [c1])]
    (is (= {c1-type {e1 c1}} (:updated-components world)))
    (is (empty? (:updated-components (ecs/clear-updated-components world))))))

(deftest test-get-component 
  (let [e1 (ecs/create-entity)
        c1 (Comp1. 23)
        world (ecs/add-entity {} e1 [c1])]
    (is (nil? (ecs/component world e1 nil)))
    (is (nil? (ecs/component world e1 Comp3)))
    (is (identical? c1 (ecs/component world e1 Comp1)))))

(deftest test-get-entity-components
  (let [e1 (ecs/create-entity)
        e2 (ecs/create-entity)
        c1 (Comp1. 23)
        c2 (Comp2. 46)
        world (-> (ecs/add-entity {} e1 [c1 c2])
                  (ecs/add-entity e2 []))]
    (is (empty? (ecs/entity-components world (ecs/create-entity))))
    (is (= [c1 c2] (ecs/entity-components world e1)))
    (is (empty? (ecs/entity-components world e2)))))

(deftest test-select-components
  (let [e1 (ecs/create-entity)
        c1 (Comp1. 23)
        c2 (Comp2. 46)
        world (ecs/add-entity {} e1 [c1 c2])]
    (is (= [] (ecs/select-components world e1 [])))
    (is (= [nil nil] (ecs/select-components world e1 [Comp3 Comp3])))
    (is (= [c1] (ecs/select-components world e1 [Comp1])))
    (is (= [c2 c1] (ecs/select-components world e1 [Comp2 Comp1])))
    (is (= [c1 nil c2] (ecs/select-components world e1 [Comp1 Comp3 Comp2])))))

(deftest test-entities-with-component
  (let [e1 (ecs/create-entity)
        e2 (ecs/create-entity)
        e3 (ecs/create-entity)
        c1 (Comp1. 23)
        c2 (Comp2. 56)
        world (-> {}
                  (ecs/add-entity e1 [c1])
                  (ecs/add-entity e2 [c2])
                  (ecs/add-entity e3 [c1 c2]))]
    (is (nil? (ecs/entities-with-component world nil)))
    (is (= [e1 e3] (ecs/entities-with-component world Comp1)))
    (is (= [e2 e3] (ecs/entities-with-component world Comp2)))
    (is (nil? (ecs/entities-with-component world Comp3)))))