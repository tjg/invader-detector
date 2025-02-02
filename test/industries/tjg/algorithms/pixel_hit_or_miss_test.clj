(ns industries.tjg.algorithms.pixel-hit-or-miss-test
  {:clj-kondo/ignore [:refer :unresolved-symbol]}
  (:require
   [clojure.data :as data]
   [expectations.clojure.test :refer [defexpect expect expecting from-each]]
   [industries.tjg.invader-detector.algorithms.pixel-hit-or-miss :as sut]))

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
      (expect #{:bbox :score}
              (from-each [match matches]
                (-> match keys set))))
    (expecting "scores all higher than 50%, because all overlaps are matches"
      (expect #(> % 0.5)
              (from-each [match matches]
                (:score match))))
    (expecting "perfect scores where they fully overlap"
      (expect #{{:score 1, :bbox {:x 0 :y 0 :width 2 :height 2}}
                {:score 1, :bbox {:x 1 :y 0 :width 2 :height 2}}}
              (->> matches
                   (filter #(= (:score %) 1))
                   set)))))

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
    (expecting "0 scores where they fully overlap"
      (expect #{{:score 0, :bbox {:x 0 :y 0 :width 2 :height 2}}
                {:score 0, :bbox {:x 0 :y 1 :width 2 :height 2}}
                {:score 0, :bbox {:x 0 :y 2 :width 2 :height 2}}}
              (->> matches
                   (filter #(zero? (:score %)))
                   set)))))

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
      (expect [{:invader-pixel-count 9}  ;; From `small-pixel-matrix`.
               {:invader-pixel-count 16} ;; From `big-pixel-matrix`.

               ;; Same.
               {:overlap-dimensions {:width 3 :height 3}
                :matched-pixel-count 3
                :match-image [[0 0 1]
                              [0 1 0]
                              [1 0 0]]}]

              (data/diff (#'sut/similarity-at-offset
                          small-pixel-matrix big-pixel-matrix ;; smaller, bigger
                          {:offset [0 0]})

                         (#'sut/similarity-at-offset
                          big-pixel-matrix small-pixel-matrix ;; bigger, smaller
                          {:offset [0 0]}))))))
