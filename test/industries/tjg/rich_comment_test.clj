(ns industries.tjg.rich-comment-test
  {:clj-kondo/ignore [:refer]}
  (:require [clojure.test :as t :refer [deftest]]
            [com.mjdowney.rich-comment-tests.test-runner :as test-runner]))

(deftest rct-tests
  (test-runner/run-tests-in-file-tree! :dirs #{"src"}))
