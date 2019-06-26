(ns bmsvis.data
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn conj*
  [s x]
  (conj (vec s) x))

(defn ->int [s]
  (Integer/parseInt (str/trim s)))

(defn ->float [s]
  (Float/parseFloat (str/trim s)))

(defn maybe-tick [line]
  (when (str/starts-with? line "tick:")
    (let [tokens (str/split (subs line 5) #",")]
      {:id   (->int (first tokens))
       :time (->int (second tokens))})))

(defn do-info [line tick]
  (update-in tick [:info] conj* (subs line 6)))

(defn do-error [line tick]
  (update-in tick [:error] conj* (subs line 7)))

(defn do-alert [line tick]
  (update-in tick [:alert] conj* (subs line 7)))

(defn do-unlabeled [line tick]
  (update-in tick [:error] conj* line))

(defn do-pack [line tick]
  (let [tokens (str/split (subs line 5) #",")]
    (merge tick
           {:batt_v (->float (first tokens))
            :pack_v (->float (second tokens))
            :amps   (->float (nth tokens 2))})))

(defn do-temps [line tick]
  (let [tokens (str/split (subs line 6) #",")]
    (merge tick
           {:temp1 (->float (first tokens))
            :temp2 (->float (second tokens))
            :temp3 (->float (nth tokens 2))})))

(defn do-cells [line tick]
  (let [tokens (str/split (subs line 6) #",")]
    (assoc tick :cells (map ->float tokens))))

(defn parse-line [line tick]
  (cond
    (str/starts-with? line "pack:") (do-pack line tick)
    (str/starts-with? line "temps:") (do-temps line tick)
    (str/starts-with? line "cells:") (do-cells line tick)
    (str/starts-with? line "info:") (do-info line tick)
    (str/starts-with? line "error:") (do-error line tick)
    (str/starts-with? line "alert:") (do-alert line tick)
    :else (do-unlabeled line tick)))

(defn empty-tick? [tick]
  (= {} (dissoc tick :id :time)))

(defn lines->timeline [lines]
  (loop [lines lines ticks [] tick {:id 0 :time 0}]
    (if (seq lines)
      (let [line (first lines)]
        (if-let [new-tick (maybe-tick line)]
          (if (empty-tick? tick)
            (recur (rest lines) ticks new-tick)
            (recur (rest lines) (conj ticks tick) new-tick))
          (recur (rest lines) ticks (parse-line line tick))))
      (if (empty-tick? tick)
        ticks
        (conj ticks tick)))))

(defn log->timeline [filename]
  (with-open [rdr (io/reader filename)]
    (-> rdr
        line-seq
        lines->timeline)))