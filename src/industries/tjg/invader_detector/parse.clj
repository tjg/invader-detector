(ns industries.tjg.invader-detector.parse
  "Read in ASCII."
  (:require
   [clojure.string :as str]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utils

(defn- pad-vectors [pad-val vecs]
  (let [max-size (->> vecs
                     (map count)
                     (reduce max 0))]
    (->> vecs
         (mapv (fn [v]
                 (if (= max-size (count v))
                   v
                   (let [padded-vector (concat v (repeat pad-val))]
                     (->> padded-vector
                          (take max-size)
                          vec))))))))

^:rct/test
(comment
  (pad-vectors 0 [[] [1] [1 1] [1 1 1]])
  ;; =>
  [[0 0 0] [1 0 0] [1 1 0] [1 1 1]])

(defn- parse-char [ch chars-true chars-false
                   chars-false-by-default? line-idx char-idx]
  (cond (chars-true ch)
        1
        (or (chars-false ch)
            chars-false-by-default?)
        0
        :else
        (throw (ex-info
                "Unknown character"
                {:character ch
                 :line-idx line-idx
                 :char-idx char-idx}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(def default-opts
  "Strict parsing that accepts both upper & lowercase `o` characters."
  {:chars-true [\o \O]
   :chars-false [\-]
   :pad-lines? false
   :chars-false-by-default false})

(defn parse-radar-sample
  "Parse unicode radar data into a 2D vector denoting true/false pixels.

  Each line of radar data is separated by newlines.

  `opts` can contain the following keys:
  - `:chars-true` & `:chars-false`: contains characters denoting true or
     false pixels, respectively.
  - `:pad-lines?`: if true, pad lines with false pixels so they're all
     equal lengths.

  An exception is thrown if `s` contains an unknown character."
  [s {:keys [chars-true chars-false pad-lines? chars-false-by-default?]
      :or {chars-true [\o \O] chars-false [\-]}
      :as _opts}]
  (let [chars-true (set chars-true)
        chars-false (set chars-false)
        possibly-pad-vectors (fn [vs]
                               (if pad-lines?
                                 (pad-vectors 0 vs)
                                 vs))]
    (->> s
         str/split-lines
         (map-indexed
          (fn [line-idx line]
            (->> line
                 (map-indexed (fn [char-idx ch]
                                (parse-char ch chars-true chars-false
                                            chars-false-by-default?
                                            line-idx char-idx)))
                 vec)))
         possibly-pad-vectors
         vec)))

^:rct/test
(comment
  ;; Simple parser.
  (-> "---oO\noOo-"
      (parse-radar-sample {:chars-true [\o \O] :chars-false [\-]}))
  ;; =>
  [[0 0 0 1 1]
   [1 1 1 0  ]]

  ;; Lenient parser.
  (-> "---oO\n??"
      (parse-radar-sample {:chars-true [\o \O] :chars-false [\-]
                           :pad-lines? true :chars-false-by-default? true}))
  ;; =>
  [[0 0 0 1 1]
   [0 0 0 0 0]])

(defn parse-radar-sample-from-file
  "Parse unicode radar data into a 2D vector denoting true/false pixels.

  Each line of radar data is separated by newlines.

  `opts` can contain the following keys:
  - `:chars-true` & `:chars-false`: contains characters denoting true or
     false pixels, respectively.
  - `:pad-lines?`: if true, pad lines with false pixels so they're all
     equal lengths.

  An exception is thrown if the file contains an unknown character."
  ([file]
   (parse-radar-sample-from-file file {}))
  ([file opts]
   (-> file
       slurp
       (parse-radar-sample opts))))
