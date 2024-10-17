(ns astrox.ecs.views.protocols)

(defprotocol
 ^{:doc "A game object is a view that represents an entity in the game world."}
 GameObject
  (root-sprite [this] "Returns the view's root sprite.")
  (destroy [this] "Destroys the view.")
  (set-position [this pos] "Sets the view's position.")
  (set-orientation [this angle] "Sets the view's orientation."))

(defprotocol Debuggable
  ^{:doc "A debuggable view is a game object that can show its collider."}
  (show-collider [this collider] "Shows the view's collider.")
  (hide-collider [this] "Hides the view's collider."))

(defprotocol
 ^{:doc "A destructible view is a game object that can be damaged."}
 Destructible
  (set-health [this health] "Sets the view's health level as a value in [0..1] range.")
  (set-shield [this shield] "Sets the view's shield level as a value in [0..1] range."))

(defprotocol
 ^{:doc "A self-propelled view is a game object that can move itself."}
 SelfPropelled
  (set-thrust [this thrust] "Sets the view's thrust level as a value in [0..1] range."))
