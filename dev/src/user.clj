(ns user
  (:require
   [industries.tjg.invader-detector.run :as run]
   [me.raynes.fs :as fs])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defonce ^:private temp-dirs (atom []))

(defn- make-temp-dir
  "Creates a temporary directory and returns its path."
  [subdir]
  (str (Files/createTempDirectory subdir (make-array FileAttribute 0))))

(defn delete-temp-dirs!
  "Delete temp dirs we created."
  []
  (doseq [dir @temp-dirs]
    (fs/delete-dir dir))
  (reset! temp-dirs []))

(defn- file-opts [temp-dir filename-prefix]
  (let [file-prefix (str temp-dir "/" filename-prefix)]

    {:save-ascii (str file-prefix ".txt")
     :print-ascii true
     :save-matches (str file-prefix ".edn")
     :print-matches false
     :save-images [(str file-prefix ".png")
                   (str file-prefix ".jpeg")]}))

(def ^:private basic-opts
  {;; Source opts.
   :radar-sample-file "resources/spec-radar-sample.txt"
   :invader-files ["resources/spec-invader-1.txt"
                   "resources/spec-invader-2.txt"]
   :input-on-chars [\o \O]
   :input-off-chars [\-]
   :input-lenient-parsing false

   ;; Processor opts.
   :max-results nil
   :score-threshold 70

   ;; Sink opts.
   :invader-colors [{:r 67, :g 0, :b 255}
                    {:r 68, :g 242, :b 13}]
   :output-ascii-on-char \█
   :output-ascii-off-char \space
   :output-ascii-opaque-fill true})

(defn locate-invaders!
  "Dev sandbox for locating invaders."
  []
  (let [filename-prefix "user"
        temp-dir (make-temp-dir "invader-detector-user")
        _ (swap! temp-dirs conj temp-dir)
        opts (merge basic-opts (file-opts temp-dir filename-prefix))]
    (run/locate-invaders! opts)
    temp-dir))

(locate-invaders!)
