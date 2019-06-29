(ns bmsvis.main
  (:require [bmsvis.data :as data]
            [bmsvis.upload :as upload]
            [bmsvis.charts :as charts]
            [reagent.core :as r]
            [cljsjs.chartjs]
            [clojure.string :as str]))

(def max-data-size 1024)
(def min-zoom 5)
(def ln2 (.log js/Math 2))

(defn max-zoom-level [n]
  (let [lnn (.log js/Math n)
        log2 (/ lnn ln2)
        log2r (int (+ 0.9999999999999 log2))]
    (max min-zoom log2r)))

(defn zoom->tickn [level]
  (max 32 (.pow js/Math 2 level)))

(defn calibrate-zoom [state]
  (let [max-zoom (max-zoom-level (:tick-count state))]
    (assoc state :zoom-max max-zoom
                 :zoom max-zoom)))

(defn calibrate-pan [state]
  (let [tick-count (:tick-count state)
        size (zoom->tickn (:zoom state))
        middle (int (+ (:pan-start state) (/ (:pan-size state) 2)))
        new-start (- middle (int (/ size 2)))]
    (assoc state :pan-start (max 0 new-start)
                 :pan-size size
                 :pan-nth (max 1 (int (/ size max-data-size)))
                 :pan-step (/ size 4)
                 :pan-max (max 0 (- tick-count size)))))

(def start-state
  {:upload-status "<- Please choose a bms.log file."
   :ticks         []
   :tick-count    0
   :pan-start     0
   :pan-size      (zoom->tickn min-zoom)
   :pan-nth       1
   :zoom          min-zoom
   :zoom-max      min-zoom})

(def state (r/atom start-state))
(def cells-chart  (atom nil))
(def batt-pack-chart  (atom nil))
(def amps-chart  (atom nil))
(def temps-chart  (atom nil))

(defn file-selected []
  (swap! state assoc :upload-status "Uploading file..."))

(defn install-ticks [ticks state]
  (let [tick-count (count ticks)]
    (-> state
        (assoc :ticks ticks
               :tick-count tick-count
               :upload-status "Data processed.  See charts below.")
        calibrate-zoom
        calibrate-pan)))

(defn update-chart! [chart data]
  (set! (.-data chart) (clj->js data))
  (.update chart))

(defn load-datasets []
  (let [{:keys [ticks pan-nth pan-start pan-size]} @state
        ticks (->> ticks
                   (drop pan-start)
                   (drop-while #(not (:cells %)))
                   (take pan-size)
                   (take-nth pan-nth))
        datasets (charts/ticks->datasets ticks)]
    (update-chart! @cells-chart (:cells datasets))
    (update-chart! @batt-pack-chart (:batt-pack datasets))
    (update-chart! @amps-chart (:amps datasets))
    (update-chart! @temps-chart (:temps datasets))))

(defn file-uploaded [key atom old-state new-state]
  (swap! state assoc :upload-status "File uploaded. Processing data...")
  (let [ticks (data/lines->timeline (str/split-lines new-state))]
    (swap! state #(install-ticks ticks %))
    (load-datasets)))

(defn pan-updated [v1]
  (let [slider (.-target v1)
        value (js/parseInt (.-value slider))]
    (swap! state assoc :pan-start value)
    (load-datasets)))

(defn zoom-updated [v1]
  (let [slider (.-target v1)
        value (js/parseInt (.-value slider))]
    (swap! state #(-> %
                      (assoc :zoom value)
                      calibrate-pan))
    (load-datasets)))

(defn chart [chart-atom]
  (r/create-class
    {:display-name        "line-chart"
     :component-did-mount (fn [comp]
                            (let [chart (js/Chart. (r/dom-node comp) (clj->js charts/line-chart))]
                              (reset! chart-atom chart)
                              chart))
     :reagent-render      (fn [comp] [:canvas])}))

(defn body []
  [:div.main
   [:div.upload
    [:h1.title "FlexBMS Log Visualizer"]
    [:label "bms.log file:"]
    [upload/upload-input file-selected]
    [:span (:upload-status @state)]]
   [:div.title-border]
   [:div.slidercontainer
    [:label "Pan"]
    [:div
     [:input.slider.pan
      {:type         "range" :min 0 :max (:pan-max @state) :step (:pan-step @state)
       :defaultValue (:pan-start @state)
       :onChange     pan-updated :onInput pan-updated}]]
    [:div.details
     [:span [:strong (:tick-count @state)] " ticks loaded."]
     [:span
      "Viewing " [:strong (:pan-start @state)]
      " - "
      [:strong (+ (:pan-start @state) (:pan-size @state))]
      ", every nth (" [:strong (:pan-nth @state)] ") tick."]]]
   [:div.slidercontainer
    [:label "Zoom"]
    [:div
     [:input.slider.zoom
      {:type         "range" :min min-zoom :max (:zoom-max @state)
       :defaultValue (:zoom @state)
       :onChange     zoom-updated :onInput zoom-updated}]]
    [:div.details
     [:span "Viewing " [:strong (:pan-size @state)] " ticks (max)."]]]
   [:div.title-border]
   [:div.charts
    [:div.chart
     [:p.chart-title "Cell Voltages"]
     [chart cells-chart]]
    [:div.chart
     [:p.chart-title "Batt/Pack Voltages"]
     [chart batt-pack-chart]]
    [:div.chart
     [:p.chart-title "Amps"]
     [chart amps-chart]]
    [:div.chart
     [:p.chart-title "Temps"]
     [chart temps-chart]]

    ]])


(defn ^:export main []
  (add-watch upload/file-data :file-uploaded file-uploaded)
  (r/render [body] (.getElementById js/document "app")))