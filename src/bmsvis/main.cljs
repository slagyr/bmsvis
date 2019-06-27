(ns bmsvis.main
  (:require [bmsvis.data :as data]
            [bmsvis.upload :as upload]
            [reagent.core :as r]))

(def click-count (r/atom 0))

(defn counting-component []
  [:div
   "The atom " [:code "click-count"] " has value: "
   @click-count ". "
   [:input {:type "button" :value "Click me!"
            :on-click #(reset! click-count (inc @click-count))}]])

(defn ^:export main []
  (r/render [counting-component] (.getElementById js/document "app")))

