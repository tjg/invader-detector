(ns industries.tjg.invader-detector.run
  "Run entire pipeline of sources, processors and sinks."
  (:require
   [clojure.string :as str]
   [industries.tjg.invader-detector.algorithms.pixel-hit-or-miss :as match]
   [industries.tjg.invader-detector.emit :as emit]
   [industries.tjg.invader-detector.image :as image]
   [industries.tjg.invader-detector.parse :as parse]
   [industries.tjg.invader-detector.utils :as utils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utils

(defn- parse-radar-sample [file {:keys [input-lenient-parsing
                                        input-on-chars
                                        input-off-chars]}]
  (let [opts (merge {:chars-true input-on-chars
                     :chars-false input-off-chars}
                    (when input-lenient-parsing
                      {:pad-lines? true, :chars-false-by-default? true}))]
    (parse/parse-radar-sample-from-file file opts)))

(defn- draw-ascii [{:keys [radar matches
                           output-ascii-on-char output-ascii-off-char
                           output-ascii-opaque-fill]}]
  (let [draw-opts (merge {:label-offset {:x 1, :y 0}}
                         (when output-ascii-on-char
                           {:char-true output-ascii-on-char})
                         (when output-ascii-off-char
                           {:char-false output-ascii-off-char})
                         (when output-ascii-opaque-fill
                           {:transparent-fill? false}))]
    (-> (emit/draw-pixel-matrix radar draw-opts)
        (emit/draw-scoreboxes matches draw-opts))))

(defn- format-matches
  [{:keys [matches]}]
  (->> matches
       (map #(select-keys % [:invader-id :bbox :score]))
       (sort-by :score)
       reverse
       (map (fn [scorebox]
              (update scorebox :score #(* 100.0 %))))
       utils/format-edn))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Sources, processes and sinks

(defmulti source
  "Gets input. Updates `_opts` with the key specified by `kwd`; returns it."
  (fn [kwd _opts] kwd))

(defmulti process
  "Processes data. Updates `_opts` with the key specified by `kwd`; returns it."
  (fn [kwd _opts] kwd))

(defmulti sink
  "Stores data. Updates `_opts` with the key specified by `kwd`; returns it."
  (fn [kwd _opts] kwd))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Sources

(defmethod source :invaders
  [kwd {:keys [invader-files] :as opts}]
  (assoc opts kwd
         (->> invader-files
              (map #(parse-radar-sample % opts))
              (map-indexed (fn [id invader]
                             {:invader-id id
                              :pixel-matrix invader})))))

(defmethod source :radar
  ([kwd {:keys [radar-sample-file] :as opts}]
   (assoc opts kwd
          (parse-radar-sample radar-sample-file opts))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Processors

(defmethod process :matches
  [kwd {:keys [invaders radar score-threshold max-results] :as opts}]
  (assoc opts kwd
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

(defmethod sink :save-ascii-sink
  [kwd {:keys [save-ascii] :as opts}]
  (-> opts
      draw-ascii
      (emit/save-to-file! save-ascii opts))
  (assoc opts kwd nil))

(defmethod sink :print-ascii-sink
  [kwd opts]
  (-> opts
      draw-ascii
      (emit/print! opts))
  (assoc opts kwd nil))

(defmethod sink :string-ascii-sink
  [kwd opts]
  (assoc opts kwd (emit/to-string (draw-ascii opts))))

(defmethod sink :save-images-sink
  [kwd {:keys [invaders radar matches
             save-images invader-colors]
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
    (doseq [filename save-images]
      (let [image-format (-> filename
                             (str/split #"\.")
                             last)]
        (image/save-to-file! img filename
                             {:image-format image-format})))
    (assoc opts kwd nil)))

(defmethod sink :save-matches-sink
  [kwd {:keys [save-matches] :as opts}]
  (spit save-matches (format-matches opts))
  (assoc opts kwd nil))

(defmethod sink :print-matches-sink
  [kwd opts]
  (println (format-matches opts))
  (assoc opts kwd nil))

(defmethod sink :string-matches-sink
  [kwd opts]
  (assoc opts kwd
         (format-matches opts)))

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
    (get opts :save-matches)   (sink :save-matches-sink)
    (get opts :print-matches)  (sink :print-matches-sink)
    (get opts :string-matches) (sink :string-matches-sink)
    (get opts :save-ascii)     (sink :save-ascii-sink)
    (get opts :print-ascii)    (sink :print-ascii-sink)
    (get opts :string-ascii)   (sink :string-ascii-sink)
    (get opts :save-images)    (sink :save-images-sink)))
