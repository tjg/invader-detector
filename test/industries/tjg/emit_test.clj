(ns industries.tjg.emit-test
  {:clj-kondo/ignore [:refer :unresolved-symbol]}
  (:require
   [expectations.clojure.test
    :refer [defexpect expect expecting use-fixtures]]
   [industries.tjg.invader-detector.emit :as sut]
   [industries.tjg.invader-detector.test.fixtures :as fixtures]
   [me.raynes.fs :as fs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Fixtures

(defonce ^:private test-dir (atom nil))
(use-fixtures :once
  (fixtures/make-basic-fixture
   #(reset! test-dir (fs/temp-dir ""))
   #(fs/delete-dir @test-dir)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Tests

(defexpect draws-character-2d-vector
  (expect [[\f \t \f \t]
           [\t \f \t \f]]
          (sut/draw-pixel-matrix
           [[0 1 0 1]
            [1 0 1 0]]
           {:char-true  \t
            :char-false \f}))

  (expect []
          (sut/draw-pixel-matrix
           []
           {:char-true  \t
            :char-false \f})))

(def ^:private dot-matrix-6x6
  [[\. \. \. \. \. \.]
   [\. \. \. \. \. \.]
   [\. \. \. \. \. \.]
   [\. \. \. \. \. \.]
   [\. \. \. \. \. \.]
   [\. \. \. \. \. \.]])

(defexpect draws-scoreboxes
  (expecting "transparent background"
    (expect ["......"
             ".╭──╮."
             ".│5%│."
             ".│..│."
             ".╰──╯."
             "......"]
            (->> (sut/draw-scoreboxes
                  dot-matrix-6x6
                  [{:score 1/20 ;; Label will say `5%`.
                    :bbox {:x 2 :y 2 :width 2 :height 2}}]
                  {:label-offset {:x 1 :y 1 :transparent-fill? true}})
                 (map #(apply str %)))))

  (expecting "opaque background"
    (expect ["......"
             ".╭──╮."
             ".│5%│."
             ".│  │."
             ".╰──╯."
             "......"]
            (->> (sut/draw-scoreboxes
                  dot-matrix-6x6
                  [{:score 1/20 ;; Label will say `5%`.
                    :bbox {:x 2 :y 2 :width 2 :height 2}}]
                  {:label-offset {:x 1 :y 1}
                   :transparent-fill? false})
                 (map #(apply str %)))))

  (expecting "edgecases work"
    (expect ["......"
             "......"
             "......"
             "......"
             "......"
             "......"]
            (->> (sut/draw-scoreboxes
                  dot-matrix-6x6
                  []
                  {:label-offset {:x 1 :y 1}
                   :transparent-fill? false})
                 (map #(apply str %))))))

(defexpect draws-to-string
  (expect "......\n......\n......\n......\n......\n......"
          (sut/to-string dot-matrix-6x6)))

(defexpect saves-to-file
  (let [tmpfile (fs/temp-file "test-")]
    (sut/save-to-file! [[\.]] tmpfile {})

    (expect fs/exists? tmpfile)))
