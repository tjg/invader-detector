(ns industries.tjg.invader-detector.emit
  (:require
   [clojure.string :as str]
   [industries.tjg.invader-detector.utils :as utils]))

(def default-draw-opts
  "Simple ASCII text frame."
  {:char-true            \o
   :char-false           \-
   :corner-x0-char       \┌
   :corner-xN-char       \┐
   :corner-y0-char       \└
   :corner-yN-char       \┘
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
  (and (= 1 (- y y0))
       (<= (inc x0) x (+ x0 (count label)))))

(defn- draw-scorebox [pixel-matrix {:keys [bbox score]} opts]
  (let [{:keys [corner-x0-char
                corner-xN-char
                corner-y0-char
                corner-yN-char
                vertical-side-char
                horizontal-side-char
                inner-bbox-char]}
        (merge default-draw-opts opts)

        label (utils/format-score-as-percent score)
        {:keys [width height]} bbox
        [x0 y0] [(:x bbox) (:y bbox)]

        xs (range width)
        ys (range height)
        xN (dec (+ x0 width))
        yN (dec (+ y0 height))
        ;; Don't draw label if bbox is too small.
        label-in-bbox-bounds? (and (> (dec width) (count label))
                                   (> height 1))
        cartesian-product (for [x xs, y ys]
                            [(+ x x0) (+ y y0)])]
    (->> cartesian-product
         (reduce (fn [canvas [x y]]
                   (let [char-to-draw
                         (cond
                           (and label-in-bbox-bounds?
                                (label-pos? label [x y] [x0 y0] [xN yN]))
                           (get label (dec (- x x0)))

                           (= [x y] [x0 y0])
                           corner-x0-char

                           (= [x y] [xN y0])
                           corner-xN-char

                           (= [x y] [x0 yN])
                           corner-y0-char

                           (= [x y] [xN yN])
                           corner-yN-char

                           (vertical-side? [x y] [x0 y0] [xN yN])
                           vertical-side-char

                           (horizontal-side? [x y] [x0 y0] [xN yN])
                           horizontal-side-char

                           :else inner-bbox-char)]

                     (assoc-in canvas [y x] char-to-draw)))
                 pixel-matrix))))

^:rct/test
(comment
  (->> (draw-scorebox [[\X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X]
                       [\X \X \X \X \X \X \X \X]]
                      {:score 1/10
                       :bbox {:x 1 :y 1 :width 6 :height 4}}
                      {})
       (map #(apply str %))
       (str/join \newline))
  ;; =>
"XXXXXXXX
X┌────┐X
X│10% │X
X│    │X
X└────┘X
XXXXXXXX
XXXXXXXX
XXXXXXXX"
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
          (map (fn [row]
                 (->> row
                      (map (fn [pixel]
                             (if (zero? pixel) char-false char-true)))
                      (apply str))))
          (str/join \newline)))))

^:rct/test
(comment
  (draw-pixel-matrix [[0 1 0]
                      [1 0 0]]
                     {:char-true \█
                      :char-false \space})
  ;; => " █ \n█  "
  )

(defn draw-scoreboxes
  "Draw `scoreboxes` onto `pixel-matrix`. Returns updated `pixel-matrix`.

  `opts` enables additional config. See `default-draw-opts`."
  [pixel-matrix scoreboxes opts]
  (->> scoreboxes
       (reduce (fn [pixel-matrix scorebox]
                 (draw-scorebox pixel-matrix scorebox opts))
               pixel-matrix)))

(defn save-to-file!
  "Save ASCII to file."
  [pixel-matrix output-file]
  (spit output-file (draw-pixel-matrix pixel-matrix)))
