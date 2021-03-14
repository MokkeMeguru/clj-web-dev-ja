(ns picture-gallery.interface.presenter.api.ping-post-test
  (:require [picture-gallery.interface.presenter.api.ping-post :as sut]
            [clojure.test :as t]))

(t/deftest ->http
  (t/testing "invalid data"
    (let [response (sut/->http [nil {:status 422 :body {:code 1 :message "err"}}])]
      (t/is (= 422 (:status response)))
      (t/is (= 1 (-> response :body :code)))
      (t/is (= "err" (-> response :body :message)))))
  (t/testing "correct data (w comment)"
    (let [response (sut/->http [{:pong "pong" :message "hello"} nil])]
      (t/is (= 200 (:status response)))
      (t/is (= "pong" (-> response :body :pong)))
      (t/is (= "hello" (-> response :body :message)))))
  (t/testing "correct data (w/o comment)"
    (let [response (sut/->http [{:pong "pong"} nil])]
      (t/is (= 200 (:status response)))
      (t/is (= "pong" (-> response :body :pong)))
      (t/is (nil? (-> response :body :message))))))
