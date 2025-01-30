(ns industries.tjg.invader-detector.parse
  (:require
   [clojure.string :as str]))

(defn parse-radar-sample
  "Parse unicode radar data into a 2D vector denoting true/false pixels.

  Each line of radar data is separated by newlines.

  `chars-true` & `chars-false` contain characters denoting true or
  false pixels. An exception is thrown if there's an unknown
  character."
  [s {:keys [chars-true chars-false] :as _opts}]
  (->> s
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

(defn parse-radar-sample-from-file
  "Parse unicode radar data into a 2D vector denoting true/false pixels.

  Each line of radar data is separated by newlines.

  `chars-true` & `chars-false` contain characters denoting true or
  false pixels. An exception is thrown if there's an unknown
  character."
  [file]
  (-> file
      slurp
      (parse-radar-sample {:chars-true [\o \O] :chars-false [\-]})))

^:rct/test
(comment
  (-> "--o-O\noOo-"
      (parse-radar-sample {:chars-true [\o \O] :chars-false [\-]}))
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
