(ns industries.tjg.invader-detector.cli.input
  "Parsers & validators for commandline input."
  (:require
   [clojure.string :as str]
   [industries.tjg.invader-detector.run :as run]
   [industries.tjg.invader-detector.utils :as utils]
   [malli.core :as malli]))

(defn kwd-to-switch
  "Given a keyword, convert it into the equivalent CLI switch."
  [kwd]
  (->> kwd name (format "--%s")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Schemas

(def color-schema
  "Map with red, green & blue elements."
  [:map
   [:r [:int {:min 0 :max 255}]]
   [:g [:int {:min 0 :max 255}]]
   [:b [:int {:min 0 :max 255}]]])

^:rct/test
(comment
  (malli/validate color-schema {:r 255 :g 0 :b 0}) ;; => true
  (malli/validate color-schema {:r 256 :g 0 :b 0}) ;; => false
  (malli/validate color-schema {:r 0 :g 0}) ;; => false
)

(def ^:private hex-encoded-color-schema
  [:re utils/hex-color-code-regex])

^:rct/test
(comment
  (malli/validate hex-encoded-color-schema "#aabbcc") ;; => true
  (malli/validate hex-encoded-color-schema "#abc")    ;; => false
  (malli/validate hex-encoded-color-schema "abc")     ;; => false
)

(def image-file-schema
  "File whose directory exists and whose extension is a supported image format."
  [:and
   string?
   [:fn
    {:error/message "File's directory doesn't exist."}
    (fn [s]
      (utils/directory-exists? s))]
   [:fn
    {:error/message "File's extension isn't a supported image format."}
    (fn [s]
      (->> run/available-image-formats
           (some (fn [image-fmt]
                   (clojure.string/ends-with? (str/lower-case s)
                                              (str "." image-fmt))))))]])

(def single-char-schema
  "A string with only a single character."
  [:and string?
   [:fn {:error/message "Must be a single character"}
    #(= 1 (count %))]])

^:rct/test
(comment
  (malli/validate single-char-schema ".")  ;; => true
  (malli/validate single-char-schema "")   ;; => false
  (malli/validate single-char-schema "12") ;; => false
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Formatters

(defn multiple-chars-formatter
  "Formats characters into a string, with commas separating them."
  [characters]
  (str/join "," characters))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Parsers

(defn multiple-chars-parser
  "Parses string with comma-separated elements."
  [s]
  (let [strs (str/split s #",")]
    (when-not (->> strs
                   (map count)
                   (every? #(= % 1)))
      (throw (ex-info "Contains an entry longer than a single char"
                      {:strs strs})))
    (->> strs
         (map first))))

(defn single-char-parser
  "Parses string into a character. The string must contain only a single character."
  [s]
  (if (malli/validate single-char-schema s)
    (first s)
    (throw (ex-info "Must be a single char" {:char s}))))

(defn colors-parser
  "Parses a string containing comma-separated hex-encoded colors, into an RGB map."
  [s]
  (let [strs (str/split s #",")]
    (if (malli/validate [:sequential {:min 1} hex-encoded-color-schema]
                        strs)
      (->> strs
           (map utils/hex-to-rgb))
      (throw (ex-info "Invalid hex color(s)" {:colors strs})))))

^:rct/test
(comment
  (colors-parser "#ff0000,#00ff00") ;; =>
  [{:r 255 :g 0 :b 0}
   {:r 0 :g 255 :b 0}])
