(ns industries.tjg.invader-detector.image
  (:require
   [clojure.string :as str])
  (:import
   (java.awt Color Font)
   (java.awt.image BufferedImage)
   (java.io File)
   (javax.imageio ImageIO)))

(defn draw-text [image-string output-path]
  (let [ascii-lines (str/split-lines image-string)
        char-width  10
        char-height 15
        img-width   (* char-width (apply max (map count ascii-lines)))
        img-height  (* char-height (count ascii-lines))
        img         (BufferedImage. img-width img-height BufferedImage/TYPE_INT_RGB)
        g           (.createGraphics img)]

    ;; Set background color
    (.setColor g Color/WHITE)
    (.fillRect g 0 0 img-width img-height)

    ;; Set font and color
    (.setFont g (Font. "Monospaced" Font/PLAIN 12))
    (.setColor g Color/BLACK)

    ;; Draw each character
    (doseq [[y line] (map-indexed vector ascii-lines)
            [x char] (map-indexed vector line)]
      (.drawString g (str char) (* x char-width) (* (inc y) char-height)))

    ;; Dispose of graphics context and save the image
    (.dispose g)
    (ImageIO/write img "png" (File. output-path))))

(defn draw-grid [radar-image {:keys [cell-width cell-height] :or {} :as _cell-size} output-file]
  (let [rows (count radar-image)
        cols (count (first radar-image))
        img  (BufferedImage. (* cols cell-width) (* rows cell-height) BufferedImage/TYPE_INT_RGB)
        g    (.createGraphics img)]

    ;; Set background to black
    (.setColor g Color/BLACK)
    (.fillRect g 0 0 (.getWidth img) (.getHeight img))

    ;; Draw white rectangles for 1s
    (doseq [row (range rows)
            col (range cols)]
      (when-not (zero? (get-in radar-image [row col]))
        (.setColor g Color/WHITE)
        (.fillRect g (* col cell-width) (* row cell-height) cell-width cell-height)))

    ;; Save image
    (ImageIO/write img "png" (File. output-file))

    (.dispose g)))
