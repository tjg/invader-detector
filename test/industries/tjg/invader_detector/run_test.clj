(ns industries.tjg.invader-detector.run-test
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
     (-> (sut/locate-invaders! (merge opts {:output-ascii-opaque-fill false
                                            :string-ascii true}))
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
                  :string-ascii true
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
                  :string-ascii true
                  :output-ascii-opaque-fill false}))
         :string-ascii-sink
         str/split-lines))))

(defexpect ragged-inputs
  (expecting "a string of some sort, but no guarantees"
    (expect string?
            (-> (sut/locate-invaders!
                 (merge opts
                        {:invader-files ["resources/spec-invader-1-glitchy.txt"
                                         "resources/spec-invader-2.txt"]
                         :radar-sample-file
                         "resources/spec-radar-sample-2-guys-glitchy.txt"
                         :string-ascii true
                         :output-ascii-opaque-fill false
                         :input-lenient-parsing true}))
                ;; Just use the string, without splitting lines for readability.
                :string-ascii-sink)))

  (expecting "unknown chars parsed into 0's, and ragged lines padded with 0's"
   (expect
    ["   ╭84%─────╮      █ ╭85%───────────"
     " ██│   ██   │ █  █ █ │  █     █  █  "
     " █ │  ████  │   ██   │       █      "
     "   │████████│  █     │  ████ ██  █  "
     "   │██ ██  █│ █  █   │    ███ ██    "
     "   │ ███████│        │█  █████      "
     "   │  █  ███│  █    █│█ █ █████ █   "
     "   │ █ ██   │     █ █│█ █     █ █   "
     "   │██  ██ █│     █  │   ██ ██      "
     "   ╰────────╯      █ ╰──────────────"
     "   █    █      █     ╰────────╯──╯  "]
    (-> (sut/locate-invaders!
         (merge opts
                {:invader-files ["resources/spec-invader-1-glitchy.txt"
                                 "resources/spec-invader-2.txt"]
                 :radar-sample-file "resources/spec-radar-sample-2-guys-glitchy.txt"
                 :string-ascii true
                 :output-ascii-opaque-fill false
                 :input-lenient-parsing true}))
        :string-ascii-sink
        str/split-lines))))

(defexpect matches-available
  (let [results
        (sut/locate-invaders!
         (merge opts {:radar-sample-file
                      "resources/spec-radar-sample-2-guys.txt"
                      :string-ascii true}))]
    (expecting "four matches"
      (expect #{{:bbox {:x 4, :y 1, :width 8, :height 8},
                 :score 27/32,
                 :invader-id 1}
                {:bbox {:x 22, :y 1, :width 11, :height 8},
                 :score 10/11,
                 :invader-id 0}
                {:bbox {:x 22, :y 2, :width 8, :height 8},
                 :score 23/32,
                 :invader-id 1}
                {:bbox {:x 25, :y 2, :width 8, :height 8},
                 :score 45/64,
                 :invader-id 1}}
              (-> (:matches results)
                  set))

      (expect ["   ╭84%─────╮      █ ╭91%────────╮ "
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
              (-> results
                  :string-ascii-sink
                  str/split-lines)))))
