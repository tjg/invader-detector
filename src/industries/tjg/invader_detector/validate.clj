(ns industries.tjg.invader-detector.validate
  (:require
   [clojure.pprint :as pprint]
   [clojure.set :as set]
   [clojure.string :as str]
   [malli.core :as m]
   [malli.error :as me]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utils

(defn- uniform-line-length? [s]
  (let [line-lengths (->> s str/split-lines (map count) set)]
    (= 1 (count line-lengths))))

(defn- invalid-chars [s ok-chars]
  (set/difference (set s) (set ok-chars)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API

(defn make-radar-sample-schema
  "Malli schema for radar samples."
  [{:keys [ok-chars pad-lines?]}]
  (let [ok-chars (set/union (set ok-chars) #{\newline \return})]
    [:and
     string?
     [:fn {:error/message "all lines should have the same length"}
      (fn [s] (or pad-lines?
                  (uniform-line-length? s)))]
     [:fn
      {:error/fn
       {:en (fn [{:keys [value]} _]
              (let [bad-chars (invalid-chars value ok-chars)]
                (pprint/cl-format
                 nil "contains invalid chars: ~{'~a'~^, ~}" bad-chars)))}}
      #(empty? (invalid-chars % ok-chars))]]))


^:rct/test
(comment
  (me/humanize (m/explain (make-radar-sample-schema {:ok-chars #{\. \newline \return}
                                             :pad-lines? true})
                          "..\n'?"))
  ;; => ["contains invalid chars: ''', '?'"]

  (me/humanize (m/explain (make-radar-sample-schema {:ok-chars #{\. \newline \return}})
                          "..\n."))
  ;; => ["all lines should have the same length"]

  (m/validate (make-radar-sample-schema {:ok-chars #{\. \newline \return}})
              "..\n..")
  ;; => true

  )
