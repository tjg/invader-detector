(ns industries.tjg.invader-detector.run
  (:require
   [clojure.string :as str]
   [industries.tjg.invader-detector.algorithms.pixel-hit-or-miss :as match]
   [industries.tjg.invader-detector.emit :as emit]
   [industries.tjg.invader-detector.image :as image]
   [industries.tjg.invader-detector.parse :as parse]
   [industries.tjg.invader-detector.utils :as utils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utils

(defn- get-matches [invaders radar
                    {:keys [score-threshold max-results]}]
  (cond->> invaders
    true        (mapcat
                 (fn [{:keys [invader-id pixel-matrix]}]
                   (->> (match/matches pixel-matrix radar)
                        (map #(assoc % :invader-id invader-id)))))
    true        (sort-by :score)
    max-results (take-last max-results)
    true        (filter (fn [{:keys [score]}]
                          (>= score
                              (/ score-threshold 100))))))

(defn- draw-ascii! [radar matches {:keys [output-ascii
                                          output-on-char
                                          output-off-char
                                          output-opaque-fill]}]
  (let [draw-opts (merge {:label-offset {:x 1, :y 0}}
                         (when output-on-char
                           {:char-true output-on-char})
                         (when output-off-char
                           {:char-false output-off-char})
                         (when output-opaque-fill
                           {:transparent-fill? false}))]
    (-> (emit/draw-pixel-matrix radar draw-opts)
        (emit/draw-scoreboxes matches draw-opts)
        (emit/save-to-file! output-ascii {}))))

(defn- draw-images! [invaders radar matches {:keys [output-images invader-colors]}]
  (let [invader-color-table (->> [(->> invaders
                                       (map :invader-id))
                                  (cycle invader-colors)]
                                 (apply map vector)
                                 (into {}))
        colored-matches (->> matches
                             (map (fn [{:keys [invader-id] :as match}]
                                    (assoc match
                                           :color (invader-color-table invader-id)))))
        img (-> (image/draw-pixel-matrix radar {})
                (image/draw-scoreboxes colored-matches {}))]
    (doseq [filename output-images]
      (let [image-format (-> filename
                             (str/split #"\.")
                             last)]
        (image/save-to-file! img filename
                             {:image-format image-format})))))

(defn- save-matches-to-file! [matches {:keys [save-matches]}]
  (let [edn (->> matches
                 (map #(select-keys % [:invader-id :bbox :score]))
                 utils/format-edn)]
    (spit save-matches edn)))

(defn- parse-radar-sample [file {:keys [input-lenient-parsing]}]
  (parse/parse-radar-sample-from-file
   file
   (if input-lenient-parsing
     {:pad-lines? true, :chars-false-by-default true}
     {})))

(defn- print-matches! [matches _opts]
  (->> matches
       (map #(select-keys % [:invader-id :bbox :score]))
       utils/format-edn
       print))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

;; FIXME: validate extensions
(def available-image-formats
  "List of supported image types.

  A set of strings that correspond to common filename extensions."
  (set (image/supported-image-formats)))

(defn locate-invaders!
  "Execute commandline program that locates invaders in radar."
  [{:keys [invader-files radar-sample-file
           print-matches save-matches
           output-images
           output-ascii]
    :as opts}]

  (let [ ;; Parse input files.
        invaders (->> invader-files
                      (map #(parse-radar-sample % opts))
                      (map-indexed (fn [id invader]
                                     {:invader-id id
                                      :pixel-matrix invader})))
        radar (parse-radar-sample radar-sample-file opts)

        ;; Matches.
        matches (get-matches invaders radar opts)]

    ;; Output to ascii.
    (when output-ascii
      (draw-ascii! radar matches opts))

    ;; Output to images.
    (when output-images
      (draw-images! invaders radar matches opts))

    (when save-matches
      (save-matches-to-file! matches opts))

    (when print-matches
      (print-matches! matches opts))))
