(ns industries.tjg.invader-detector.run
  (:require
   [clojure.string :as str]
   [industries.tjg.invader-detector.algorithms.pixel-hit-or-miss :as match]
   [industries.tjg.invader-detector.emit :as emit]
   [industries.tjg.invader-detector.image :as image]
   [industries.tjg.invader-detector.parse :as parse]
   [industries.tjg.invader-detector.utils :as utils]
   [me.raynes.fs :as fs]))

;; FIXME: validate extensions
(def available-image-formats
  "List of supported image types.

  A set of strings that correspond to common filename extensions."
  (set (image/supported-image-formats)))

(defn locate-invaders!
  "Execute commandline program that locates invaders in radar."
  [{:keys [invader-files radar-sample-file
           input-lenient-parsing
           score-threshold max-results
           print-matches save-matches
           output-images
           output-ascii
           output-on-char output-off-char]
    :or {score-threshold 70}}]

  (let [ ;; Parse input files.
        invaders (->> invader-files
                      (map parse/parse-radar-sample-from-file))
        radar (parse/parse-radar-sample-from-file
               radar-sample-file
               (if input-lenient-parsing
                 {:pad-lines? true, :chars-false-by-default true}
                 {}))

        ;; Matches.
        matches (cond->> invaders
                  true (mapcat
                        (fn [invader]
                          (->> (match/matches invader radar)
                               (map #(assoc % :color {:r 67 :g 0 :b 255})))))
                  true (sort-by :score)
                  max-results (take-last max-results)
                  true (filter (fn [{:keys [score]}]
                                 (>= score
                                     (/ score-threshold 100)))))]

    ;; Output to ascii.
    (when output-ascii
      (let [draw-opts (merge {:label-offset {:x 1, :y 0}}
                             (when output-on-char
                               {:char-true output-on-char})
                             (when output-off-char
                               {:char-false output-off-char}))]
        (-> (emit/draw-pixel-matrix radar draw-opts)
            (emit/draw-scoreboxes matches draw-opts)
            (emit/save-to-file! output-ascii {}))))

    ;; Output to images.
    (when output-images
      (let [img (-> (image/draw-pixel-matrix radar {})
                    (image/draw-scoreboxes matches {}))]
        (doseq [filename output-images]
          (let [image-format (-> filename fs/extension str/lower-case)]
            (image/save-to-file! img filename
                                 {:image-format image-format})))))

    (when save-matches
      (let [edn (->> matches
                     (map #(select-keys % [:bbox :score]))
                     utils/format-edn)]
        (spit save-matches edn)))

    (when print-matches
      (->> matches
           (map #(select-keys % [:bbox :score]))
           utils/format-edn
           print))))
