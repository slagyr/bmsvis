(ns bmsvis.main-spec
  (:require [speclj.core]
            [bmsvis.data :as data]
            [bmsvis.main :as main]
            [bmsvis.charts :as charts]
            [bmsvis.sample-data :as sample]
            [clojure.string :as str])
  (:require-macros [speclj.core :refer [describe context it should= should-not= after before should-contain around with]]))


(describe "bmsvis Main"

  (it "calculates max zoom level"
    (should= 5 (main/max-zoom-level 1))
    (should= 5 (main/max-zoom-level 8))
    (should= 5 (main/max-zoom-level 9))
    (should= 5 (main/max-zoom-level 16))
    (should= 5 (main/max-zoom-level 17))
    (should= 6 (main/max-zoom-level 33))
    (should= 7 (main/max-zoom-level 65))
    (should= 8 (main/max-zoom-level 129))
    (should= 9 (main/max-zoom-level 257))
    (should= 10 (main/max-zoom-level 513))
    (should= 11 (main/max-zoom-level 1025))
    (should= 12 (main/max-zoom-level 2049))
    (should= 13 (main/max-zoom-level 4097))
    (should= 14 (main/max-zoom-level 8193))
    (should= 15 (main/max-zoom-level 16385))
    (should= 16 (main/max-zoom-level 32769))
    (should= 17 (main/max-zoom-level 65537))
    (should= 18 (main/max-zoom-level 131073)))

  (it "converts zoom level to frame size"
    (should= 32 (main/zoom->tickn 1))
    (should= 32 (main/zoom->tickn 5))
    (should= 64 (main/zoom->tickn 6))
    (should= 128 (main/zoom->tickn 7))
    (should= 256 (main/zoom->tickn 8))
    (should= 512 (main/zoom->tickn 9)))

  (it "calibrates zoom 10"
    (let [zoom (main/calibrate-zoom {:tick-count 10})]
      (should= main/min-zoom (:zoom zoom))
      (should= 5 (:zoom-max zoom))))

  (it "calibrates zoom 1000"
    (let [zoom (main/calibrate-zoom {:tick-count 1000} )]
      (should= 10 (:zoom zoom))
      (should= 10 (:zoom-max zoom))))

  (it "calibrates pan 10"
    (let [state {:tick-count 10 :zoom 5 :zoom-max 5 :pan-start 0}
          pan (main/calibrate-pan state)]
      (should= 0 (:pan-start pan))
      (should= 32 (:pan-size pan))
      (should= 1 (:pan-nth pan))
      (should= 0 (:pan-max pan))
      (should= 8 (:pan-step pan))))

  (it "calibrates pan 1000 zoom 10"
    (let [state {:tick-count 1000 :zoom 10 :zoom-max 10 :pan-start 0 :pan-size 0 :pan-nth 0}
          pan (main/calibrate-pan state)]
      (should= 0 (:pan-start pan))
      (should= 1024 (:pan-size pan))
      (should= 1 (:pan-nth pan))
      (should= 0 (:pan-max pan))
      (should= 256 (:pan-step pan))))

  (it "calibrates pan 1000 zoom 7 starting at 0"
    (let [state {:tick-count 1000 :zoom 7 :zoom-max 10 :pan-start 0 :pan-size 0 :pan-nth 0}
          pan (main/calibrate-pan state)]
      (should= 0 (:pan-start pan))
      (should= 128 (:pan-size pan))
      (should= 1 (:pan-nth pan))
      (should= (- 1000 128) (:pan-max pan))
      (should= 32 (:pan-step pan))))

  (it "calibrates pan 1000 zoom 7 starting at 500"
    (let [state {:tick-count 1000 :zoom 7 :zoom-max 10 :pan-start 500 :pan-size 100 :pan-nth 0}
          pan (main/calibrate-pan state)]
      (should= 486 (:pan-start pan))
      (should= 128 (:pan-size pan))
      (should= 1 (:pan-nth pan))
      (should= (- 1000 128) (:pan-max pan))
      (should= 32 (:pan-step pan))))

  (it "calibrates pan 10000 zoom 13 starting at 500"
    (let [state {:tick-count 10000 :zoom 13 :zoom-max 14 :pan-start 500 :pan-size 100 :pan-nth 0}
          pan (main/calibrate-pan state)]
      (should= 0 (:pan-start pan))
      (should= 8192 (:pan-size pan))
      (should= 8 (:pan-nth pan))
      (should= (- 10000 8192) (:pan-max pan))
      (should= (/ 8192 4) (:pan-step pan))))

  )