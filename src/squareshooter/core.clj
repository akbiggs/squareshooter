(ns squareshooter.core
  (:require [game-utils.game :as game]
            [squareshooter.world :as world])
  (:gen-class :main true))

(defn -main [& args]
  (game/create "Squares" [400 400]
               world/setup
               (fn [ctx world] (world/update ctx world))
               (fn [world] (world/draw world))))

(-main)
