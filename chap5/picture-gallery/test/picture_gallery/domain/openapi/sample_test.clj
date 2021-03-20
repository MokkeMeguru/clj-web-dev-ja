(ns picture-gallery.domain.openapi.sample-test
  (:require
   [picture-gallery.domain.openapi.sample :as sut]
   [clojure.test :as t]
   [clojure.spec.alpha :as s]))

(t/deftest ping
  (t/is (s/valid? ::sut/ping "ping")))

(t/deftest pong
  (t/is  (s/valid? ::sut/pong "pong")))

(t/deftest _comment
  (t/is  (s/valid? ::sut/comment "comment")))

(t/deftest post-ping-query-parameters
  (t/testing "invalid value"
    (t/is (not (s/valid? ::sut/post-ping-query-parameters nil))))
  (t/testing "comment exist"
    (t/is (not (s/valid? ::sut/post-ping-query-parameters {:ping 123})))
    (t/is (s/valid? ::sut/post-ping-query-parameters {:ping "ping"})))
  (t/testing "comment not exist"
    (t/is (not (s/valid? ::sut/post-ping-query-parameters {:ping "ping"
                                                           :comment 123})))
    (t/is (s/valid? ::sut/post-ping-query-parameters {:ping "ping"
                                                      :comment "comment"}))))

(t/deftest post-ping-response
  (t/testing "invalid value"
    (t/is (not (s/valid? ::sut/post-ping-response nil))))
  (t/testing "comment exist"
    (t/is (not (s/valid? ::sut/post-ping-response  {:pong 123})))
    (t/is (s/valid? ::sut/post-ping-response  {:pong "pong"})))
  (t/testing "comment not exist"
    (t/is (not (s/valid? ::sut/post-ping-response {:pong "pong"
                                                   :comment 123})))
    (t/is (s/valid? ::sut/post-ping-response {:pong "pong"
                                              :comment "comment"}))))
