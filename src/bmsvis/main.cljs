(ns bmsvis.main
  (:require [bmsvis.data :as data]
            [bmsvis.upload :as upload]
            [bmsvis.charts :as charts]
            [reagent.core :as r]
            [cljsjs.chartjs]
            [clojure.string :as str]))

(def start-state
  {:upload-status "<- Please choose a bms.log file."
   :ticks         []})

(def state (r/atom start-state))
(def cells-atom (r/atom charts/line-chart))
(def batt-pack-atom (r/atom charts/line-chart))

(defn file-selected []
  (swap! state assoc :upload-status "Uploading file..."))

(defn install-ticks [ticks state]
  (-> state
      (assoc :ticks ticks)
      (assoc :upload-status "Data processed.  See charts below.")))

(defn file-uploaded [key atom old-state new-state]
  (swap! state assoc :upload-status "File uploaded. Processing data...")
  (let [ticks (data/lines->timeline (str/split-lines new-state))]
    (swap! state #(install-ticks ticks %))
    (let [datasets (charts/ticks->datasets ticks)]
      (swap! cells-atom assoc :data (:cells datasets))
      (prn "(:batt-pack datasets): " (:batt-pack datasets))
      (swap! batt-pack-atom assoc :data (:batt-pack datasets)))))

(defn mount-chart [comp]
  (js/Chart. (r/dom-node comp) (clj->js (r/props comp))))

(defn update-chart [comp]
  (mount-chart comp))

(defn cells-chart-inner []
  (r/create-class
    {:component-did-mount  mount-chart
     :component-did-update update-chart
     :reagent-render       (fn [comp] [:canvas])}))

(defn cells-chart-outer [config]
  [cells-chart-inner @config])

(defn body []
  [:div.main
   [:h1.title "FlexBMS Log Visualizer"]
   [:div.title-border]
   [:div.upload
    [:label "bms.log file:"]
    [upload/upload-input file-selected]
    [:span (:upload-status @state)]]
   [:div.title-border]
   [:div.charts
    [:div.chart
     [:p.chart-title "Cell Voltages"]
     [cells-chart-outer cells-atom]]]
   [:div.chart
    [:p.chart-title "Batt/Pack Voltages"]
    [cells-chart-outer batt-pack-atom]]])

(defn ^:export main []
  (add-watch upload/file-data :file-uploaded file-uploaded)
  (r/render [body] (.getElementById js/document "app")))