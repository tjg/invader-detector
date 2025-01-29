(ns industries.tjg.invader-detector.format
  (:require
   [clojure.string :as str]))

(def default-char-true \█)
(def default-char-false \space)

(defn format-image
  ([image]
   (format-image image {}))
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
  (format-image [[0 1 0]
                 [1 0 0]])
  ;; => " █ \n█  "

  )
