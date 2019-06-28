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
(def amps-atom (r/atom charts/line-chart))
(def temps-atom (r/atom charts/line-chart))

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
      (swap! batt-pack-atom assoc :data (:batt-pack datasets))
      (swap! amps-atom assoc :data (:amps datasets))
      (swap! temps-atom assoc :data (:temps datasets)))))

(defn mount-chart [comp]
  (js/Chart. (r/dom-node comp) (clj->js (r/props comp))))

(defn update-chart [comp]
  (mount-chart comp))

(defn cells-chart-inner []
  (r/create-class
    {:component-did-mount  mount-chart
     :component-did-update update-chart
     :reagent-render       (fn [comp] [:canvas])}))

(defn chart-outer [config]
  [cells-chart-inner @config])

(defn time-slider-updated [v1]
  (let [slider (.-target v1)
        value (.-value slider)]
    (println "value: " value)))

(defn body []
  [:div.main
   [:div.upload
    [:h1.title "FlexBMS Log Visualizer"]
    [:label "bms.log file:"]
    [upload/upload-input file-selected]
    [:span (:upload-status @state)]]
   [:div.title-border]
   [:div.charts
    [:div.chart
     [:p.chart-title "Cell Voltages"]
     [chart-outer cells-atom]]
    [:div.chart
     [:p.chart-title "Batt/Pack Voltages"]
     [chart-outer batt-pack-atom]]
    [:div.chart
     [:p.chart-title "Amps"]
     [chart-outer amps-atom]]
    [:div.chart
     [:p.chart-title "Temps"]
     [chart-outer temps-atom]]
    [:div.slidercontainer
     [:input.slider {:type "range" :min 1 :max 100 :defaultValue 50 :id "time-range" :width "100%" :onChange time-slider-updated :onInput time-slider-updated}]]
    ]])


(defn ^:export main []
  (add-watch upload/file-data :file-uploaded file-uploaded)
  (r/render [body] (.getElementById js/document "app")))