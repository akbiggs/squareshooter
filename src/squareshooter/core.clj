(ns squareshooter.core
  (:require [game-utils.game :as game]
            [squareshooter.world :as world]))

(defn -main [& args]
  (game/create "Squares" [400 400]
               world/setup world/update world/draw))

(-main)
