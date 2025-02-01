(ns user
  (:require
   [industries.tjg.invader-detector.run :as run])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- make-temp-dir
  "Creates a temporary directory and returns its path."
  [subdir]
  (str (Files/createTempDirectory subdir (make-array FileAttribute 0))))


(def ^:private default-opts
  {:radar-sample-file "resources/spec-radar-sample.txt"
   :invader-files ["resources/spec-invader-1.txt"
                   "resources/spec-invader-2.txt"]
   :input-on-chars [\o \O]
   :input-off-chars [\-]

   :score-threshold 70

   :invader-colors [{:r 67, :g 0, :b 255}
                    {:r 68, :g 242, :b 13}]
   :output-on-char \o
   :output-off-char \-
   :output-opaque-fill true

   :print-matches true})

(defn locate-invaders!
  "Dev sandbox for locating invaders."
  []
  (let [filename-prefix "test"
        temp-dir (make-temp-dir "invader-detector-user")
        file-prefix (str temp-dir "/" filename-prefix)
        file-opts {:save-matches (str file-prefix ".edn")
                   :output-ascii (str file-prefix ".txt")
                   :output-images [(str file-prefix ".png")
                                   (str file-prefix ".jpeg")]}
        opts (merge default-opts file-opts)]
    (cond->> opts
      true (run/source  :invaders)
      true (run/source  :radar)
      true (run/process :matches)
      (get opts :output-ascii)  (run/sink :ascii)
      (get opts :output-images) (run/sink :images)
      (get opts :save-matches)  (run/sink :save-matches)
      (get opts :print-matches) (run/sink :print-matches))
    temp-dir))
