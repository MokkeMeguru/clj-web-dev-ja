(ns picture-gallery.domain.sample-test
  (:require [picture-gallery.domain.sample :as sut]
            [picture-gallery.utils.string :as pg-string]
            [clojure.spec.alpha :as s]
            [clojure.test :as t]))

(t/deftest ping
  (t/is (not (s/valid? ::sut/ping "pong")))
  (t/is (not (s/valid? ::sut/ping 0)))
  (t/is (s/valid? ::sut/ping "ping")))

(t/deftest pong
  (t/is (not (s/valid? ::sut/pong "ping")))
  (t/is (not (s/valid? ::sut/pong 0)))
  (t/is (s/valid? ::sut/pong "pong")))

(t/deftest _comment
  (t/is (s/valid? ::sut/comment "hello"))
  (t/is (not (s/valid? ::sut/comment (pg-string/rand-str 2048)))))
