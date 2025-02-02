(ns industries.tjg.invader-detector.emit
  (:require
   [clojure.string :as str]
   [industries.tjg.invader-detector.utils :as utils]))

(def default-draw-opts
  "Simple ASCII text frame."
  {:char-true            \o
   :char-false           \-
   :transparent-fill?    true
   :label-offset         {:x 1 :y 1}
   :corner-x0-char       \╭
   :corner-xN-char       \╮
   :corner-y0-char       \╰
   :corner-yN-char       \╯
   :vertical-side-char   \│
   :horizontal-side-char \─
   :inner-bbox-char      \space})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utils

;; Notation:
;; - `0`     is the pixel matrix's min x-index.
;; - `x`     is relative to `pixel-matrix`'s origin.
;; - `x0`    is `x`'s min value when drawing a rectangle.
;; - `xN`    is `x`'s max value when drawing a rectangle.
;; - `xSize` is the pixel matrix's width.

(defn- vertical-side? [[x _] [x0 _] [xN _]]
  (or (= x x0)
      (= x xN)))

(defn- horizontal-side? [[_ y] [_ y0] [_ yN]]
  (or (= y y0)
      (= y yN)))

(defn- draw-label-char? [label [x y] [x0 y0] [_ yN] label-offset]
  (and
   ;; `x` is in the label's column.
   (<= (+ x0 (:x label-offset))
       x
       (dec (+ x0
               (:x label-offset)
               (count label))))
   ;; `y` is in the label's row.
   (or
    (= y
       (+ y0 (:y label-offset)))
    ;; Box straddles radar upper edge? Draw label on that edge.
    (and (< y0 0)
         (<= 0 yN)
         (= y 0)))))

(defn- char-to-draw [[x y] [x0 y0] [xN yN]
                     label
                     {:keys [corner-x0-char
                             corner-xN-char
                             corner-y0-char
                             corner-yN-char
                             vertical-side-char
                             horizontal-side-char
                             inner-bbox-char
                             label-offset
                             transparent-fill?
                             ::pixel-matrix-background]}]
  (cond
    ;; Is this a label? Print its character.
    (draw-label-char? label [x y] [x0 y0] [xN yN] label-offset)
    (get label (- x x0 (:x label-offset)))

    ;; Corners.
    (= [x y] [x0 y0]) corner-x0-char
    (= [x y] [xN y0]) corner-xN-char
    (= [x y] [x0 yN]) corner-y0-char
    (= [x y] [xN yN]) corner-yN-char

    ;; Sides.
    (vertical-side? [x y] [x0 y0] [xN yN])
    vertical-side-char
    (horizontal-side? [x y] [x0 y0] [xN yN])
    horizontal-side-char

    :else ;; Inside the box.
    (if (and transparent-fill? pixel-matrix-background)
      ;; If transparent & we have underlying matrix, print it.
      (get-in pixel-matrix-background [y x])
      ;; Print a default character.
      inner-bbox-char)))

(defn- draw-scorebox [pixel-matrix {:keys [bbox score]} opts]
  (let [opts (merge {::pixel-matrix-background pixel-matrix}
                    default-draw-opts
                    opts)
        label (utils/format-score-as-percent score)

        {:keys [width height]} bbox
        [x0 y0] [(:x bbox) (:y bbox)]
        [xSize ySize] (utils/size pixel-matrix)

        ;; Widen frame to surround the bounding box.
        [x0 y0] [(dec x0) (dec y0)]
        [width height] [(+ 2 width) (+ 2 height)]
        [xN yN] [(dec (+ x0 width))
                 (dec (+ y0 height))]

        points-to-draw (utils/cartesian-product
                        (range x0 (+ x0 width))
                        (range y0 (+ y0 height)))]
    (->> points-to-draw
         ;; Don't draw outside pixel-matrix bounds.
         (filter (fn [[x y]]
                   (and (<= 0 x)
                        (<= 0 y)
                        (< x xSize)
                        (< y ySize))))
         ;; Draw the scorebox on the pixel matrix.
         (reduce (fn [pixel-matrix [x y]]
                   (assoc-in pixel-matrix [y x]
                             (char-to-draw [x y] [x0 y0] [xN yN]
                                           label
                                           opts)))
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
                       [\X \X \X \X \X \X \X \X \X \X]]
                      {:score 1/10 ;; Label will say `10%`.
                       :bbox {:x 2 :y 2 :width 6 :height 4}}
                      {:label-offset {:x 2, :y 0} ;; Score label position.
                       :transparent-fill? false   ;; Enable `:inner-bbox-char`.
                       :inner-bbox-char \space    ;; Blank spaces inside box.
                       })
       ;; Format as string (with a newline at the beginning for readability).
       (map #(apply str %))
       (str/join \newline)
       (str \newline))
  ;; =>
  "
XXXXXXXXXX
X╭─10%──╮X
X│      │X
X│      │X
X│      │X
X│      │X
X╰──────╯X
XXXXXXXXXX
XXXXXXXXXX")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn draw-pixel-matrix
  "Format pixel matrix as unicode text.

  True & false pixels are represented as `:char-true` & `:char-false`
  from `opts`, respectively. Defaults in `default-draw-opts`."
  [pixel-matrix opts]
  (let [{:keys [char-true char-false]} (merge default-draw-opts opts)
        char-pixel-matrix
        (->> pixel-matrix
             (mapv (fn [row]
                     (->> row
                          (mapv (fn [pixel]
                                  (if (zero? pixel) char-false char-true)))))))]
    (with-meta char-pixel-matrix
      ;; Remember this original background to draw transparent windows later.
      {::pixel-matrix-background char-pixel-matrix})))

^:rct/test
(comment
  (draw-pixel-matrix [[1 1 0]
                      [1 0 0]]
                     {:char-true  \█
                      :char-false \-})
  ;; =>
  [[\█ \█ \-]
   [\█ \- \-]]
  )

(defn draw-scoreboxes
  "Draw `scoreboxes` onto `pixel-matrix`. Returns updated `pixel-matrix`.

  `opts` enables additional config. See `default-draw-opts`.

  Note: `draw-pixel-matrix` adds metadata necessary to draw
  transparent scoreboxes. So transparent scoreboxes may not work
  properly if that metadata's missing."
  [pixel-matrix scoreboxes opts]
  (let [orig-meta (meta pixel-matrix)
        opts (merge orig-meta opts)
        char-pixel-matrix (->> scoreboxes
                               (reduce (fn [pixel-matrix scorebox]
                                         (draw-scorebox pixel-matrix scorebox opts))
                                       pixel-matrix))]
    (with-meta char-pixel-matrix
      orig-meta)))

(defn to-string
  "ASCII."
  [pixel-matrix]
  (->> pixel-matrix
       (map #(apply str %))
       (str/join \newline)))

(defn save-to-file!
  "Save ASCII to file."
  [pixel-matrix output-file _opts]
  (spit output-file (to-string pixel-matrix)))

(defn print!
  "Print ASCII."
  [pixel-matrix _opts]
  (print (to-string pixel-matrix)))
