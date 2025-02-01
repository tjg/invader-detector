(ns industries.tjg.invader-detector.image
  (:require
   [industries.tjg.invader-detector.utils :as utils])
  (:import
   (java.awt AlphaComposite BasicStroke Color Font RenderingHints)
   (java.awt.image BufferedImage)
   (java.io File)
   (javax.imageio ImageIO)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utils

(defn- make-color
  ([{:keys [r g b]}]
   (make-color r g b))
  ([r g b]
   (Color. (int r) (int g) (int b))))

(defn- calculate-text-frame-color
  "Dark color based on a bounding box's color.

  Designed to make white text on it readable."
  [{:keys [r g b]}]
  (make-color (utils/round (/ r 2))
              (utils/round (/ g 2))
              (utils/round (/ b 2))))

(defn- draw-bounding-box-label
  "Readably draws a label on a bounding box.

  Designed to be readable regardless of what's underneath it, though
  it may be obscured by any bounding boxes and labels on top of it."
  [^java.awt.Graphics2D gfx
   ^String label
   font-name font-size bounding-box-color bounding-box-x bounding-box-y]
  (let [x (max bounding-box-x 0)
        y (max bounding-box-y 0)

        font (Font. font-name Font/PLAIN font-size)
        padding-around-text 2

        ;; Calculate frame dimensions.
        metrics     (.getFontMetrics gfx font)
        text-width  (.stringWidth metrics label)
        text-height (.getHeight metrics)
        text-ascent (.getAscent metrics)
        rect-width  (+ text-width  (* 2 padding-around-text))
        rect-height (+ text-height (* 2 padding-around-text))
        text-x      (+ x padding-around-text)
        text-y      (+ y text-ascent padding-around-text)]

    ;; Draw underlying frame to make label more readable.
    (.setColor      gfx (calculate-text-frame-color bounding-box-color))
    (.fillRoundRect gfx x y rect-width rect-height 10 10)

    ;; Draw text.
    (.setFont    gfx font)
    (.setColor   gfx Color/white)
    (.drawString gfx label (int text-x) (int text-y))))

(defn- draw-scorebox [^java.awt.Graphics2D gfx
                      {:keys [bbox score color]}
                      {:keys [cell-width cell-height font-name font-size]}]
  (let [{:keys [x y width height]} bbox

        alpha (* score 0.5)
        label (utils/format-score-as-percent score)
        [x y width height] [(* x cell-width)
                            (* y cell-height)
                            (* width cell-width)
                            (* height cell-height)]]
    (.setRenderingHint
     gfx RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)

    ;; Fill bounding box.
    (.setComposite gfx (AlphaComposite/getInstance AlphaComposite/SRC_OVER alpha))
    (.setColor     gfx (make-color color))
    (.fillRect     gfx x y width height)

    ;; Bounding box outline.
    (.setComposite gfx (AlphaComposite/getInstance AlphaComposite/SRC_OVER 1))
    (.setStroke    gfx (BasicStroke. 2))
    (.drawRect     gfx x y width height)

    (draw-bounding-box-label gfx label font-name font-size color x y)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(def default-draw-opts
  "This cell width & height is designed to resemble ASCII terminals."
  {:cell-width 10
   :cell-height 20
   :font-name "Monospaced"
   :font-size 12})

(defn draw-pixel-matrix
  "Draw pixel grid. Returns a BufferedImage.

  `opts` enables additional config. See `default-draw-opts`."
  [pixel-matrix opts]
  (let [{:keys [cell-width cell-height]} (merge default-draw-opts opts)
        [cols rows] (utils/size pixel-matrix)
        img  (BufferedImage. (* cols cell-width)
                             (* rows cell-height)
                             BufferedImage/TYPE_INT_RGB)
        gfx  (.createGraphics img)]

    (.setColor gfx Color/BLACK)
    (.fillRect gfx 0 0 (.getWidth img) (.getHeight img))

    (doseq [row (range rows)
            col (range cols)]
      (when-not (zero? (get-in pixel-matrix [row col]))
        (.setColor gfx Color/WHITE)
        (.fillRect gfx (* col cell-width) (* row cell-height) cell-width cell-height)))

    (.dispose gfx)
    img))

(defn draw-scoreboxes
  "Draw scoreboxes onto BufferedImage. Returns the modified BufferedImage.

  `opts` enables additional config. See `default-draw-opts`."
  [^BufferedImage img colored-scoreboxes opts]
  (let [gfx (.createGraphics img)]
    (doseq [colored-scorebox colored-scoreboxes]
      (draw-scorebox gfx colored-scorebox
                     (merge default-draw-opts opts)))

    (.dispose gfx)
    img))

(defn save-to-file!
  "Save image to file."
  [^BufferedImage img ^String output-file {:keys [image-format]}]
  (ImageIO/write img image-format (File. output-file))
  img)

(defn supported-image-formats
  "List of supported image types.

  Returns strings that correspond to common filename extensions."
  []
  (ImageIO/getReaderFormatNames))
