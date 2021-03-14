(ns picture-gallery.interface.controller.api.sample.ping-post-test
  (:require [picture-gallery.interface.controller.api.sample.ping-post :as sut]
            [clojure.test :as t]))

(t/deftest http->
  (t/testing "invalid data"
    (let [[result err] (sut/http-> {:parameters {:query {:ping "hello"}}})]
      (t/is (nil? result))
      (t/is (= 422 (:status err)))
      (t/is (= 1 (-> err :body :code)))))
  (t/testing "with comment"
    (let [[result err] (sut/http-> {:parameters {:query {:ping "ping" :comment "hello"}}})]
      (t/is (nil? err))
      (t/is (= "ping" (:ping result)))
      (t/is (= "hello" (:comment result)))))
  (t/testing "without comment"
    (let [[result err] (sut/http-> {:parameters {:query {:ping "ping"}}})]
      (t/is (nil? err))
      (t/is (= "ping" (:ping result)))
      (t/is (nil? (:comment result))))))
