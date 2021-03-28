(ns picture-gallery.interface.presenter.api.pic-image-get
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.openapi.pics :as pics-openapi]
            [picture-gallery.domain.openapi.base :as base-openapi]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.error :as error-domain]
            [clojure.java.io :as io]))

(s/def ::body ::pics-openapi/pic-image-get-response)
(s/def ::http-output-data (s/keys :req-un [::base-openapi/status ::body]))
(s/fdef ->http
  :args (s/cat :args
               (s/or :success (s/tuple ::pics-domain/pic-image-get-output nil?)
                     :failure (s/tuple nil? ::error-domain/error)))
  :ret (s/or :success ::http-output-data
             :failure ::error-domain/error))

(defn ->http "
  usecase output model -> http response
  "
  [[output-data error]]
  (let [{:keys [image-file]} output-data
        output-model image-file]
    (if (nil? error)
      {:status 200
       :headers {"Content-Type" "image/png"}
       :body (io/input-stream output-model)}
      error)))
