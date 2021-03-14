(ns picture-gallery.usecase.sample.ping-pong-test
  (:require [picture-gallery.usecase.sample.ping-pong :as sut]
            [clojure.test :as t]))

(t/deftest ping-pong
  (t/testing "w comment"
    (let [[result _] (sut/ping-pong {:ping "ping" :comment "hello"})]
      (t/is (= "pong" (:pong result)))
      (t/is (= "hello" (:comment result)))))
  (t/testing "w/o comment"
    (let [[result _] (sut/ping-pong {:ping "ping"})]
      (t/is (= "pong" (:pong result)))
      (t/is (nil? (:comment result))))))
