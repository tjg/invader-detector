(ns industries.tjg.invader-detector.utils)

(defn round [n]
  (if (integer? n)
    n
    (Math/round (float n))))

(defn size [a]
  [(-> a first count)
   (-> a count)])

(defn overlapping-bounding-boxes [a b [x y :as _b-offset]]
  (let [[b-offset-x b-offset-y] [x y]

        ;; Actually measured from the datastructures, not calculated.
        [b-size-x b-size-y] (size b)
        [a-size-x a-size-y] (size a)

        ;; Zero; or if the offset's negative, shift.
        [a-start-x a-start-y] [(max 0 (- b-offset-x))
                               (max 0 (- b-offset-y))]
        ;; Size; or distance from b's start to its end.
        [a-end-x a-end-y] [(min a-size-x
                                (- b-size-x b-offset-x))
                           (min a-size-y
                                (- b-size-y b-offset-y))]

        ;; We've calculated a's bounds, using a's & b's dimensions. So
        ;; use it to calculate the effective size of both a & b.
        [effective-size-x effective-size-y] [(- a-end-x a-start-x)
                                             (- a-end-y a-start-y)]

        ;; b's offset, or 0 if it's negative.
        [b-start-x b-start-y] [(max 0 b-offset-x)
                               (max 0 b-offset-y)]

        ;; b's start + the effective size of a (and b).
        [b-end-x b-end-y] [(+ b-start-x effective-size-x)
                           (+ b-start-y effective-size-y)]
        a-size-total (* a-size-x a-size-y)]
    {:a-start {:x a-start-x :y a-start-y}
     :b-start {:x b-start-x :y b-start-y}
     :a-size {:x a-size-x :y a-size-y}
     :b-size {:x b-size-x :y b-size-y}
     :overlap-size {:x effective-size-x :y effective-size-y}}))
