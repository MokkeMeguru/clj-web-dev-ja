(ns picture-gallery.interface.presenter.api.signin-post
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.openapi.auth :as auth-openapi]
            [picture-gallery.domain.openapi.base :as base-openapi]
            [picture-gallery.domain.auth :as auth-domain]
            [picture-gallery.domain.error :as error-domain]))

(s/def ::body ::auth-openapi/signin-response)
(s/def ::http-output-data (s/keys :req-un [::base-openapi/status ::body]))
(s/fdef ->http
  :args (s/cat :args
               (s/or :success (s/tuple ::auth-domain/signin-output nil?)
                     :failure (s/tuple nil? ::error-domain/error)))
  :ret (s/or :success ::http-output-date
             :failure ::error-domain/error))

(defn ->http "
  usecase output model -> http response
  "
  [[output-data error]]
  (println output-data error)
  (if (nil? error)
    {:status 201
     :body output-data}
    error))
