(ns industries.tjg.invader-detector.datastructures
  (:require [malli.core :as malli]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utils

(defn- equal-length-rows? [matrix]
  (let [lengths (set (map count matrix))]
    (<= (count lengths) 1)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(def BBox
  "A boundary box."
  [:map
   [:x number?]
   [:y number?]
   [:width number?]
   [:height number?]])

(def ScoreBox
  "A boundary box, with a matching score."
  [:map
   [:score number?]
   [:bbox #'BBox]])

^:rct/test
(comment
  (malli/validate ScoreBox
                  {:score 1/3
                   :bbox {:x 0 :y 0 :width 10 :height 10}}) ;; => true
  )

(def PixelMatrix
  "A 2D vector containing pixels represented as numbers or characters.

  For example, characters may represent pixels during rendering stages."
  [:and
   [:vector [:vector [:or number? char?]]]
   [:fn equal-length-rows?]])

^:rct/test
(comment
  (malli/validate PixelMatrix [[0 0]
                               [0 0]]) ;; => true
  (malli/validate PixelMatrix [[\a]])  ;; => true
  ;; Rows not equal length.
  (malli/validate PixelMatrix [[0 0]
                               [0  ]]) ;; => false

)
