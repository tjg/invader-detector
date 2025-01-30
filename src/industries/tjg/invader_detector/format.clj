(ns industries.tjg.invader-detector.format
  (:require
   [clojure.string :as str]))

(def ^:private default-char-true \█)
(def ^:private default-char-false \space)

(defn format-radar-sample
  "Format radar sample as unicode text.

  True & false pixels are emitted as `char-true` & `char-false`,
  respectively."
  ([image]
   (format-radar-sample image {}))
  ([image {:keys [char-true char-false]
           :or {char-true default-char-true
                char-false default-char-false}
           :as _opts}]
   (->> image
        (map (fn [row]
               (->> row
                    (map (fn [pixel]
                           (if (zero? pixel) char-false char-true)))
                    (apply str))))
        (str/join \newline))))

^:rct/test
(comment
  (format-radar-sample [[0 1 0]
                 [1 0 0]])
  ;; => " █ \n█  "

  )
