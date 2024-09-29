(ns tails.rinc.reaction
  "Defines reaction macros.")

(defmacro reaction*
  "Creates a new reaction by calling the `make-reaction` function with a given body"
  [& body]
  `(tails.rinc.reaction/make-reaction nil (fn [] ~@body)))

(defmacro reaction
  "Creates a new reaction by calling the `make-reaction` function with a given body"
  [id & body]
  `(tails.rinc.reaction/make-reaction ~id (fn [] ~@body)))

(defmacro defreaction 
  "Equivalent to '(def x (reaction 'x ...))'"
  [name & body]
  `(def ~name
     (tails.rinc.reaction/make-reaction ~name (fn [] ~@body))))