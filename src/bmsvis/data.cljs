(ns bmsvis.data
  (:require [clojure.string :as str]))

(defn conj*
  [s x]
  (conj (vec s) x))

(defn parse! [f v]
  (let [result (f v)]
    (if (js/isNaN result)
      (throw (js/Error "parsed NaN"))
      result)))

(defn ->int [s]
  (parse! js/parseInt (str/trim s)))

(defn ->float [s]
  (parse! js/parseFloat (str/trim s)))

(defn tick? [line]
  (and line (str/starts-with? line "tick:")))

(defn maybe-tick [line]
  (when (tick? line)
    (let [tokens (str/split (subs line 5) #",")]
      {:id   (->int (first tokens))
       :time (->int (second tokens))})))

(defn do-tick [line tick]
  (let [tokens (str/split (subs line 5) #",")]
    (assoc tick :id (->int (first tokens))
                :time (->int (second tokens)))))

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
           {:batt-v (->float (first tokens))
            :pack-v (->float (second tokens))
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
    (str/starts-with? line "tick:") (do-tick line tick)
    (str/starts-with? line "pack:") (do-pack line tick)
    (str/starts-with? line "temps:") (do-temps line tick)
    (str/starts-with? line "cells:") (do-cells line tick)
    (str/starts-with? line "info:") (do-info line tick)
    (str/starts-with? line "error:") (do-error line tick)
    (str/starts-with? line "alert:") (do-alert line tick)
    :else (do-unlabeled line tick)))

(defn empty-tick? [tick]
  (= {} (dissoc tick :id :time)))

;(defn lines->timeline [lines]
;  (loop [lines lines ticks [] tick {:id 0 :time 0}]
;    (if (seq lines)
;      (let [line (first lines)]
;        (if-let [new-tick (maybe-tick line)]
;          (if (empty-tick? tick)
;            (recur (rest lines) ticks new-tick)
;            (recur (rest lines) (conj ticks tick) new-tick))
;          (recur (rest lines) ticks (parse-line line tick))))
;      (if (empty-tick? tick)
;        ticks
;        (conj ticks tick)))))

(defn split-tick-lines [lines]
  (if (seq lines)
    (loop [before [(first lines)] after (rest lines)]
      (let [line (first after)]
        (if (or (nil? line) (tick? line))
          [before after]
          (recur (conj* before line) (rest after)))))
    [nil nil]))

(def new-tick {:id 0 :time 0})

(defn lines->timeline [lines]
  (let [[tick-lines remaining-lines] (split-tick-lines lines)]
    (if (seq tick-lines)
      (let [tick (reduce #(parse-line %2 %1) new-tick tick-lines)]
        (if (empty-tick? tick)
          (lines->timeline remaining-lines)
          (lazy-seq (cons tick (lines->timeline remaining-lines)))))
      ())))
