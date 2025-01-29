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

    (.setColor g Color/WHITE)
    (.fillRect g 0 0 img-width img-height)

    (.setFont g (Font. "Monospaced" Font/PLAIN 12))
    (.setColor g Color/BLACK)

    (doseq [[y line] (map-indexed vector ascii-lines)
            [x char] (map-indexed vector line)]
      (.drawString g (str char) (* x char-width) (* (inc y) char-height)))

    (.dispose g)
    (ImageIO/write img "png" (File. output-path))))

(defn draw-grid
  ([radar-image output-file]
   (draw-grid radar-image {} output-file))
  ([radar-image
    {:keys [cell-width cell-height]
     :or {cell-width 10 cell-height 20}}
    output-file]
   (let [rows (count radar-image)
         cols (count (first radar-image))
         img  (BufferedImage. (* cols cell-width) (* rows cell-height) BufferedImage/TYPE_INT_RGB)
         g    (.createGraphics img)]

     (.setColor g Color/BLACK)
     (.fillRect g 0 0 (.getWidth img) (.getHeight img))

     (doseq [row (range rows)
             col (range cols)]
       (when-not (zero? (get-in radar-image [row col]))
         (.setColor g Color/WHITE)
         (.fillRect g (* col cell-width) (* row cell-height) cell-width cell-height)))

     (ImageIO/write img "png" (File. output-file))

     (.dispose g))))
