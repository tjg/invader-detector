(ns industries.tjg.invader-detector.image
  (:require
   [industries.tjg.invader-detector.utils :as utils])
  (:import
   (java.awt AlphaComposite BasicStroke Color Font RenderingHints)
   (java.awt.image BufferedImage)
   (java.io File)
   (javax.imageio ImageIO)))

(defn copy-image
  "Copy a BufferedImage."
  [^BufferedImage img]
  (let [copy (BufferedImage. (.getWidth img) (.getHeight img) (.getType img))
        gfx (.createGraphics copy)]
    (.drawImage gfx img 0 0 nil)
    (.dispose gfx)
    copy))

(defn draw-grid
  "Draw pixel grid. Returns a BufferedImage."
  ([radar-image]
   (draw-grid radar-image {}))
  ([radar-image
    {:keys [cell-width cell-height]
     :or {cell-width 10 cell-height 20}}]
   (let [[cols rows] (utils/size radar-image)
         img  (BufferedImage. (* cols cell-width)
                              (* rows cell-height)
                              BufferedImage/TYPE_INT_RGB)
         gfx  (.createGraphics img)]

     (.setColor gfx Color/BLACK)
     (.fillRect gfx 0 0 (.getWidth img) (.getHeight img))

     (doseq [row (range rows)
             col (range cols)]
       (when-not (zero? (get-in radar-image [row col]))
         (.setColor gfx Color/WHITE)
         (.fillRect gfx (* col cell-width) (* row cell-height) cell-width cell-height)))

     (.dispose gfx)
     img)))

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

(defn draw-bounding-boxes
  "Draw bounding box onto BufferedImage. Returns the BufferedImage."
  [^BufferedImage img bounding-boxes
   {:keys [cell-width cell-height font-name font-size]
    :or {cell-width 10 cell-height 20 font-name "Monospaced" font-size 12}}]
  (let [gfx (.createGraphics img)]
    (doseq [{:keys [x y width height color alpha label]}
            bounding-boxes]
      (let [[x y width height] [(* x cell-width)
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

    (.dispose gfx)
    img))

(defn save-image!
  "Save png image to file."
  [^BufferedImage img ^String output-file]
  (ImageIO/write img "png" (File. output-file)))
