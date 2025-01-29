(ns industries.tjg.rich-comment-test
  (:require [clojure.test :as t :refer [deftest]]
            [com.mjdowney.rich-comment-tests.test-runner :as test-runner]))

(deftest rct-tests
  (test-runner/run-tests-in-file-tree! :dirs #{"src"}))
