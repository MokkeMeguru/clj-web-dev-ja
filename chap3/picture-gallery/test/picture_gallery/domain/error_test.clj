(ns picture-gallery.domain.error-test
  (:require [picture-gallery.domain.error :as sut]
            [clojure.test :as t]
            [clojure.spec.alpha :as s]))

(t/deftest body
  (t/is (not (s/valid? ::sut/body {:code -1 :message "sample"})))
  (t/is (not (s/valid? ::sut/body {:code 1 :message nil})))
  (t/is (s/valid? ::sut/body {:code 1 :message "sample"})))

(t/deftest error
  (t/is (not (s/valid? ::sut/error {:status -1  :body {:code 1 :message "sample"}})))
  (t/is (s/valid? ::sut/error {:status 400 :body {:code 1 :message "sample"}})))

(t/deftest input-data-is-invalid
  (let [error (sut/input-data-is-invalid "spec error")]
    (t/is (s/valid? ::sut/error error))))
