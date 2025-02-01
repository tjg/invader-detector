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

(defn- parse-radar-sample [file {:keys [input-lenient-parsing]}]
  (parse/parse-radar-sample-from-file
   file
   (if input-lenient-parsing
     {:pad-lines? true, :chars-false-by-default true}
     {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Sources, processes and sinks

(defmulti source
  "Gets input for later computations. Associates `kwd` to opts, with the input."
  (fn [kwd _opts] kwd))

(defmulti process
  "Computes inputs. Associates `kwd` to opts, with the computation's results."
  (fn [kwd _opts] kwd))

(defmulti sink
  "Stores data. Optionally associates `kwd` to opts, with any value."
  (fn [kwd _opts] kwd))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Sources

(defmethod source :invaders
  [_ {:keys [invader-files] :as opts}]
  (assoc opts :invaders
         (->> invader-files
              (map #(parse-radar-sample % opts))
              (map-indexed (fn [id invader]
                             {:invader-id id
                              :pixel-matrix invader})))))

(defmethod source :radar
  ([_ {:keys [radar-sample-file] :as opts}]
   (assoc opts :radar
          (parse-radar-sample radar-sample-file opts))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Processors

(defmethod process :matches
  [_ {:keys [invaders radar score-threshold max-results] :as opts}]
  (assoc opts :matches
         (cond->> invaders
           true        (mapcat
                        (fn [{:keys [invader-id pixel-matrix]}]
                          (->> (match/matches pixel-matrix radar)
                               (map #(assoc % :invader-id invader-id)))))
           true        (sort-by :score)
           max-results (take-last max-results)
           true        (filter (fn [{:keys [score]}]
                                 (>= score
                                     (/ score-threshold 100)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Sinks

(defmethod sink :ascii
  [_ {:keys [radar matches output-ascii
             output-on-char output-off-char output-opaque-fill]
      :as opts}]
  (let [draw-opts (merge {:label-offset {:x 1, :y 0}}
                         (when output-on-char
                           {:char-true output-on-char})
                         (when output-off-char
                           {:char-false output-off-char})
                         (when output-opaque-fill
                           {:transparent-fill? false}))]
    (-> (emit/draw-pixel-matrix radar draw-opts)
        (emit/draw-scoreboxes matches draw-opts)
        (emit/save-to-file! output-ascii {})))
  opts)

(defmethod sink :images
  [_ {:keys [invaders radar matches
             output-images invader-colors]
      :as opts}]
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
                             {:image-format image-format})))
    opts))

(defmethod sink :save-matches
  [_ {:keys [save-matches matches]
      :as opts}]
  (let [edn (->> matches
                 (map #(select-keys % [:invader-id :bbox :score]))
                 utils/format-edn)]
    (spit save-matches edn))
  opts)

(defmethod sink :print-matches
  [_ {:keys [matches] :as opts}]
  (->> matches
       (map #(select-keys % [:invader-id :bbox :score]))
       utils/format-edn
       print)
  opts)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(def available-image-formats
  "List of supported image types.

  A set of strings that correspond to common filename extensions."
  (set (image/supported-image-formats)))

(defn locate-invaders!
  "Execute commandline program that locates invaders in radar."
  [opts]
  (cond->> opts
    true (source  :invaders)
    true (source  :radar)
    true (process :matches)
    (get opts :output-ascii)  (sink :ascii)
    (get opts :output-images) (sink :images)
    (get opts :save-matches)  (sink :save-matches)
    (get opts :print-matches) (sink :print-matches)))
