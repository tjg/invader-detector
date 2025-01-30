(ns industries.tjg.invader-detector.match
  (:require
   [clojure.string :as str]
   [industries.tjg.invader-detector.utils :as utils]))

(defn- similarity-score [a b]
  (when (not= [(-> a first count)
               (-> a count)]
              [(-> b first count)
               (-> b count)])
    (ex-info "Array sizes don't match." {:a a :b b}))
  (let [[size-x size-y] [(-> a first count)
                         (-> a count)]
        size-total (* size-x size-y)
        matches (for [y (range 0 size-y)
                      x (range 0 size-x)]
                  (= (get-in a [y x])
                     (get-in b [y x])))
        match-count (->> matches
                         (filter identity)
                         count)]
    (/ match-count size-total)))

^:rct/test
(comment
  (similarity-score
   [[1 0 0]]
   [[0 1 0]]) ;; => 1/3

  (similarity-score
   [[0 1 0]]
   [[0 1 0]])  ;; => 1

  )


(defn- similarity-at-offset [a b {:keys [b-offset] :as _opts}]
  (when (not= [(-> a first count)
               (-> a count)]
              [(-> b first count)
               (-> b count)])
    (ex-info "Array sizes don't match." {:a a :b b}))
  (let [{:keys [a-start a-size b-start overlap-size]}
        (utils/bounding-box-intersection a b b-offset)

        match-image
        (->> (range 0 (:y overlap-size))
             (mapv (fn [y]
                     (let [a-array-at-y (get a (+ y (:y a-start)))
                           b-array-at-y (get b (+ y (:y b-start)))]
                       (->> (range 0 (:x overlap-size))
                            (mapv (fn [x]
                                    (let [a-x (get a-array-at-y (+ x (:x a-start)))
                                          b-x (get b-array-at-y (+ x (:x b-start)))]
                                      (if (= a-x b-x) 1 0)))))))))

        match-count (->> match-image
                         flatten
                         (filter #(= % 1))
                         count)]
    {:effective-sizes [(:x overlap-size) (:y overlap-size)]
     :a-absolute-size (* (:x a-size) (:y a-size))
     :match-count match-count
     :match-image match-image}))

^:rct/test
(comment
  (def ^:private test-a
    [[0 0 0]
     [0 1 1]
     [0 1 1]])

  (def ^:private test-b
    [[1 1 0 0]
     [1 1 0 0]
     [0 0 0 0]
     [0 0 0 0]])

  (similarity-at-offset test-a test-b
                        {:b-offset [0 0]})
  ;; => {:effective-sizes [3 3]
  ;;     :a-absolute-size 9
  ;;     :match-count 3
  ;;     :match-image [[0 0 1]
  ;;                   [0 1 0]
  ;;                   [1 0 0]]}

  (similarity-at-offset test-a test-b
                        {:b-offset [-1 -1]})
  ;; => {:effective-sizes [2 2]
  ;;     :a-absolute-size 9
  ;;     :match-count 4
  ;;     :match-image [[1 1]
  ;;                   [1 1]]}

  (similarity-at-offset test-a test-b
                        {:b-offset [-4 -4]})
  ;; => {:effective-sizes [-1 -1]
  ;;     :a-absolute-size 9
  ;;     :match-count 0
  ;;     :match-image []}

  )

(defn- averaging-score [{:keys [effective-sizes a-absolute-size match-count]
                        :as _similarity}]
  (let [[xe ye] effective-sizes
        matched-size (* xe ye)
        unmatched-size (- a-absolute-size matched-size)]
    (/ (+ match-count (* unmatched-size 0.5))
       a-absolute-size)))

(defn matches [a b]
  (let [[ax ay] (utils/size a)
        [bx by] (utils/size b)
        [x0 y0] [(- 1 ax) (- 1 ay)]]
    (for [y (range y0 by)
          x (range x0 bx)]
      (let [similarity (similarity-at-offset a b {:b-offset [x y]})
            similarity (assoc similarity
                              ::averaging-score (averaging-score similarity)
                              ::radar-offset [x y])]
        similarity))))
