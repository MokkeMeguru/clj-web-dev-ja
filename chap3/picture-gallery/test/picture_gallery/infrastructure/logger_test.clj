(ns picture-gallery.infrastructure.logger-test
  (:require [picture-gallery.infrastructure.logger :as sut]
            [clojure.test :as t]
            [integrant.core :as ig]))

(t/deftest integrant-logger
  (t/testing "integrate by some keys"
    (let [system (ig/init {:picture-gallery.infrastructure.env/env {}
                           :picture-gallery.infrastructure.logger/logger {:env (ig/ref :picture-gallery.infrastructure.env/env)}})
          init-result (:picture-gallery.infrastructure.logger/logger system)]
      (t/is (= {} init-result))
      (t/is (nil? (ig/halt! system))))))
