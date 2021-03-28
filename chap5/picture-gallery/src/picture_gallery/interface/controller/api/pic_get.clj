(ns picture-gallery.interface.controller.api.pic-get
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.error :as error-domain]))

(defn http-> "
  http request -> usecase input model
  "
  [input-data]
  (let [{:keys [parameters]} input-data
        {{:keys [pic-id]} :path} parameters
        input-model {:pic-id (java.util.UUID/fromString pic-id)}
        conformed-input-model (s/conform
                               ::pics-domain/pic-get-input
                               input-model)]
    (if (not= ::s/invalid conformed-input-model)
      [conformed-input-model nil]
      [nil (error-domain/input-data-is-invalid
            (s/explain-str ::pics-domain/pic-get-input input-model))])))
