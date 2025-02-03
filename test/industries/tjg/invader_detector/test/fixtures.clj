(ns industries.tjg.invader-detector.test.fixtures
  (:import
   (java.io StringWriter)))


(defn make-basic-fixture [setup-fn teardown-fn]
  (fn [f]
    (setup-fn)
    (try
      (f)
      (finally
        (teardown-fn)))))

(defn make-silent-fixture [setup-fn teardown-fn]
  (fn [f]
   (let [null-writer (StringWriter.)]
     (binding [*out* null-writer
               *err* null-writer]
       (setup-fn)
       (try
         (f)
         (finally
           (teardown-fn)))))))
