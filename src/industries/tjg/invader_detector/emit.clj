(ns industries.tjg.invader-detector.emit
  (:require
   [clojure.string :as str]
   [industries.tjg.invader-detector.utils :as utils]))

(def default-draw-opts
  "Simple ASCII text frame."
  {:char-true            \o
   :char-false           \-
   :transparent-bbox?    true
   :corner-x0-char       \╭
   :corner-xN-char       \╮
   :corner-y0-char       \╰
   :corner-yN-char       \╯
   :vertical-side-char   \│
   :horizontal-side-char \─
   :inner-bbox-char      \space})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utils

(defn- vertical-side? [[x _] [x0 _] [xN _]]
  (or (= x x0)
      (= x xN)))

(defn- horizontal-side? [[_ y] [_ y0] [_ yN]]
  (or (= y y0)
      (= y yN)))

(defn- label-pos? [label [x y] [x0 y0] [_ _]]
  (let [label-x-pos (<= (inc x0)
                        x
                        (+ x0 (count label)))]
    (or
     (and label-x-pos
          (< y0 0)
          (= y 0))
     (and label-x-pos
          (= 1 (- y y0))))))

(defn- draw-scorebox [pixel-matrix {:keys [bbox score]} opts]
  (let [{:keys [corner-x0-char
                corner-xN-char
                corner-y0-char
                corner-yN-char
                vertical-side-char
                horizontal-side-char
                inner-bbox-char
                transparent-bbox?
                pixel-matrix-0
                char-true
                char-false]}
        (merge default-draw-opts opts)

        label (utils/format-score-as-percent score)
        {:keys [width height]} bbox
        [x0 y0] [(:x bbox) (:y bbox)]
        [xMax yMax] (utils/size pixel-matrix)

        ;; Make frame surround bounding box.
        [x0 y0] [(dec x0) (dec y0)]
        [width height] [(+ 2 width) (+ 2 height)]

        xs (range width)
        ys (range height)
        xN (dec (+ x0 width))
        yN (dec (+ y0 height))
        ;; Don't draw label if bbox is too small.
        label-in-bbox-bounds? (and (> (dec width) (count label))
                                   (> height 1))
        points-to-draw (for [x xs, y ys]
                         ;; Cartesian product: all points in rectangle.
                         [(+ x x0) (+ y y0)])]
    (->> points-to-draw
         ;; Don't draw outside pixel-matrix bounds.
         (filter (fn [[x y]]
                   (and (<= 0 x)
                        (<= 0 y)
                        (< x xMax)
                        (< y yMax))))
         ;; Draw here.
         (reduce (fn [canvas [x y]]
                   (let [char-to-draw
                         (cond
                           (and label-in-bbox-bounds?
                                (label-pos? label [x y] [x0 y0] [xN yN]))
                           (get label (dec (- x x0)))

                           (= [x y] [x0 y0]) corner-x0-char
                           (= [x y] [xN y0]) corner-xN-char
                           (= [x y] [x0 yN]) corner-y0-char
                           (= [x y] [xN yN]) corner-yN-char
                           (vertical-side? [x y] [x0 y0] [xN yN])
                           vertical-side-char
                           (horizontal-side? [x y] [x0 y0] [xN yN])
                           horizontal-side-char

                           :else
                           (if (and transparent-bbox? pixel-matrix-0)
                             ;; If has original matrix.
                             (get-in pixel-matrix-0 [y x])
                             inner-bbox-char))]

                     (assoc-in canvas [y x] char-to-draw)))
                 pixel-matrix))))

^:rct/test
(comment
  (->> (draw-scorebox [[\X \X \X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X \X \X]]
                      {:score 1/10
                       :bbox {:x 2 :y 2 :width 6 :height 4}}
                      {:transparent-bbox? false
                       :inner-bbox-char \space})
       (map #(apply str %))
       (str/join \newline))
  ;; =>
"XXXXXXXXXX
X╭──────╮X
X│10%   │X
X│      │X
X│      │X
X│      │X
X╰──────╯X
XXXXXXXXXX
XXXXXXXXXX
XXXXXXXXXX"
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn draw-pixel-matrix
  "Format radar sample as unicode text.

  True & false pixels are emitted as `:char-true` & `:char-false` from
  `opts`, respectively. Defaults are specified in `default-draw-opts`."
  ([pixel-matrix]
   (draw-pixel-matrix pixel-matrix {}))
  ([pixel-matrix opts]
   (let [{:keys [char-true char-false]} (merge default-draw-opts opts)]
     (->> pixel-matrix
          (mapv (fn [row]
                  (->> row
                       (mapv (fn [pixel]
                               (if (zero? pixel) char-false char-true))))))))))

^:rct/test
(comment
  (draw-pixel-matrix [[0 1 0]
                      [1 0 0]]
                     {:char-true \█
                      :char-false \space})
  ;; => [[\space \█ \space] [\█ \space \space]]
  )

(defn draw-scoreboxes
  "Draw `scoreboxes` onto `pixel-matrix`. Returns updated `pixel-matrix`.

  `opts` enables additional config. See `default-draw-opts`."
  [pixel-matrix scoreboxes opts]
  (let [opts (merge {:pixel-matrix-0 pixel-matrix}  ;; Useful for transparency.
                    opts)]
    (->> scoreboxes
         (reduce (fn [pixel-matrix scorebox]
                   (draw-scorebox pixel-matrix scorebox opts))
                 pixel-matrix))))

(defn save-to-file!
  "Save ASCII to file."
  [pixel-matrix output-file]
  (let [ascii-text (->> pixel-matrix
                        (map #(apply str %))
                        (str/join \newline))]
    (spit output-file ascii-text)))
