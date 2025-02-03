(ns industries.tjg.invader-detector.utils-test
  {:clj-kondo/ignore [:refer :unresolved-symbol]}
  (:require
   [clojure.string :as str]
   [expectations.clojure.test
    :refer [defexpect expect expecting]]
   [industries.tjg.invader-detector.utils :as sut]))

(defexpect rounding
  (expect 1 (sut/round 0.5))
  (expect 1 (sut/round 1/2))
  (expect 0 (sut/round 0.49))
  (expect 0 (sut/round 49/100)))


(defexpect edn-formatting
  (binding [*print-length* 0]
    (expect "[1 2 3 4]"
            (-> (sut/format-edn [1 2 3 4])
                str/trim))))

(defexpect pixel-matrix-sizing
  (expect [0 0]
          (sut/size []))
  (expect [2 3]
          (sut/size [[1 2]
                     [2 2]
                     [3 3]])))

(defexpect intersecting
  (expecting "2x2 intersection"
    (expect {:a-start {:x 0 :y 0}
             :b-start {:x 0 :y 0}
             :a-size {:x 3 :y 2}
             :b-size {:x 2 :y 3}
             :overlap-size {:width 2 :height 2}}

            (sut/bounding-box-intersection
             [[0 0 0]
              [0 0 0]]

             [[0 0]
              [0 0]
              [0 0]]

             [0 0])))

  (expecting "1x1 intersection, when we slide the first box ↖"
   (expect {:a-start {:x 1 :y 1}
            :b-start {:x 0 :y 0}
            :a-size {:x 3 :y 2}
            :b-size {:x 2 :y 3}
            :overlap-size {:width 2 :height 1}}
           (sut/bounding-box-intersection
            [[0 0 0]
             [0 0 0]]

            [[0 0]
             [0 0]
             [0 0]]

            [-1 -1]))))

(defexpect percent-formatting
  (expect "0%" (sut/format-score-as-percent 0))
  (expect "50%" (sut/format-score-as-percent 1/2))
  (expect "999900%" (sut/format-score-as-percent 9999)))

(defexpect cartesian-production
  (expect []
          (sut/cartesian-product [] [1 2 3]))
  (expect #{[:x1 :y1]
            [:x2 :y1]
            [:x1 :y2]
            [:x2 :y2]
            [:x1 :y3]
            [:x2 :y3]}
          (->> (sut/cartesian-product [:x1 :x2]
                                      [:y1 :y2 :y3])
               set)))

(defexpect hex-to-rgb-conversion
  (expect {:r 0 :g 255 :b 0} (sut/hex-to-rgb "#00FF00"))
  (expect NumberFormatException (sut/hex-to-rgb "#00FG00"))
  (expect Exception (sut/hex-to-rgb "#00FF000"))
  (expect Exception (sut/hex-to-rgb "#")))

(defexpect rgb-to-hex-conversion
  (expect "#00ff00" (sut/rgb-to-hex {:r 0 :g 255 :b 0}))
  (expect Exception (sut/rgb-to-hex {:r 0 :g 256 :b 0}))
  (expect Exception (sut/rgb-to-hex {:r 0 :g -1 :b 0}))
  (expect Exception (sut/rgb-to-hex {:r 0})))

(defexpect file-existence
  (expect identity
          (sut/file-exists? "deps.edn"))
  (expect identity
          (sut/file-exists? "resources/spec-invader-1.txt"))
  (expect not
          (sut/file-exists? "resources/spec-invader-129382938.txt"))

  (expect identity
          (sut/directory-exists? "deps.edn"))
  (expect identity
          (sut/directory-exists? "resources/spec-invader-1.txt"))
  (expect not
          (sut/directory-exists? "this_should_not_exist/spec-invader-1.txt")))
