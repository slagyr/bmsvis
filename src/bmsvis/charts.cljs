(ns bmsvis.charts)

(def cell-colors ["#86C98A" "#54A759" "#2D8632" "#116416" "#004304"
                  "#67989A" "#417E80" "#236467" "#0D4A4D" "#003133"
                  "#FFD1AA" "#D49B6A" "#AA6D39" "#804615" "#552700"
                  "#FFACAA" "#D46D6A" "#AA3C39" "#801815" "#550200"])

(def empty-dataset {:data             []
                    :backgroundColor  "rgba(255, 255, 255, 0)"
                    :pointBorderColor "rgba(255, 255, 255, 0)"
                    :borderWidth      1
                    :label            "missing"
                    :borderColor      "#FF0000"
                    })

(def starting-chart-data {:labels   [3 1 4 1 5 6 2]
                          :datasets [(assoc empty-dataset
                                       :data [1 1 1 1 1 1 1]
                                       :label "Awating data"
                                       :borderColor (first cell-colors))]})

(def line-chart {:type    "line"
                 :options {:responsive          true
                           :maintainAspectRatio false}
                 :data    starting-chart-data})

(defn fill-cells [datasets tick]
  (if-let [cells (:cells tick)]
    (let [cell-datasets (get-in datasets [:cells :datasets])]
      (-> datasets
          (update-in [:cells :labels] conj (:id tick))
          (assoc-in [:cells :datasets]
                    (map #(update-in %2 [:data] conj %1) cells cell-datasets))))
    datasets))

(defn fill-batt-pack [datasets tick]
  (if-let [batt-v (:batt-v tick)]
    (let [pack-v (:pack-v tick)]
      (-> datasets
          (update-in [:batt-pack :labels] conj (:id tick))
          (update-in [:batt-pack :datasets 0 :data] conj batt-v)
          (update-in [:batt-pack :datasets 1 :data] conj pack-v)))
    datasets))

(defn fill-amps [datasets tick]
  (if-let [amps (:amps tick)]
    (-> datasets
        (update-in [:amps :labels] conj (:id tick))
        (update-in [:amps :datasets 0 :data] conj amps))
    datasets))

(defn fill-temps [datasets tick]
  (if-let [temp1 (:temp1 tick)]
    (let [temp2 (:temp2 tick)
          temp3 (:temp3 tick)]
      (-> datasets
          (update-in [:temps :labels] conj (:id tick))
          (update-in [:temps :datasets 0 :data] conj temp1)
          (update-in [:temps :datasets 1 :data] conj temp2)
          (update-in [:temps :datasets 2 :data] conj temp3)))
    datasets))

(defn fill-datasets [datasets ticks]
  (loop [ticks ticks datasets datasets]
    (if-let [tick (first ticks)]
      (recur (rest ticks)
             (-> datasets
                 (fill-cells tick)
                 (fill-batt-pack tick)
                 (fill-amps tick)
                 (fill-temps tick)))
      datasets)))

(defn bare-cells-dataset [ticks]
  (let [cell-count (count (:cells (first (filter :cells ticks))))]
    {:labels   []
     :datasets (->> (take cell-count (repeat empty-dataset))
                    (map #(assoc %2 :borderColor %1) cell-colors)
                    (map #(assoc %2 :label %1) (rest (range))))}))

(defn bare-batt-pack-dataset []
  (let [batt-v (assoc empty-dataset :label "Battery Voltage" :borderColor "#2D8632")
        pack-v (assoc empty-dataset :label "Pack Voltage" :borderColor "#AA6D39")]
    {:labels   []
     :datasets [batt-v pack-v]}))

(defn bare-amps-dataset []
  {:labels   []
   :datasets [(assoc empty-dataset :label "Amps : > 1 Charge current, < 1 Discharge current"
                                   :borderColor "#AA3C39")]})

(defn bare-temps-dataset []
  (let [temp1 (assoc empty-dataset :label "Temp #1" :borderColor "#2D8632")
        temp2 (assoc empty-dataset :label "Temp #2" :borderColor "#236467")
        temp3 (assoc empty-dataset :label "Temp #3" :borderColor "#AA6D39")]
    {:labels   []
     :datasets [temp1 temp2 temp3]}))

(defn ticks->datasets [ticks]
  (let [cells (bare-cells-dataset ticks)
        batt-pack (bare-batt-pack-dataset)
        amps (bare-amps-dataset)
        temps (bare-temps-dataset)]
    (fill-datasets {:cells     cells
                    :batt-pack batt-pack
                    :amps      amps
                    :temps     temps}
                   ticks)))

(defn ticks->msg-groups [ticks size]
  (let [part-n (min size 32)
        part-size (.ceil js/Math (/ size part-n))
        parts (partition part-size ticks)]
    {:alerts (map #(filter :alert %) parts)
     :errors (map #(filter :error %) parts)
     :info (map #(filter :info %) parts)}))

