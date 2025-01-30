(ns industries.tjg.invader-detector.image
  (:require
   [clojure.string :as str]
   [industries.tjg.invader-detector.utils :as utils])
  (:import
   (java.awt AlphaComposite BasicStroke Color Font RenderingHints)
   (java.awt.image BufferedImage)
   (java.io File)
   (javax.imageio ImageIO)))

(defn copy-image
  "Copy a BufferedImage."
  [img]
  (let [copy (BufferedImage. (.getWidth img) (.getHeight img) (.getType img))
        gfx (.createGraphics copy)]
    (.drawImage gfx img 0 0 nil)
    (.dispose gfx)
    copy))

(defn draw-ascii-text
  "Draw monospaced ASCII text. Returns a BufferedImage."
  [image-string output-path]
  (let [ascii-lines (str/split-lines image-string)
        char-width  10
        char-height 15
        img-width   (* char-width (apply max (map count ascii-lines)))
        img-height  (* char-height (count ascii-lines))
        img         (BufferedImage. img-width img-height BufferedImage/TYPE_INT_RGB)
        gfx         (.createGraphics img)]

    (.setColor gfx Color/WHITE)
    (.fillRect gfx 0 0 img-width img-height)

    (.setFont gfx (Font. "Monospaced" Font/PLAIN 12))
    (.setColor gfx Color/BLACK)

    (doseq [[y line] (map-indexed vector ascii-lines)
            [x char] (map-indexed vector line)]
      (.drawString gfx (str char) (* x char-width) (* (inc y) char-height)))

    (.dispose gfx)
    img))

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

(defn- calculate-text-frame-color
  "A dark color similar to the bounding box's color, for white text on
  it to stand out."
  [{:keys [r g b]}]
  {:r (utils/round (/ r 2))
   :g (utils/round (/ g 2))
   :b (utils/round (/ b 2))})

(defn- draw-bounding-box-label
  "Readably draws a label on a bounding box.

  Designed to be readable regardless of what's underneath it, though
  it may be obscured by any bounding boxes and labels on top of it."
  [gfx label font-name font-size bounding-box-color bounding-box-x bounding-box-y]
  (let [x (max bounding-box-x 0)
        y (max bounding-box-y 0)
        {:keys [r g b]} (calculate-text-frame-color bounding-box-color)

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
    (.setColor      gfx (Color. r g b))
    (.fillRoundRect gfx x y rect-width rect-height 10 10)

    ;; Draw text.
    (.setFont    gfx font)
    (.setColor   gfx Color/white)
    (.drawString gfx label text-x text-y)))

(defn draw-bounding-boxes
  [img bounding-boxes
   {:keys [cell-width cell-height font-name font-size]
    :or {cell-width 10 cell-height 20 font-name "Monospaced" font-size 12}}]
  (let [gfx (.createGraphics img)]
    (doseq [{:keys [x y width height color alpha label]}
            bounding-boxes]
      (let [{:keys [r g b]} color
            [x y width height] [(* x cell-width)
                                (* y cell-height)
                                (* width cell-width)
                                (* height cell-height)]]
        (.setRenderingHint
         gfx RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)

        ;; Fill bounding box.
        (.setComposite gfx (AlphaComposite/getInstance AlphaComposite/SRC_OVER alpha))
        (.setColor     gfx (Color. r g b))
        (.fillRect     gfx x y width height)

        ;; Bounding box outline.
        (.setComposite gfx (AlphaComposite/getInstance AlphaComposite/SRC_OVER 1))
        (.setStroke    gfx (BasicStroke. 2))
        (.drawRect     gfx x y width height)

        (draw-bounding-box-label gfx label font-name font-size color x y)))

    (.dispose gfx)
    img))

(defn save-image! [img output-file]
  (ImageIO/write img "png" (File. output-file)))
