(ns astrox.ecs.input
  "Translates game inputs into the changes of the ECS entities."
  (:require [tails.pixi.keyboard :as kbd]
            [tails.ecs.core :as ecs]
            [astrox.ecs.components :as c]
            [tails.math.vector2d :as v]))

;; Input handling for the game.
;; Torque produced by the input
(def ^:private base-torque 5)

;; Impulse produced by the thruster
(def ^:private base-impulse 400)

(defn- rotation-input []
  (* base-torque (cond
                   (or (kbd/pressed? kbd/key-code.RIGHT) (kbd/pressed? kbd/key-code.D))  1
                   (or (kbd/pressed? kbd/key-code.LEFT) (kbd/pressed? kbd/key-code.A))  -1
                   :else                                                                 0)))

(defn- thruster-input []
  (* base-impulse (cond
                    (or (kbd/pressed? kbd/key-code.UP) (kbd/pressed? kbd/key-code.W))    1
                    (or (kbd/pressed? kbd/key-code.DOWN) (kbd/pressed? kbd/key-code.S)) -1
                    :else                                                                0)))

(defn- ship-impulse->force 
  "Converts a scalar impulse to the force vector aligned with the ship's current orientation.
   Simulates a thruster engine aligned with the ships direction"
  [rigid-body impulse]
  (-> (v/rotate v/up (.-orientation rigid-body))
      (v/mul impulse)))

(defn- move-player-ship 
  "Move player's ship (it's rigid body component) based on the torque and force produced by keyboard inputs."
  [world]
  (let [torque (rotation-input)
        impulse (thruster-input)]
    (if (or (not= torque 0) (not= impulse 0))
      (if-let [player-eid (first (ecs/entities-with-component world c/Player))]
        (let [rigid-body  (ecs/component world player-eid c/RigidBody)
              force (ship-impulse->force rigid-body impulse)]
          (ecs/update-component world player-eid rigid-body assoc :torque torque :force force))
        world)
      world)))

(defn process-input
  "Handles inputs on each game frame and modifies corresponding components in the ECS world.
   Returns updated version of the world"
  [world]
  (move-player-ship world))
