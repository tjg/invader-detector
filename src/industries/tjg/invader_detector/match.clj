(ns industries.tjg.invader-detector.match
  (:require
   [clojure.string :as str]))

(defn parse-image [image {:keys [chars-true chars-false] :as _opts}]
  (->> image
       (map-indexed (fn [line-idx line]
                      (->> line
                           (map-indexed (fn [char-idx c]
                                          (cond ((set chars-true) c)
                                                1
                                                ((set chars-false) c)
                                                0
                                                :else
                                                (throw (ex-info
                                                        "Unknown character"
                                                        {:character c
                                                         :line-idx line-idx
                                                         :char-idx char-idx})))))
                           vec)))
       vec))

^:rct/test
(comment
  (-> "--o-O\noOo-"
      str/split-lines
      (parse-image {:chars-true [\o \O] :chars-false [\-]}))
  ;; => [[0 0 1 0 1]
  ;;     [1 1 1 0  ]]

  ;; What characters exist in all sample files?
  (->> ["resources/spec-radar-sample.txt"
        "resources/spec-invader-1.txt"
        "resources/spec-invader-2.txt"]
       (map slurp)
       (apply str)
       frequencies) ;; => {\- 3995, \o 1156, \newline 66, \O 1}

  )

(defn similarity-score [a b]
  (when (not= [(-> a first count)
               (-> a count)]
              [(-> b first count)
               (-> b count)])
    (ex-info "Array sizes don't match." {:a a :b b}))
  (let [[size-x size-y] [(-> a first count)
                         (-> a count)]
        size-total (* size-x size-y)
        matches (for [y (range 0 size-y)
                      x (range 0 size-x)]
                  (= (get-in a [y x])
                     (get-in b [y x])))
        match-count (->> matches
                         (filter identity)
                         count)]
    (/ match-count size-total)))

^:rct/test
(comment
  (similarity-score
   [[1 0 0]]
   [[0 1 0]]) ;; => 1/3

  (similarity-score
   [[0 1 0]]
   [[0 1 0]])  ;; => 1

  )
