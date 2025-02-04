(ns industries.tjg.invader-detector.test.fixtures
  (:import
   (java.io StringWriter)))


(defn make-basic-fixture
  "Run `setup-fn` before the tests, then afterwards run `teardown-fn`.

  Both `setup-fn` & `teardown-fn` functions are called with no parameters."
  [setup-fn teardown-fn]
  (fn [f]
    (setup-fn)
    (try
      (f)
      (finally
        (teardown-fn)))))

(defn make-silent-fixture
  "Silence 'noisy' tests that print data.

  Run `setup-fn` before the tests, then afterwards run `teardown-fn`.
  Both `setup-fn` & `teardown-fn` functions are called with no
  parameters."
  [setup-fn teardown-fn]
  (fn [f]
    (let [null-writer (StringWriter.)]
      (binding [*out* null-writer
                *err* null-writer]
        (setup-fn)
        (try
          (f)
          (finally
            (teardown-fn)))))))
