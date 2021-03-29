(ns pic-gallery-web.routers-test
  (:require [pic-gallery-web.routers :as sut]
            [pic-gallery-web.domain.routes :as routes-domain]
            [reitit.frontend :as rf]
            [cljs.test :as t :include-macros true]))

(t/deftest route-match
  (t/testing "home"
    (t/is ::routes-domain/home (-> (rf/match-by-path sut/router "/") :data :name))))
