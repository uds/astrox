(ns astrox.game.levels
  (:require [tails.ecs.core :as ecs]
            [tails.math.core :as math]
            [tails.math.vector2d :as v]
            [astrox.ecs.entities :as e]))

(defn- create-random-meteor []
  (e/create-meteor {:position    (v/rand-in-circle (v/vec2d 400 400) 350)
                    ;; :force       (->> (math/rand-num 50000 150000)
                    ;;                   (v/rand-in-circle v/zero))
                    :torque      (math/rand-num 10 70)}))

(defn- create-meteors-field [world]
  (let [meteors (repeatedly 20 create-random-meteor)]
    (reduce (fn [w m] (ecs/add-entity w m))
            world meteors)))

(defn- create-two-overlapping-meteors [world]
  (-> world
      (ecs/add-entity (e/create-meteor {:position (v/vec2d 500 500)}))
      (ecs/add-entity (e/create-meteor {:position (v/vec2d 530 530)}))))

(defn create-level01
  "Create a new level-01 in the ECS world"
  [world]
  (-> world
      ;; meteors
      (create-meteors-field)
      ;;(create-two-overlapping-meteors)

      ;; player ship
      (ecs/add-entity (e/create-player-ship {:position    (v/vec2d 300 300)
                                             :orientation 0}))))
