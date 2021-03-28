(ns picture-gallery.utils.string-test
  (:require [picture-gallery.utils.string :as sut]
            [clojure.test :as t]))

(t/deftest rand-str
  (t/testing "length of the generated string"
    (t/is (= 128 (count (sut/rand-str 128))))))
