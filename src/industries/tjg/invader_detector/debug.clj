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
      (image/draw-text "resources/spec-invader-1.png"))

  (-> "resources/spec-invader-1.txt"
      slurp
      (parse/parse-radar-data {:chars-true [\o \O] :chars-false [\-]})
      (image/draw-grid {:cell-width 10 :cell-height 20} "resources/spec-invader-1-1.png"))

  (-> "resources/spec-radar-sample.txt"
      slurp
      (parse/parse-radar-data {:chars-true [\o \O] :chars-false [\-]})
      (image/draw-grid {:cell-width 10 :cell-height 20} "resources/spec-radar-sample.png"))

  )

(comment

  (let [invader (-> "resources/spec-invader-1.txt"
                    slurp
                    (parse/parse-radar-data {:chars-true [\o \O] :chars-false [\-]}))
        radar (-> "resources/spec-radar-sample.txt"
                  slurp
                  (parse/parse-radar-data {:chars-true [\o \O] :chars-false [\-]}))]
    (->> (match/matches invader radar)
         (filter (fn [{:keys [::averaging-score]}]
                   (> averaging-score 0.7)))
         (sort-by ::averaging-score)
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
                  (filter (fn [{:keys [::averaging-score]}]
                            (> averaging-score 0.7)))
                  (sort-by ::averaging-score)
                  reverse
                  #_clojure.pprint/pprint)))

  )
