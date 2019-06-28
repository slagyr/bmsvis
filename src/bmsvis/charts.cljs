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

(def MAX 3000)

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

(defn fill-datasets [datasets ticks]
  (loop [ticks ticks datasets datasets]
    (if-let [tick (first ticks)]
      (recur (rest ticks)
             (-> datasets
                 (fill-cells tick)
                 (fill-batt-pack tick)))
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

(defn ticks->datasets [ticks]
  (let [ticks (take MAX ticks)
        cells (bare-cells-dataset ticks)
        batt-pack (bare-batt-pack-dataset)]
    (fill-datasets {:cells     cells
                    :batt-pack batt-pack}
                   ticks)))

