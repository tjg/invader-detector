(ns industries.tjg.cli-test
  {:clj-kondo/ignore [:refer :unresolved-symbol]}
  (:require
   [expectations.clojure.test :refer [defexpect expect use-fixtures]]
   [industries.tjg.invader-detector.cli :as sut]
   [industries.tjg.invader-detector.test.fixtures :as fixtures]
   [me.raynes.fs :as fs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Test fixtures

(defonce ^:private test-dir (atom nil))
(use-fixtures :once
  (fixtures/make-silent-fixture
   #(reset! test-dir (fs/temp-dir ""))
   #(fs/delete-dir @test-dir)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Tests

(def ^:private base-switches
  ["--radar-sample-file" "resources/spec-radar-sample-2-guys.txt"
   "--invader-files" "resources/spec-invader-1.txt:resources/spec-invader-2.txt"
   "--print-matches"])

(def ^:private alt-switches
  ["--radar-sample-file" "resources/spec-radar-sample-2-guys-alt.txt"
   "--invader-files" "resources/spec-invader-1-alt.txt:resources/spec-invader-2-alt.txt"
   "--print-matches"])

(defexpect lenient-parsing
  (let [results (apply sut/-main
                       "--input-on-chars" "█"
                       "--input-off-chars" " "
                       "--input-lenient-parsing"
                       alt-switches)]
    (expect
     true
     (< 5 (->> results :matches count)))))

(defexpect max-results
  (let [results (apply sut/-main "--max-results" "1" base-switches)]
    (expect 1
            (->> results :matches count))))

(defexpect score-threshold
  (let [results-90 (apply sut/-main "--score-threshold" "90" base-switches)
        results-70 (apply sut/-main "--score-threshold" "70" base-switches)]
    (expect
     true
     (< (->> results-90 :matches count)
        (->> results-70 :matches count)))))

(defexpect save-files
  (let [ascii-file (fs/file @test-dir "a.txt")
        image-file-1 (fs/file @test-dir "a.jpg")
        image-file-2 (fs/file @test-dir "a.png")
        match-file (fs/file @test-dir "a.edn")]
    (apply sut/-main
           "--save-ascii" (str ascii-file)
           "--save-images" (str image-file-1 ":" image-file-2)
           "--save-matches" (str match-file)
           base-switches)
    (expect
     #{"a.txt" "a.jpg" "a.png" "a.edn"}
     (->> @test-dir fs/list-dir (map fs/base-name) set))))

(defexpect print-ascii
  (let [results (apply sut/-main "--print-ascii" base-switches)]
    (expect
     true
     (contains? results :print-ascii-sink))))

(defexpect print-matches
  (let [results (apply sut/-main "--print-matches"
                       base-switches)]
    (expect
     true
     (contains? results :print-matches-sink))))
