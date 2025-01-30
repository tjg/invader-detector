(ns industries.tjg.invader-detector.debug
  (:require
   [industries.tjg.invader-detector.algorithms.pixel-hit-or-miss :as match]
   [industries.tjg.invader-detector.image :as image]
   [industries.tjg.invader-detector.parse :as parse]))

(defn- matches [invader radar]
  (->> (match/matches invader radar)
       (filter #(<= 0.7 (:score %)))
       (sort-by :score)))

(defn- make-invader-location-image! []
  (let [invader-1 (parse/parse-radar-sample-from-file "resources/spec-invader-1.txt")
        invader-2 (parse/parse-radar-sample-from-file "resources/spec-invader-2.txt")
        radar     (parse/parse-radar-sample-from-file "resources/spec-radar-sample.txt")
        grid      (image/draw-grid radar)

        matches-1 (->> (matches invader-1 radar)
                       (map #(assoc % :color {:r 67 :g 0 :b 255})))
        matches-2 (->> (matches invader-2 radar)
                       (map #(assoc % :color {:r 68 :g 242 :b 13})))

        all-matches (->> (concat matches-1 matches-2)
                         (sort-by :score))]
    (image/draw-scoreboxes grid all-matches {})
    (image/save-image! grid "resources/blah.png")))

(comment
  (make-invader-location-image!)

  )
