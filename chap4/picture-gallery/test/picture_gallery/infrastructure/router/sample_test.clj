(ns picture-gallery.infrastructure.router.sample-test
  (:require [picture-gallery.infrastructure.router.sample :as sut]
            [clojure.test :as t]
            [clojure.spec.alpha :as s]))

(t/deftest ping-pong-handler
  (t/testing "with comment"
    (let [http-response (sut/ping-post-handler {:parameters {:query {:ping "ping" :comment "hello"}}})]
      (t/is (= 200 (:status http-response)))
      (t/is (= "pong" (-> http-response :body :pong)))
      (t/is (= "hello" (-> http-response :body :comment)))))
  (t/testing "without comment"
    (let [http-response (sut/ping-post-handler {:parameters {:query {:ping "ping"}}})]
      (t/is (= 200 (:status http-response)))
      (t/is (= "pong" (-> http-response :body :pong)))
      (t/is (nil? (-> http-response :body :comment))))))
