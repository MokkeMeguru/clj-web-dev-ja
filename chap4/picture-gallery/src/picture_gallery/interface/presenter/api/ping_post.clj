(ns picture-gallery.interface.presenter.api.ping-post
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.sample :as sample-domain]
            [picture-gallery.domain.openapi.sample :as sample-openapi]
            [picture-gallery.domain.openapi.base :as base-openapi]
            [picture-gallery.domain.error :as error-domain]
            [orchestra.spec.test :as st]))

(s/def ::body ::sample-openapi/post-ping-response)
(s/def ::http-output-data (s/keys :req-un [::base-openapi/status ::body]))
(s/fdef ->http
  :args (s/cat :arg
               (s/or :success (s/tuple ::sample-domain/ping-pong-output nil?)
                     :failure (s/tuple nil? ::error-domain/error)))
  :ret (s/or :success ::http-output-data
             :failure ::error-domain/error))

(defn ->http "
  usecase output model -> http response
  "
  [[output-data error]]
  (if (nil? error)
    {:status 200
     :body output-data}
    error))

;; (st/instrument)
;; (->http [{:pong "pong"} nil])
