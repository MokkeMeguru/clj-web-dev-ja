(ns picture-gallery.interface.presenter.api.pic-get
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.openapi.pics :as pics-openapi]
            [picture-gallery.domain.openapi.base :as base-openapi]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.error :as error-domain]))

(s/def ::body ::pics-openapi/pic-get-response)
(s/def ::http-output-data (s/keys :req-un [::base-openapi/status ::body]))
(s/fdef ->http
  :args (s/cat :args
               (s/or :success (s/tuple ::pics-domain/pic-get-output nil?)
                     :failure (s/tuple nil? ::error-domain/error)))
  :ret (s/or :success ::http-output-data
             :failure ::error-domain/error))

(defn ->http "
  usecase output model -> http response
  "
  [[output-data error]]
  (let [{:keys [pic-id user-id title image-urls created-at description]} output-data
        output-model (cond-> {:id (.toString pic-id) :user-id user-id :title title
                              :image-urls image-urls :created-at created-at}
                       description (assoc :description description))]
    (if (nil? error)
      {:status 200
       :body output-model}
      error)))
