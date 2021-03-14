(ns picture-gallery.utils.error-test
  (:require [picture-gallery.utils.error :as sut]
            [clojure.test :as t]
            [clojure.spec.alpha :as s]
            [orchestra.spec.test :as st]
            [clojure.string]))

(t/deftest bind-error
  (let [sample-func (fn [i] (if (= i 0) [:success nil] [nil :error]))]
    (t/testing "success in sample-func"
      (t/is (= [:success nil] (sut/bind-error sample-func [0 nil]))))
    (t/testing "fail in sample-func"
      (t/is (= [nil :error] (sut/bind-error sample-func [1 nil]))))
    (t/testing "fail in previous func"
      (t/is (= [nil :prev-error] (sut/bind-error sample-func [nil :prev-error]))))))

(t/deftest err->>
  (let [sample-func1 (fn [i] (if (zero? (mod i 2)) [i nil] [nil :error1]))
        sample-func2 (fn [i] (if (zero? (mod i 3)) [i nil] [nil :error2]))]
    (t/testing "all passed"
      (t/is (= [6 nil] (sut/err->> 6 sample-func1 sample-func2))))
    (t/testing "error at func1"
      (t/is (= [nil :error1] (sut/err->> 3 sample-func1 sample-func2)))
      (t/is (= [nil :error1] (sut/err->> 5 sample-func1 sample-func2))))
    (t/testing "error at func2"
      (t/is (= [nil :error2] (sut/err->> 2 sample-func1 sample-func2)))
      (t/is (= [nil :error2] (sut/err->> 4 sample-func1 sample-func2))))))

(defn sample-function [i] (inc i))
(defn sample-function-with-spec [i] {:pre [(int? i)]} (inc i))
(s/fdef sample-speced-function :args (s/cat :i int?))
(defn sample-speced-function [i] (inc i))

(t/deftest border-error
  (t/testing "success"
    (t/is (= [2 nil] (sut/border-error {:function #(sample-function 1)
                                        :error-wrapper str}))))
  (t/testing "fail in exceptioninfo"
    (let [[result message] (sut/border-error {:function #(sample-function-with-spec "")
                                              :error-wrapper str})]
      (t/is (= nil result))
      (t/is (clojure.string/starts-with? message "spec exception:"))))
  (t/testing "fail in assertion error"
    (st/instrument)
    (let [[result message] (sut/border-error {:function #(sample-speced-function "")
                                              :error-wrapper str})]

      (t/is (= nil result))
      (t/is (clojure.string/starts-with? message "spec exception:")))
    (st/unstrument))
  (t/testing "fail in exception"
    (let [[result message] (sut/border-error {:function #(sample-function "")
                                              :error-wrapper str})]
      (t/is (= nil result))
      (t/is (clojure.string/starts-with? message "unknown exception:")))))
