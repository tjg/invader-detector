(ns industries.tjg.invader-detector.cli
  (:gen-class)
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [industries.tjg.invader-detector.run :as run]
   [industries.tjg.invader-detector.utils :as utils]
   [malli.core :as malli]))

(def ^:private default-opts
  {:invader-colors [{:r 67, :g 0, :b 255}
                    {:r 68, :g 242, :b 13}]
   :input-on-chars [\o \O]
   :input-off-chars [\-]
   :score-threshold 70
   :output-ascii-on-char \o
   :output-ascii-off-char \-})

(defn- kwd-to-switch [kwd]
  (->> kwd name (format "--%s")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Schemas

(def ^:private color-schema
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

(def ^:private multiple-colors-schema
  [:sequential {:min 1} color-schema])

(def ^:private hex-encoded-color-schema
  [:re utils/hex-color-code-regex])

(def ^:private multiple-hex-encoded-colors-schema
  [:sequential {:min 1} hex-encoded-color-schema])

^:rct/test
(comment
  (malli/validate multiple-hex-encoded-colors-schema ["#aabbcc"]) ;; => true
  (malli/validate multiple-hex-encoded-colors-schema []) ;; => false
)

(def ^:private image-file-schema
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

(def ^:private multiple-image-files-schema
  [:sequential {:min 1} image-file-schema])

(def ^:private single-char-schema
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

(defn- multiple-chars-formatter [characters]
  (str/join "," characters))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Parsers

(defn- multiple-chars-parser [s]
  (let [strs (str/split s #",")]
    (when-not (->> strs
                   (map count)
                   (every? #(= % 1)))
      (throw (ex-info "Contains an entry longer than a single char"
                      {:strs strs})))
    (->> strs
         (map first))))

(defn- single-char-parser [s]
  (if (malli/validate single-char-schema s)
    (first s)
    (throw (ex-info "Must be a single char" {:char s}))))

(defn- colors-parser [s]
  (let [strs (str/split s #",")]
    (if (malli/validate multiple-hex-encoded-colors-schema strs)
      (->> strs
           (map utils/hex-to-rgb))
      (throw (ex-info "Invalid hex color(s)" {:colors strs})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; CLI options

(def ^:private cli-options
  "Commandline opts."
  [[nil "--radar-sample-file FILE" "Radar sample file"
    :validate [utils/file-exists? "Image file must exist"]]
   [nil "--invader-files FILES" "Invader files separated by colons"
    :parse-fn #(str/split % #":")
    :validate [#(every? utils/file-exists? %) "Image files must exist"]]

   ;; Parsing.
   [nil "--input-on-chars CHARS" "Characters denoting 'on', separated by commas"
    :default (:input-on-chars default-opts)
    :default-desc (multiple-chars-formatter (:input-on-chars default-opts))
    :parse-fn multiple-chars-parser]
   [nil "--input-off-chars CHARS" "Characters denoting 'off', separated by commas"
    :default (:input-off-chars default-opts)
    :default-desc (multiple-chars-formatter (:input-off-chars default-opts))
    :parse-fn multiple-chars-parser]
   [nil "--input-lenient-parsing"
    "Be lenient when interpreting input files."
    :default true]


   ;; Matching.
   [nil "--max-results COUNT" "Maximum number of matches"
    :parse-fn #(Long/parseLong %)]
   [nil "--score-threshold PERCENT" "Minimum match score to include in results. Number from 0 to 100"
    :default (:score-threshold default-opts)
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 0 % 100) "Must be a number between 0 and 100"]]

   ;; Output.
   [nil "--print-ascii" "Print ascii to screen"]
   [nil "--save-ascii FILE" "Output text file"
    :validate [utils/directory-exists? "Image directory must exist"]]
   [nil "--save-images FILES"
    "Output image files, separated by colons."
    :parse-fn #(str/split % #":")
    :validate [#(malli/validate multiple-image-files-schema %)
               (str "Directory doesn't exist, or filename extension unknown. \nExtension must be one of: "
                    (->> run/available-image-formats
                         (map str/lower-case)
                         set
                         sort
                         (str/join ", ")))]]
   [nil "--print-matches" "Print matches to screen"]
   [nil "--save-matches FILE" "File with EDN-encoded matches"
    :validate [utils/directory-exists? "Matches directory must exist"]]

   ;; Colors
   [nil "--invader-colors COLORS"
    "Colors to highlight invaders. Recycled if fewer colors than invaders."
    :default (:invader-colors default-opts)
    :default-desc (->> (:invader-colors default-opts)
                       (map utils/rgb-to-hex)
                       multiple-chars-formatter)
    :parse-fn colors-parser
    :validate [#(malli/validate multiple-colors-schema %)
               "Colors must be hex-encoded values. Example: '#aabbcc,#001122'"]]

   ;; Emitting ascii output.
   [nil "--output-ascii-on-char CHAR"
    "For ascii output, character denoting 'on'."
    :default (:output-ascii-on-char default-opts)
    :parse-fn single-char-parser]
   [nil "--output-ascii-off-char CHAR" "For ascii output, character denoting 'off'."
    :default (:output-ascii-off-char default-opts)
    :parse-fn single-char-parser]
   [nil "--output-ascii-opaque-fill"
    "For ascii output, make bounding boxes blank inside."]

   ["-h" "--help"]])

(defn- usage [options-summary]
  (->> ["Detect invaders in radar samples."
        ""
        "Usage: ./invader-detector.sh [options]"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(def ^:private required-args
  #{:radar-sample-file :invader-files})

(defn validate-args
  "Validate command line arguments.

  Either return a map indicating the program should exit (with an error
  message, and optional ok status), or a map indicating the action the
  program should take and the options provided."
  [args]
  (let [{:keys [options _arguments errors summary]}
        (cli/parse-opts args cli-options)
        opt-keys (->> options keys set)
        input-switches (->> [:radar-sample-file :invader-files]
                            (map kwd-to-switch))
        output-formats #{:save-ascii :print-ascii :save-images
                         :save-matches :print-matches}
        output-format-switches (->> output-formats
                                    (map kwd-to-switch)
                                    sort)
        help-pointer "Use --help for more explanations."]
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}

      errors
      {:exit-message (error-msg errors)}

      (not (every? #(contains? options %) required-args))
      {:exit-message
       (error-msg
        (concat ["Must specify a radar sample and at least one invader:"]
                input-switches
                [""
                 help-pointer]))}

      (not (some #(contains? options %) output-formats))
      {:exit-message
       (error-msg
        (concat ["Must specify at least one output format."
                 ""
                 "Output format switches:"]
                output-format-switches
                [""
                 help-pointer]))}

      ;; FIXME: let user know what's required
      (seq (set/intersection opt-keys required-args))
      {:action :run :options options}

      :else
      {:exit-message (usage summary)})))

(defn- exit [status msg]
  (println msg)
  (System/exit status))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Program entrypoint

(defn -main
  "Locate invaders in radar sample file. Program entry point."
  [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        :run (run/locate-invaders! options)))))
