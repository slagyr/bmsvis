(ns bmsvis.main
  (:require [bmsvis.data :as data]
            [bmsvis.upload :as upload]
            [bmsvis.charts :as charts]
            [reagent.core :as r]
            [cljs.pprint :refer [pprint]]
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
   :zoom-max      min-zoom
   :alerts        []
   :errors        []
   :infos         []
   :log           nil
   :tooltip       {:x    100
                   :y    100
                   :text nil}})

(def state (r/atom start-state))
(def tooltip (r/cursor state [:tooltip]))
(def cells-chart (atom nil))
(def batt-pack-chart (atom nil))
(def amps-chart (atom nil))
(def temps-chart (atom nil))

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
                   (take pan-size))
        msg-groups (charts/ticks->msg-groups ticks pan-size)
        log (when (< pan-size 257) ticks)
        ticks (if (> (count ticks) max-data-size)
                (drop-while #(not (:cells %)) ticks)
                ticks)
        ticks (take-nth pan-nth ticks)
        datasets (charts/ticks->datasets ticks)]
    (swap! state #(-> %
                      (merge msg-groups)
                      (assoc :log log)))
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

(defn chart [chart-atom conf]
  (r/create-class
    {:display-name        "line-chart"
     :component-did-mount (fn [comp]
                            (let [chart (js/Chart. (r/dom-node comp) (clj->js conf))]
                              (reset! chart-atom chart)
                              chart))
     :reagent-render      (fn [a] [:canvas])}))

(defn message-tooltip [e key ticks]
  (when-not (:text @tooltip)
    (let [n (count ticks)
          x (- (.-clientX e) 125)
          y (+ (.-clientY e) 20)]
      (swap! tooltip assoc
             :text (if (= 1 n)
                     (str "Tick #" (:id (first ticks)) ". Click to zoom.")
                     (str "Click to zoom in on these " (name key) "s."))
             :lines (if (= 1 n) (get (first ticks) key) nil)
             :x x
             :y y))))

(defn zoom-on-ticks [ticks]
  (if (= 1 (count ticks))
    (let [tick (first ticks)]
      (swap! state #(-> %
                        (assoc :zoom min-zoom)
                        (assoc :pan-start (:id tick) :pan-size 1)
                        calibrate-pan)))
    (let [start (:id (first ticks))
          end (:id (last ticks))
          size (- end start)
          zoom (max-zoom-level size)]
      (swap! state #(-> %
                        (assoc :zoom zoom)
                        (assoc :pan-start start :pan-size size)
                        calibrate-pan))))
  (load-datasets))

(defn message-tick [key ticks]
  (let [n (count ticks)]
    (if (= 0 n)
      [:div.message-marker [:span.empty]]
      (let [id (str (name key) (:id (first ticks)))]
        [:div.message-marker {:onMouseOver #(message-tooltip %1 key ticks)
                              :onMouseOut  #(swap! tooltip assoc :text nil :lines nil)
                              :onClick     #(zoom-on-ticks ticks)}
         [:span {:class (name key)}
          (cond
            (= n 0) ""
            (> n 999) "999"
            :else n)]]))))


(defn message-chart [msgs-atom key]
  (let [msgs @msgs-atom]
    [:div.message-bar
     [:div.message-bar-title (reduce #(+ %1 (count %2)) 0 msgs) " " (str (name key) "s")]
     [:div.message-markers
      (for [[i ticks] (map-indexed vector @msgs-atom)]
        ^{:key (str key i)} [message-tick key ticks])]]))

(defn body []
  [:div.main
   [:div#tooltip {:style {:left       (:x @tooltip)
                          :top        (:y @tooltip)
                          :visibility (if (:text @tooltip) "visible" "hidden")}}
    (when-let [text (:text @tooltip)]
      [:span text])
    (when-let [lines (:lines @tooltip)]
      (for [[i line] (map-indexed vector lines)]
        [:pre {:key i} line]))]
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
    [:div.messages-chart
     [message-chart (r/cursor state [:alerts]) :alert]
     [message-chart (r/cursor state [:errors]) :error]
     [message-chart (r/cursor state [:infos]) :info]]
    [:div.chart
     [:p.chart-title "Cell Voltages"]
     [chart cells-chart charts/line-chart]]
    [:div.chart
     [:p.chart-title "Batt/Pack Voltages"]
     [chart batt-pack-chart charts/line-chart]]
    [:div.chart
     [:p.chart-title "Amps"]
     [chart amps-chart charts/line-chart]]
    [:div.chart
     [:p.chart-title "Temps"]
     [chart temps-chart charts/line-chart]]]
   [:p.chart-title "Info/Alert/Error Log (show only on high zoom)"]
   [:div.log
    (when-let [ticks (:log @state)]
      (for [tick ticks]
        (list
          [:pre.plain "Tick #" (:id tick)]
          (when (:info tick)
            [:pre.info {:key (str (:id tick) "info")}
             (str/join "\n" (:info tick))])
          (when (:alert tick)
            [:pre.alert {:key (str (:id tick) "alert")}
             (str/join "\n" (:alert tick))])
          (when (:error tick)
            [:pre.error {:key (str (:id tick) "error")}
             (str/join "\n" (:error tick))]))))]
   ])


(defn ^:export main []
  (add-watch upload/file-data :file-uploaded file-uploaded)
  (r/render [body] (.getElementById js/document "app")))