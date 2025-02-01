(ns industries.tjg.invader-detector.utils
  (:require
   [clojure.pprint :as pprint]))

(defn round
  "Rounds a number to the nearest integer, preserving types where possible."
  [n]
  (cond
    (double? n) (Math/round ^double n)
    (float? n)  (Math/round ^float n)
    (ratio? n)  (Math/round (double n))
    (integer? n) n
    (instance? java.math.BigDecimal n) (.setScale ^java.math.BigDecimal n 0
                                                  java.math.RoundingMode/HALF_UP)
    :else (Math/round (double n))))

(defn format-edn
  "Format data as EDN-encoded string."
  ([x]
   (format-edn x {}))
  ([x {:keys [pretty?] :or {pretty? true}}]
   (binding [*print-length* nil
             *print-level*  nil]
     (if pretty?
       (with-out-str
         (pprint/pprint x))
       (pr-str x)))))

(defn size
  "Calculates size of 2D vector as [size-x, size-y]."
  [a]
  [(-> a first count)
   (-> a count)])

(defn bounding-box-intersection
  "Calculate overlap of two bounding boxes.

  `a`'s position relative to `b` is translated by `_offset`. This may
  put `a` outside `b`'s bounds."
  [a b [x y :as _offset]]
  (let [[offset-x offset-y] [x y]

        ;; Actually measured from the datastructures, not calculated.
        [b-size-x b-size-y] (size b)
        [a-size-x a-size-y] (size a)

        ;; Zero; or if the offset's negative, shift.
        [a-start-x a-start-y] [(max 0 (- offset-x))
                               (max 0 (- offset-y))]
        ;; Size; or distance from b's start to its end.
        [a-end-x a-end-y] [(min a-size-x
                                (- b-size-x offset-x))
                           (min a-size-y
                                (- b-size-y offset-y))]

        ;; We've calculated a's bounds, using a's & b's dimensions. So
        ;; use it to calculate the effective size of both a & b.
        [effective-size-x effective-size-y] [(- a-end-x a-start-x)
                                             (- a-end-y a-start-y)]

        ;; b's offset, or 0 if it's negative.
        [b-start-x b-start-y] [(max 0 offset-x)
                               (max 0 offset-y)]]
    {:a-start {:x a-start-x :y a-start-y}
     :b-start {:x b-start-x :y b-start-y}
     :a-size {:x a-size-x :y a-size-y}
     :b-size {:x b-size-x :y b-size-y}
     :overlap-size {:width effective-size-x :height effective-size-y}}))

(defn format-score-as-percent
  "Format ratio as percentage."
  [score]
  (let [score-percent (round (* 100 score))]
    (str score-percent "%")))

(defn cartesian-product
  "All pairs [x,y] for each x ∈ xs and y ∈ ys."
  [xs ys]
  (for [x xs, y ys]
    [x y]))

(def hex-color-code-regex
  "Matches strings like `#aabbcc`."
  #"^#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})$")

(defn hex-to-rgb
  "Formats hex-encoded color string into {:keys [r g b]} map."
  [hex-color-code]
  (let [[_ r g b]
        (re-matches hex-color-code-regex hex-color-code)]
    {:r (Integer/parseInt r 16)
     :g (Integer/parseInt g 16)
     :b (Integer/parseInt b 16)}))

(defn rgb-to-hex
  "Formats {:keys [r g b]} map into hex-encoded color string."
  [{:keys [r g b]}]
  (format "#%02x%02x%02x" r g b))


^:rct/test
(comment
  (rgb-to-hex {:r 16 :g 0 :b 255})
  ;; =>
  "#1000ff"

  (-> {:r 16 :g 0 :b 255}
      rgb-to-hex
      hex-to-rgb)
  ;; =>
  {:r 16 :g 0 :b 255})  ;; Back to the original color.
