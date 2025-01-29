(ns industries.tjg.invader-detector.image
  (:require
   [clojure.string :as str]
   [industries.tjg.invader-detector.utils :as utils])
  (:import
   (java.awt AlphaComposite BasicStroke Color Font RenderingHints)
   (java.awt.image BufferedImage)
   (java.io File)
   (javax.imageio ImageIO)))

(defn copy-image [img]
  (let [copy (BufferedImage. (.getWidth img) (.getHeight img) (.getType img))
        gfx (.createGraphics copy)]
    (.drawImage gfx img 0 0 nil)
    (.dispose gfx)
    copy))

(defn draw-ascii-text [image-string output-path]
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
  ([radar-image]
   (draw-grid radar-image {}))
  ([radar-image
    {:keys [cell-width cell-height]
     :or {cell-width 10 cell-height 20}}]
   (let [rows (count radar-image)
         cols (count (first radar-image))
         img  (BufferedImage. (* cols cell-width) (* rows cell-height) BufferedImage/TYPE_INT_RGB)
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

(defn calculate-text-frame-color
  "A darker color for white text to stand out."
  [{:keys [r g b]}]
  {:r (utils/round (/ r 2))
   :g (utils/round (/ g 2))
   :b (utils/round (/ b 2))})

(defn draw-text-rect [gfx font text text-frame-color padding x y]
  (let [x (max x 0)
        y (max y 0)
        metrics (.getFontMetrics gfx font)
        text-width (.stringWidth metrics text)
        text-height (.getHeight metrics)
        text-ascent (.getAscent metrics)
        rect-width (+ text-width (* 2 padding))
        rect-height (+ text-height (* 2 padding))
        text-x (+ x padding)
        text-y (+ y text-ascent padding)
        {:keys [r g b]} text-frame-color]
    (.setColor gfx (Color. r g b))
    (.fillRoundRect gfx x y rect-width rect-height 10 10)
    (.setFont gfx font)
    (.setColor gfx Color/white)
    (.drawString gfx text text-x text-y)))

(defn draw-bounding-boxes
  [img bounding-boxes
   {:keys [cell-width cell-height background-color line-color font-name font-size]
    :or {cell-width 10 cell-height 20 font-name "Monospaced" font-size 12}}]
  (let [gfx (.createGraphics img)]
    (doseq [{:keys [x y width height color text-frame-color alpha score]} bounding-boxes]
      (let [{:keys [r g b]} color
            [x y width height] [(* x cell-width)
                                (* y cell-height)
                                (* width cell-width)
                                (* height cell-height)]
            font (Font. font-name Font/PLAIN font-size)
            score-text (str score "%")
            text-padding 2]
        (.setRenderingHint gfx RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)

        (.setComposite gfx (AlphaComposite/getInstance AlphaComposite/SRC_OVER alpha))
        (.setColor gfx (Color. r g b))
        (.fillRect gfx x y width height)

        (.setComposite gfx (AlphaComposite/getInstance AlphaComposite/SRC_OVER 1))
        (.setStroke gfx (BasicStroke. 2))
        (.drawRect gfx x y width height)

        (draw-text-rect gfx font score-text text-frame-color text-padding x y)))

    (.dispose gfx)
    img))

(defn save-image! [img output-file]
  (ImageIO/write img "png" (File. output-file)))
