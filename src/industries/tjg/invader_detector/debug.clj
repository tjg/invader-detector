(ns industries.tjg.invader-detector.debug
  (:require
   [industries.tjg.invader-detector.format :as format]
   [industries.tjg.invader-detector.image :as image]
   [industries.tjg.invader-detector.match :as match]
   [industries.tjg.invader-detector.parse :as parse]))

(comment
  (-> "resources/spec-invader-1.txt"
      slurp
      (parse/parse-radar-data {:chars-true [\o \O] :chars-false [\-]})
      (format/format-image)
      (image/draw-ascii-text "resources/spec-invader-1.png"))

(-> "resources/spec-invader-2.txt"
      slurp
      (parse/parse-radar-data {:chars-true [\o \O] :chars-false [\-]})
      (image/draw-grid "resources/spec-invader-2.png"))

  (-> "resources/spec-invader-1.txt"
      slurp
      (parse/parse-radar-data {:chars-true [\o \O] :chars-false [\-]})
      (image/draw-grid "resources/spec-invader-1-1.png"))

  (-> "resources/spec-radar-sample.txt"
      slurp
      (parse/parse-radar-data {:chars-true [\o \O] :chars-false [\-]})
      (image/draw-grid "resources/spec-radar-sample.png"))

  )

(comment

  (let [invader (-> "resources/spec-invader-1.txt"
                    slurp
                    (parse/parse-radar-data {:chars-true [\o \O] :chars-false [\-]}))
        radar (-> "resources/spec-radar-sample.txt"
                  slurp
                  (parse/parse-radar-data {:chars-true [\o \O] :chars-false [\-]}))]
    (->> (match/matches invader radar)
         (filter (fn [{:keys [::match/averaging-score]}]
                   (> averaging-score 0.7)))
         (sort-by ::match/averaging-score)
         reverse
         clojure.pprint/pprint))

  (require '[criterium.core :as c])
  (c/bench (let [invader (-> "resources/spec-invader-1.txt"
                             slurp
                             (parse/parse-radar-data {:chars-true [\o \O] :chars-false [\-]}))
                 radar (-> "resources/spec-radar-sample.txt"
                           slurp
                           (parse/parse-radar-data {:chars-true [\o \O] :chars-false [\-]}))]
             (->> (match/matches invader radar)
                  (filter (fn [{:keys [::match/averaging-score]}]
                            (> averaging-score 0.7)))
                  (sort-by ::match/averaging-score)
                  reverse)))

  )

(defn parse-file [file]
  (-> file
      slurp
      (parse/parse-radar-data {:chars-true [\o \O] :chars-false [\-]})))

(defn bounding-box [{:keys [match color text-frame-color score]}]
  (let [{:keys [effective-sizes
                ::match/radar-offset
                ::match/averaging-score]} match]
    (let [[x y] radar-offset
          [width height] effective-sizes]
      {:x x :y y :width width :height height
       :color color
       :text-frame-color text-frame-color
       :alpha (* 0.5 score)
       :score (Math/round (* 100 score))})))

(defn matches [invader radar]
  (->> (match/matches invader radar)
       (filter (fn [{:keys [::match/averaging-score]}]
                 (> averaging-score 0.7)))
       (sort-by ::match/averaging-score)))

(defn invader-location-image []
  (let [invader-1 (parse-file "resources/spec-invader-1.txt")
        invader-2 (parse-file "resources/spec-invader-2.txt")
        radar (parse-file "resources/spec-radar-sample.txt")
        grid (image/draw-grid radar)

        matches-1 (->> (matches invader-1 radar)
                       (map (fn [match]
                              (let [color {:r 67 :g 0 :b 255}
                                    text-frame-color (image/calculate-text-frame-color color)]
                                {:match match
                                 :color color
                                 :text-frame-color text-frame-color
                                 :score (::match/averaging-score match)}))))
        matches-2 (->> (matches invader-2 radar)
                       (map (fn [match]
                              (let [color {:r 68 :g 242 :b 13}
                                    text-frame-color (image/calculate-text-frame-color color)]
                                {:match match
                                 :text-frame-color text-frame-color
                                 :color color
                                 :score (::match/averaging-score match)}))))

        all-matches (->> (concat matches-1 matches-2)
                         (sort-by :score))]
    (image/draw-bounding-boxes grid (map bounding-box all-matches) {})
    (image/save-image! grid "resources/blah.png")))

(comment
  (invader-location-image)

  )
