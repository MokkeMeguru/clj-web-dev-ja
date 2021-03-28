(ns picture-gallery.interface.controller.api.pic-delete
  (:require [clojure.walk :as w]
            [clojure.spec.alpha :as s]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.error :as error-domain]))

(defn http-> "
  http request -> usecase input model
  "
  [input-data]
  (let [{:keys [headers parameters]} input-data
        {:keys [authorization]} (w/keywordize-keys headers)
        {{:keys [pic-id]} :path} parameters
        input-model {:pic-id (java.util.UUID/fromString pic-id)
                     :encrypted-id-token authorization}
        conformed-input-model (s/conform
                               ::pics-domain/pic-delete-input
                               input-model)]
    (if (not= ::s/invalid conformed-input-model)
      [conformed-input-model nil]
      [nil (error-domain/input-data-is-invalid
            (s/explain-data ::pics-domain/pic-delete-input input-model))])))
