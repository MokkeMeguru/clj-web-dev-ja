(ns picture-gallery.infrastructure.server-test
  (:require [picture-gallery.infrastructure.server :as sut]
            [clojure.test :as t]
            [integrant.core :as ig]
            [reitit.ring :as ring]))

(t/deftest integrant-server
  (t/testing "integrate by some keys"
    (let [template-router (ring/ring-handler
                           (ring/router
                            [["/api"]]
                            {})
                           (ring/routes (ring/create-default-handler)))
          system (ig/init {:picture-gallery.infrastructure.server/server
                           {:env {}
                            :port 3034
                            :router template-router}})]
      (t/is (instance? org.eclipse.jetty.server.Server
                       (:picture-gallery.infrastructure.server/server system)))
      (t/is (nil? (ig/halt! system))))))
