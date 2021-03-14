(ns picture-gallery.infrastructure.router.core-test
  (:require [picture-gallery.infrastructure.router.core :as sut]
            [clojure.test :as t]
            [integrant.core :as ig]))

(t/deftest app
  (t/testing "unknown endpoint"
    (let [response ((sut/app) {:request-method :get
                               :uri "/api/unknown-path"
                               :query-params {:x "1" :y 2}})]
      (t/is (map? response))
      (t/is (= 404 (:status response)))
      (t/is (= "" (:body response))))))

(t/deftest integrant-router
  (t/testing "integrate by some keys"
    (let [system (ig/init {:picture-gallery.infrastructure.env/env {}
                           :picture-gallery.infrastructure.router.core/router
                           {:env (ig/ref :picture-gallery.infrastructure.env/env)}})]
      (t/is (map? system))
      (t/is (nil? (ig/halt! system))))))
