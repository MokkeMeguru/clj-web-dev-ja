(ns picture-gallery.interface.presenter.api.pic-post
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.openapi.pics :as pics-openapi]
            [picture-gallery.domain.openapi.base :as base-openapi]
            [picture-gallery.domain.error :as error-domain]))

(s/def ::body ::pics-openapi/pics-post-response)
(s/def ::http-output-data (s/keys :req-un [::base-openapi/status ::body]))
(s/fdef ->http
  :args (s/cat :args
               (s/or :success (s/tuple ::pics-domain/pic-post-output nil?)
                     :failure (s/tuple nil? ::error-domain/error)))
  :ret (s/or :success ::http-output-data
             :failure ::error-domain/error))
(defn ->http "
  usecase output model -> http response
  "
  [[output-model error]]
  (if (nil? error)
    {:status 200
     :body  {:id (.toString (:pic-id output-model))}}
    error))
