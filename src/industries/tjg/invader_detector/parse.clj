(ns industries.tjg.invader-detector.parse
  (:require
   [clojure.string :as str]))

(defn parse-radar-data [image {:keys [chars-true chars-false] :as _opts}]
  (->> image
       str/split-lines
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
      (parse-radar-data {:chars-true [\o \O] :chars-false [\-]}))
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
