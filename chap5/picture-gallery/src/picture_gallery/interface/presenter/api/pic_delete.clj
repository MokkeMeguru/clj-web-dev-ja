(ns picture-gallery.interface.presenter.api.pic-delete
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.openapi.base :as base-openapi]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.error :as error-domain]))

(s/def ::http-output-data (s/keys :req-un [::base-openapi/status]))
(s/fdef ->http
  :args (s/cat :args
               (s/or :success (s/tuple ::pics-domain/pic-delete-output nil?)
                     :failure (s/tuple nil? ::error-domain/error)))
  :ret (s/or :success ::http-output-data
             :failure ::error-domain/error))

(defn ->http "
  usecase output model -> http response
  "
  [[_ error]]
  (if (nil? error)
    {:status 204}
    error))
