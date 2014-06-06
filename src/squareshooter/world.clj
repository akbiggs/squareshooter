(ns squareshooter.world
  (:require [quil.core :as q]
            [game-utils.input :as input]
            [game-utils.helpers :as helpers]))

(defn setup [width height]
  {:width width
   :height height
   :player {:position {:x 50 :y 50}}})

(defn update [ctx world]
  (let [in (:input ctx)
        dt (:elapsed-time ctx)]
    (helpers/react* world
                    (input/key-down? \z in)
                    (update-in [:player :position] #(assoc % :y (+ (:y %) 10)))

                    (input/key-down? \x in)
                    (update-in [:player :position] #(assoc % :y (- (:y %) 10))))))

(defn draw [world]
  (q/background 255)
  (let [{:keys [x y]} (:position (:player world))]
    (q/rect x y 50 50)))

