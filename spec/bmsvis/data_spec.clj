(ns bmsvis.data-spec
  (:require [speclj.core :refer :all]
            [bmsvis.data :refer :all]
            [clojure.java.io :as io]))

(describe "Data"

  (it "empty file"
    (let [ticks (log->timeline (io/file "data/empty.log"))]
      (should= 0 (count ticks))))

  (it "3 ticks"
    (let [ticks (lines->timeline ["info: setup"
                                  "tick: 1, 2111"
                                  "info: 1"
                                  "tick: 2, 2361"
                                  "info: 2"
                                  "tick: 3, 2611"
                                  "info: 3"])]
      (should= 4 (count ticks))
      (should= [0, 1, 2, 3] (map :id ticks))
      (should= [0, 2111, 2361, 2611] (map :time ticks))))

  (it "parses info"
    (let [ticks (lines->timeline ["info: setup"
                                  "info: more"
                                  "tick: 1, 2111"
                                  "info: hello there"])]
      (should= 2 (count ticks))
      (should= ["setup" "more"] (:info (first ticks)))
      (should= ["hello there"] (:info (last ticks)))))

  (it "parses error"
    (let [ticks (lines->timeline ["info: setup"
                                  "error: oops!"])]
      (should= ["oops!"] (:error (first ticks)))))

  (it "parses alert"
    (let [ticks (lines->timeline ["info: setup"
                                  "alert: oops!"])]
      (should= ["oops!"] (:alert (first ticks)))))

  (it "parses pack line"
    (let [ticks (lines->timeline ["info: setup"
                                  "pack: 34.098,26.96186,0"])
          tick (first ticks)]
      (should= 34.098 (:batt_v tick) 0.001)
      (should= 26.96186 (:pack_v tick) 0.001)
      (should= 0.0 (:amps tick) 0.001)))

  (it "parses temps line"
    (let [ticks (lines->timeline ["info: setup"
                                  "temps: 32.74458,33.46426,25.26466"])
          tick (first ticks)]
      (should= 32.74458 (:temp1 tick) 0.001)
      (should= 33.46426 (:temp2 tick) 0.001)
      (should= 25.26466 (:temp3 tick) 0.001)))

  (it "parses cells line"
    (let [ticks (lines->timeline ["info: setup"
                                  "cells: 3.406709,3.410856,3.409725,3.405578,3.410856,3.413118,3.408217,3.41161,3.412741,3.407463"])
          tick (first ticks)]
      (should= 3.406709 (nth (:cells tick) 0) 0.00001)
      (should= 3.410856 (nth (:cells tick) 1) 0.00001)
      (should= 3.409725 (nth (:cells tick) 2) 0.00001)
      (should= 3.405578 (nth (:cells tick) 3) 0.00001)
      (should= 3.410856 (nth (:cells tick) 4) 0.00001)
      (should= 3.413118 (nth (:cells tick) 5) 0.00001)
      (should= 3.408217 (nth (:cells tick) 6) 0.00001)
      (should= 3.41161 (nth (:cells tick) 7) 0.00001)
      (should= 3.412741 (nth (:cells tick) 8) 0.00001)
      (should= 3.407463 (nth (:cells tick) 9) 0.00001)))


  (it "unlabled lines go in error"
    (let [ticks (lines->timeline ["info: setup"
                                  "yo"])]
      (should= ["yo"] (:error (first ticks)))))

  (it "simple data"
    (let [timeline (log->timeline (io/file "data/simple.log"))]
      (should= 62 (count timeline))
      )
    )

  )