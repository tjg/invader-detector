(ns industries.tjg.invader-detector.algorithms.pixel-hit-or-miss
  "Algorithm to find similarities between two pixel-matrices.

  It positions the invader's image all around the radar sample, then
  scoring the similarity. The simlarity score is pixels matched,
  divided by number of invader's pixels.

  This is currently a O(N²) algorithm. Some possibilities if
  performance is needed: CPU cache-friendly datastructures, bit-vector
  comparisons, pre-processing, and judicious parallelism."
  (:require
   [industries.tjg.invader-detector.utils :as utils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utils

(defn- similarity-at-offset [a b {:keys [offset] :as _opts}]
  (let [{:keys [a-start a-size b-start overlap-size]}
        (utils/bounding-box-intersection a b offset)

        [ax0 ay0] [(:x a-start) (:y a-start)]
        [bx0 by0] [(:x b-start) (:y b-start)]
        {:keys [width height]} overlap-size
        points-to-sample (utils/cartesian-product (range width) (range height))

        match-image
        (->> points-to-sample
             (map (fn [[x y]]
                    ;; Compare counterpart values in both pixel-matrices.
                    (= (get-in a [(+ y ay0) (+ x ax0)])
                       (get-in b [(+ y by0) (+ x bx0)]))))
             ;; Encode true pixels as 1's; false as 0's.
             (map #(if % 1 0)))]
    {:overlap-dimensions {:width (max 0 width), :height (max 0 height)}
     :invader-pixel-count (* (:x a-size) (:y a-size))
     :match-image (partition width match-image)
     :matched-pixel-count (->> match-image (filter #(= % 1)) count)}))

^:rct/test
(comment
  ;; Invader: a 1 on the lower right.
  (def ^:private test-invader
    [[0 0]
     [0 1]])

  ;; Radar sample: a square of 1's on the upper right.
  (def ^:private test-radar-sample
    [[1 1 0]
     [1 1 0]
     [0 0 0]])

  ;; No offset. So both pixel-matrices are compared starting at the
  ;; upper-left corner. The invader overlaps fully on the radar.
  (similarity-at-offset test-invader test-radar-sample {:offset [0 0]})
  ;; =>
  {:overlap-dimensions {:width 2 :height 2} ;; 2x2 square, same as invader.
   :match-image [[0 0]     ;; Only lower right pixel matched.
                 [0 1]]
   :invader-pixel-count 4
   :matched-pixel-count 1} ;; 1 out of 4 pixels matched.

  ;; Now let's move the invader 1px ↖ (up & left): that is, offset [-1,-1].
  ;; So the overlap will be a 1x1 square, not 2x2.
  (similarity-at-offset test-invader test-radar-sample {:offset [-1 -1]})
  ;; =>
  {:overlap-dimensions {:width 1 :height 1}
   :match-image [[1]]      ;; The overlapped pixel matched.
   :invader-pixel-count 4  ;; 1 overlapped pixel + 3 outside the radar.
   :matched-pixel-count 1}

  ;; Now let's move the invader 3px ↖ (up & left), so definitely no overlap.
  (similarity-at-offset test-invader test-radar-sample {:offset [-3 -3]})
  ;; =>
  {:overlap-dimensions {:width 0 :height 0} ;; No overlap.
   :match-image []         ;; No overlap.
   :invader-pixel-count 4  ;; All 4 invader's pixels are outside radar sample.
   :matched-pixel-count 0}

  )

(defn- averaging-score
  [{:keys [overlap-dimensions invader-pixel-count matched-pixel-count]
    :as _similarity}]
  (let [{:keys [width height]} overlap-dimensions
        overlap-pixel-count (* width height)
        unmatched-pixel-count (- invader-pixel-count overlap-pixel-count)]

    ;; Divide invader's matched pixels by its total pixels.
    (/ (+ matched-pixel-count
          ;; If there's parts we can't see (because they're outside the
          ;; radar sample), score those hidden pixels as 50% probability.
          (* 1/2 unmatched-pixel-count))
       invader-pixel-count)))

^:rct/test
(comment
  (averaging-score
   {:overlap-dimensions {:width 2 :height 2}
    :match-image [[0 0]     ;; 4 pixels overlap; only 1 matches.
                  [0 1]]
    :invader-pixel-count 4
    :matched-pixel-count 1})
  ;; =>
  1/4 ;; 1 pixels matched, out of 4 invader pixels.

  (averaging-score
   {:overlap-dimensions {:width 1 :height 1}
    :match-image [[1]]       ;; 1 overlaps (and matches): scored as 1 match.
    :invader-pixel-count 4   ;; 3 don't overlap: each scored as 1/2 match.
    :matched-pixel-count 1})
  ;; =>
  5/8 ;; (1 + 1/2 + 1/2 + 1/2) / 4 = (2 + 1 + 1 + 1) / 8 = 5/8

  (averaging-score
   {:overlap-dimensions {:width 0 :height 0}
    :invader-pixel-count 4
    :matched-pixel-count 0
    :match-image []})
  ;; =>
  1/2 ;; All hidden pixels. Lacking further info, we score the whole thing 50%.

  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn matches
  "Match `invader` to `radar-sample`.

  Returns a sequence of score-boxes, each of which looks like:
  {:score 5/8
   :bbox {:x -1, :y -1, :width 1, :height 1}}"
  [invader radar-sample]
  ;; Place `invader` at all positions in and around `radar-sample`,
  ;; as long as there's the slightest bit of overlap. Calculate the
  ;; similarity score at each position.
  (let [[invader-height invader-width] (utils/size invader)
        [radar-height radar-width]     (utils/size radar-sample)

        ;; Position `invader` initially so its lowest-right pixel just
        ;; barely overlaps with `radar-sample`'s upper-left pixel.
        [x0 y0] [(- 1 invader-height) (- 1 invader-width)]]
    (->> (utils/cartesian-product (range x0 radar-height)
                                  (range y0 radar-width))
         (map (fn [[x y]]
                (let [similarity (similarity-at-offset
                                  invader radar-sample {:offset [x y]})]
                  {:bbox (merge {:x x :y y} (:overlap-dimensions similarity))
                   :score (averaging-score similarity)}))))))
