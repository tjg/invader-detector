(ns industries.tjg.invader-detector.cli
  (:gen-class)
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [industries.tjg.invader-detector.run :as run]
   [me.raynes.fs :as fs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utils

(defn- validate-image-filepaths [image-paths]
  (->> image-paths
       (map fs/extension)
       (map #(subs % 1))
       (map str/lower-case)
       (every? run/available-image-formats)))

^:rct/test
(comment
  (validate-image-filepaths ["foo.png" "foo.PnG"])
  ;; => true
  (validate-image-filepaths ["foo.txt"])
  ;; => false
)

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
  (when-not (= 1 (count s))
    (throw (ex-info "Must be a single char" {:char s})))
  (first s))

(def ^:private cli-options
  "Commandline opts."
  [[nil "--radar-sample-file FILE" "Radar sample file"]
   [nil "--invader-files FILES" "Invader files separated by colons"
    :parse-fn #(str/split % #":")]

   ;; Parsing.
   [nil "--input-on-chars CHARS" "Characters denoting 'on', separated by commas"
    :default [\o \O]
    :default-desc "o,O"
    :parse-fn multiple-chars-parser]
   [nil "--input-off-chars CHARS" "Characters denoting 'off', separated by commas"
    :default [\-]
    :default-desc "-"
    :parse-fn multiple-chars-parser]
   [nil "--input-lenient-parsing"
    "Be lenient when interpreting input files."]


   ;; Matching.
   [nil "--max-results COUNT" "Maximum number of matches"
    :parse-fn #(Long/parseLong %)]
   [nil "--score-threshold PERCENT" "Minimum match score to include in results. Number from 0 to 100"
    :default 70
    :parse-fn #(Integer/parseInt %)
    :validate [#(<= 0 % 100) "Must be a number between 0 and 100"]]

   ;; Output.
   [nil "--output-ascii FILE" "Output text file"]
   [nil "--output-images FILES"
    "Output image files, separated by colons."
    :parse-fn #(str/split % #":")
    :validate [validate-image-filepaths
               (str "Filename extension unknown. \nIt must be one of: "
                    (->> run/available-image-formats
                         (map str/lower-case)
                         set
                         sort
                         (str/join ", ")))]]
   [nil "--save-matches FILE" "File with EDN-encoded matches"]
   [nil "--print-matches"]

   ;; Emitting ascii output.
   [nil "--output-on-char CHAR"
    "For ascii output, character denoting 'on'."
    :default \o
    :parse-fn single-char-parser]
   [nil "--output-off-char CHAR" "For ascii output, character denoting 'off'."
    :default \-
    :parse-fn single-char-parser]

   ["-h" "--help"]])

(defn- usage [options-summary]
  (->> ["This is my program. There are many like it, but this one is mine."
        ""
        "Usage: program-name [options]"
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
        output-formats #{:output-ascii :output-images
                         :save-matches :print-matches}
        output-format-switches (->> output-formats
                                    (map name)
                                    (map #(format "--%s" %))
                                    sort)]
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}

      errors
      {:exit-message (error-msg errors)}

      (not (seq (set/intersection opt-keys output-formats)))
      {:exit-message
       (error-msg
        (concat ["Must specify at least one output format."
                 ""
                 "Output format switches:"]
                output-format-switches
                [""
                 "Use --help for more explanation of each switch."]))}

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
