(ns picture-gallery.infrastructure.env-test
  (:require [picture-gallery.infrastructure.env :as sut]
            [clojure.test :as t]
            [orchestra.spec.test :as st]
            [integrant.core :as ig]))

(t/deftest decode-log-level
  (t/testing "exist key"
    (st/instrument)
    (t/is (= :trace (sut/decode-log-level "trace")))
    (t/is (= :debug (sut/decode-log-level "debug")))
    (t/is (= :info (sut/decode-log-level "info")))
    (t/is (= :warn (sut/decode-log-level "warn")))
    (t/is (= :error (sut/decode-log-level "error")))
    (t/is (= :fatal (sut/decode-log-level "fatal")))
    (t/is (= :report (sut/decode-log-level "report")))
    (st/unstrument))
  (t/testing "unknown key"
    (st/instrument)
    (t/is (= :info (sut/decode-log-level "unknown")))
    (st/unstrument)))

(t/deftest integrant-env
  (t/testing "integrate by some keys"
    (let [system (ig/init {:picture-gallery.infrastructure.env/env {}})
          init-result (:picture-gallery.infrastructure.env/env system)]
      (t/is (map? init-result))
      (t/is (contains? init-result :running))
      (t/is (contains? init-result :log-level))
      (t/is (nil? (ig/halt! system))))))
