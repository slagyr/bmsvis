(ns bmsvis.charts-spec
  (:require [speclj.core]
            [bmsvis.data :as data]
            [bmsvis.charts :as charts]
            [bmsvis.sample-data :as sample]
            [clojure.string :as str])
  (:require-macros [speclj.core :refer [describe context it should= should-not= after before should-contain around with]]))

(describe "Chart data"

  {:labels   (map str (range 5))
   :datasets [{:data            [5 10 15 20 25]
               :label           "Rev in MM"
               :borderColor     "#90EE90"
               :backgroundColor "rgba(255, 255, 255, 0)"}
              {:data            [3 6 9 12 15]
               :label           "Cost in MM"
               :borderColor     "#F08080"
               :backgroundColor "rgba(255, 255, 255, 0)"}]}

  (it "generates cell datasets"
    (let [ticks (data/lines->timeline (str/split-lines sample/sample))
          cells (:cells (charts/ticks->datasets ticks))]
      (should= [1 21 41] (:labels cells))
      (should= 10 (count (:datasets cells)))
      (should= (take 10 (repeat "rgba(255, 255, 255, 0)"))
               (map :backgroundColor (:datasets cells)))
      (should= (take 10 charts/cell-colors)
               (map :borderColor (:datasets cells)))
      (should= (range 1 11)
               (map :label (:datasets cells)))
      (should= (take 10 (repeat 3)) (map #(count (:data %)) (:datasets cells)))))

  (it "generates batt/pack datasets"
      (let [ticks (data/lines->timeline (str/split-lines sample/sample))
            volts (:batt-pack (charts/ticks->datasets ticks))]
        (should= (filter odd? (range 42)) (:labels volts))
        (should= 2 (count (:datasets volts)))
        (should= 21 (count (:data (first (:datasets volts)))))
        (should= 21 (count (:data (second (:datasets volts)))))
        ))
  )


