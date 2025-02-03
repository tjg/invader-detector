(ns industries.tjg.invader-detector.datastructures
  (:require [malli.core :as malli]))

(def BBox
  [:map
   [:x number?]
   [:y number?]
   [:width number?]
   [:height number?]])

(def ScoreBox
  [:map
   [:score number?]
   [:bbox #'BBox]])

(defn- equal-length-rows? [matrix]
  (let [lengths (set (map count matrix))]
    (<= (count lengths) 1)))

(def PixelMatrix
  [:and
   [:vector [:vector number?]]
   [:fn equal-length-rows?]])
