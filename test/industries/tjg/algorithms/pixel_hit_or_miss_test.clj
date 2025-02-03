(ns industries.tjg.algorithms.pixel-hit-or-miss-test
  {:clj-kondo/ignore [:refer :unresolved-symbol]}
  (:require
   [clojure.data :as data]
   [expectations.clojure.test
    :refer [defexpect expect expecting from-each more->]]
   [industries.tjg.invader-detector.algorithms.pixel-hit-or-miss :as sut]
   [industries.tjg.invader-detector.datastructures :as ds]
   [malli.core :as m]))

(defexpect matches-with-high-scores
  (let [matches (sut/matches [[0 0]
                              [0 0]]
                             [[0 0 0]
                              [0 0 0]])]
    (expecting "numMatches = (aSizeX + bSizeX - 1) * (aSizeY + bSizeY - 1)"
      (expect (* (+ 2 3 -1)
                 (+ 2 2 -1))
              (count matches)))
    (expecting "uniform score-box keys"
      (expect #(m/validate ds/ScoreBox %)
              (from-each [m matches]
                m)))
    (expecting "scores all higher than 50%, because all overlaps are matches"
      (expect #(> % 0.5)
              (from-each [match matches]
                (:score match))))
    (expecting "perfect scores where they fully overlap"
      (expect (more-> 1
                      :score

                      {:width 2 :height 2}
                      (-> :bbox (select-keys [:width :height])))
              (from-each [match (->> matches
                                     (filter #(= (:score %) 1)))]
                match)))
    (expecting "2 perfect scores"
      (expect 2
              (->> matches
                   (filter #(= (:score %) 1))
                   count)))))

(defexpect matches-with-low-scores
  (let [matches (sut/matches [[0 0]
                              [0 0]]
                             [[1 1]
                              [1 1]
                              [1 1]
                              [1 1]])]
    (expecting "numMatches = (aSizeX + bSizeX - 1) * (aSizeY + bSizeY - 1)"
      (expect (* (+ 2 2 -1)
                 (+ 2 4 -1))
              (count matches)))
    (expecting "scores all less than 50%, because all overlaps are non-matches"
      (expect #(< % 0.5)
              (from-each [match matches]
                (:score match))))
    (expecting "in zero-scores, they fully overlap"
      (expect (more-> 0
                      :score

                      {:width 2 :height 2}
                      (-> :bbox (select-keys [:width :height])))
              (from-each [match (->> matches
                                     (filter #(zero? (:score %))))]
                match))
      (expecting "three zero-scores"
        (expect 3
                (->> matches
                     (filter #(zero? (:score %)))
                     count))))))

;; Testing private fn `sut/similarity-at-offset`.
;; Feel free to delete this code as internals change.
(defexpect similarity-reversal
  (let [small-pixel-matrix [[0 0 0]
                            [0 1 1]
                            [0 1 1]]
        big-pixel-matrix [[1 1 0 0]
                          [1 1 0 0]
                          [0 0 0 0]
                          [0 0 0 0]]]
    (expecting "switching similarity params only impacts `:invader-pixel-count`"
      (expect (more-> {:invader-pixel-count 9} ;; From `small-pixel-matrix`.
                      first

                      {:invader-pixel-count 16} ;; From `big-pixel-matrix`.
                      second

                      {:overlap-dimensions {:width 3 :height 3}
                       :matched-pixel-count 3
                       :match-image [[0 0 1]
                                     [0 1 0]
                                     [1 0 0]]}
                      (nth 2))
              (data/diff (#'sut/similarity-at-offset
                          small-pixel-matrix big-pixel-matrix ;; smaller, bigger
                          {:offset [0 0]})

                         (#'sut/similarity-at-offset
                          big-pixel-matrix small-pixel-matrix ;; bigger, smaller
                          {:offset [0 0]}))))))
