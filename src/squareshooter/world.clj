(ns squareshooter.world
  (:require [quil.core :as q]

            [game-utils.profiler :as p]
            [game-utils.input :as input]
            [game-utils.helpers :as helpers]
            [game-utils.point :as point]
            [game-utils.effects :as fx]))

(defn setup [width height]
  {:width width
   :height height
   :player {:position (point/create 50 50)
            :size (point/create 50 50)}
   :bullets nil
   :next-wave-time 1000
   :enemies nil})

(defn spawn-enemy [time]
  {:position (point/create 450 200)
   :size (point/create 50 50)
   :health 3
   :last-damage-time time
   :spawn-time time})

(defn spawn-wave-when-ready [world dt]
  (p/p :spawn-wave (let [wave-time-left (- (:next-wave-time world) dt)
        should-spawn-wave (< wave-time-left 0)]
    (assoc world
      :next-wave-time (mod wave-time-left 5000)
      :enemies (helpers/react (:enemies world) should-spawn-wave
                              (conj (spawn-enemy (helpers/now))))))))

(defn fire-bullet [object bullet-size speed direction]
  (let [{:keys [position size]} object
        bullet-fire-offset-y (- (/ (:y size) 2) (/ (:y bullet-size) 2))
        bullet-fire-offset-x (if (= :left direction) 0 (:x size))
        bullet-offset (point/create bullet-fire-offset-y bullet-fire-offset-x)
        bullet-position (point/add position bullet-offset)
        direction-factor (if (= :left direction) -1 1)]
    {:position bullet-position
     :size bullet-size
     :velocity (point/create (* speed direction-factor) 0)}))

(defn move [object offset]
  (update-in object [:position]
             #(point/add % offset)))

(defn left [object]
  (:x (:position object)))

(defn top [object]
  (:y (:position object)))

(defn right [object]
  (+ (:x (:position object)) (:x (:size object))))

(defn bottom [object]
  (+ (:y (:position object)) (:y (:size object))))

(defn offscreen? [object world]
  (or (< (right object) 0)
      (< (bottom object) 0)
      (> (left object) (:width world))
      (> (top object) (:height world))))

(def onscreen? (comp not offscreen?))

(defn collides? [object other]
  (and (< (left object) (right other))
       (> (right object) (left other)))
       (< (bottom object) (top other))
       (> (top object) (bottom other)))

(defn update-enemy [enemy world]
  (-> enemy
      (update-in [:position]
                 #(assoc % :x (- (:x %) 2)))
      (update-in [:health]
                 #(if (helpers/any? (map (fn [bullet] (collides? bullet enemy))
                                         (:bullets world)))
                    (max (dec %) 0)
                    %))))

(defn update-player [player in world]
  (-> player
      (update-in [:position]
                 (fn [pos]
                   (let [mv-speed (if (:firing player) 5 10)]
                     (input/wasd pos in
                                 (assoc :y (- (:y pos) mv-speed))
                                 (assoc :x (- (:x pos) mv-speed))
                                 (assoc :y (+ (:y pos) mv-speed))
                                 (assoc :x (+ (:x pos) mv-speed))))))))

(defn update-bullets [bullets in world]
  (-> bullets
      (input/controls in
                      \z :down (conj (fire-bullet (:player world) (point/create 10 10) 5 :right)))
      (->> (map (fn [bullet] (move bullet (:velocity bullet)))))
      (->> (filter (fn [bullet] (not (offscreen? bullet world)))))))

(defn update [world ctx]
  (let [{in :input dt :elapsed-time} ctx]
    (-> world
        (spawn-wave-when-ready dt)
        (update-in [:enemies] #(filter (fn [e] (> (right e) 0))
                                       (for [enemy %]
                                         (update-enemy enemy world))))
        (update-in [:player] #(update-player % in world))
        (update-in [:bullets]
                   #(update-bullets % in world)))))

(defn draw [world]
  (q/background 255)
  (let [{:keys [x y] :as pos} (:position (:player world))
        {size-x :x size-y :y} (:size (:player world))
        center-pos (point/add pos (point/create 25 25))
        {p1-x :x p1-y :y} (fx/radial-motion center-pos 55)
        {p2-x :x p2-y :y} (fx/radial-motion center-pos 55 :offset q/PI)]
    (q/fill 0 0 255)
    (q/rect x y size-x size-y)

    (q/rect p1-x p1-y 5 5)
    (q/rect p2-x p2-y 5 5))

  (doseq [enemy (:enemies world)]
    (let [{:keys [x y]} (:position enemy)
          {size-x :x size-y :y} (:size enemy)
          {:keys [health last-damage-time]} enemy]
      (q/fill (* 75 health) 0 0)
      (q/rect x y size-x size-y)))

  (q/fill 255 0 0)
  (p/p :draw-bullets (doseq [bullet (:bullets world)]
    (let [{:keys [position size velocity]} bullet
          {pos-x :x pos-y :y} position
          {size-x :x size-y :y} size]
      (q/rect pos-x pos-y size-x size-y)))))
