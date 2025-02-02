(ns industries.tjg.run-test
  {:clj-kondo/ignore [:refer :unresolved-symbol]}
  (:require
   [clojure.string :as str]
   [expectations.clojure.test :refer [defexpect expect expecting]]
   [industries.tjg.invader-detector.run :as sut]))

(def ^:private opts
  { ;; Source opts.
   :radar-sample-file "resources/spec-radar-sample-small.txt"
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
   :output-ascii-opaque-fill false

   ;; String sinks.
   :string-matches true
   :string-ascii true})

(defexpect locates-invaders
  (expecting "readable characters surrounding candidate invaders"
    (expect
     ["    █  ██│73%█  █│77%███  │      █   ██ █│88%██   │  █         █  "
      "  █ █    │███████│ ██████ │ █   █    █   │  ███ █ │ █  █    █     "
      "  █      │ ██ ███│██ ██ ██│    █         │  █████ │   ██    █     "
      "       █ │██████ │█ ██ █  │ █     ██  █ █│██  █ ██│██ █        █  "
      "      █  │█ ███ █│█    █  │  ██       █  │██ █████│█      █    █  "
      " █  █    │█ █   █│█████ █ │     ██   █   │     █  │  █ ██         "
      "█        ╰───────╰────────╯  █       █  █│██ ██ █ │ █    ██       "
      "  █                         ██         ██│█ █  ███│   ██    █  █  "
      "                   █          █      █  █╰────────╯     █        █"
      "                     ██         ██ █ █  ███    ██   ╭91%────────╮ "
      "           █    ██      █  █       █  █     █     █ │  █     █  │ "
      "██       █      █    █     ╭84%─────╮   █     █     │       █   │ "
      "   █     █     █         ██│   ██   │ █         █   │  ████ ██  │ "
      "     █         █         █ │  ████  │          ██ ██│    ███ ██ │ "
      "               █           │████████│  █     █      │█  ██████ █│ "
      "     █      █   █   █      │██ ██  █│ █         █  █│█ █ █████ █│ "
      "         ██       ██    █  │ ███████│       █    █ █│█ █     █ █│ "
      "      █ █               █  │  █  ███│  ███       █  │   ██ ██   │ "
      "██                         │ █ ██   │      █      █ ╰───────────╯ "
      "     █     █       █ ██    │██  ██ █│   ██          ╰────────╯──╯ "
      "   █  ███ █         █ █    ╰────────╯   █         █    █  █       "
      "   █      █                █    █      █      █   ██           █  "]
     (-> (sut/locate-invaders! (merge opts {:output-ascii-opaque-fill false}))
         :string-ascii-sink
         str/split-lines)))

  (expecting "transparency/opacity set by `:output-ascii-opaque-fill`"
    (expect
     ["   ╭84%─────╮      █ ╭91%────────╮ "
      " ██│        │ █  █ █ │           │ "
      " █ │        │   ██   │           │ "
      "   │        │  █     │           │ "
      "   │        │ █  █   │           │ "
      "█  │        │        │           │ "
      "█  │        │  █    █│           │ "
      "   │        │     █ █│           │ "
      "   │        │     █  │           │ "
      "   ╰────────╯      █ ╰───────────╯ "
      "   █    █      █     ╰────────╯──╯ "]
     (-> (sut/locate-invaders!
          (merge opts
                 {:radar-sample-file "resources/spec-radar-sample-2-guys.txt"
                  :output-ascii-opaque-fill true}))
         :string-ascii-sink
         str/split-lines))

    (expect
     ["   ╭84%─────╮      █ ╭91%────────╮ "
      " ██│   ██   │ █  █ █ │  █     █  │ "
      " █ │  ████  │   ██   │       █   │ "
      "   │████████│  █     │  ████ ██  │ "
      "   │██ ██  █│ █  █   │    ███ ██ │ "
      "█  │ ███████│        │█  ██████ █│ "
      "█  │  █  ███│  █    █│█ █ █████ █│ "
      "   │ █ ██   │     █ █│█ █     █ █│ "
      "   │██  ██ █│     █  │   ██ ██   │ "
      "   ╰────────╯      █ ╰───────────╯ "
      "   █    █      █     ╰────────╯──╯ "]
     (-> (sut/locate-invaders!
          (merge opts
                 {:radar-sample-file "resources/spec-radar-sample-2-guys.txt"
                  :output-ascii-opaque-fill false}))
         :string-ascii-sink
         str/split-lines))))
