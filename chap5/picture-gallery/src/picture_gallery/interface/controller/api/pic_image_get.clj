(ns picture-gallery.interface.controller.api.pic-image-get
  (:require [clojure.spec.alpha :as s]
            [clojure.string]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.error :as error-domain]))

(defn http-> "
  http request -> usecase input model
  "
  [input-data]
  (let [{:keys [parameters]} input-data
        {{:keys [image-id]} :path} parameters
        image-id (clojure.string/replace image-id ".png" "")
        input-model (cond->
                     {:image-url image-id})
        conformed-input-model (s/conform
                               ::pics-domain/pic-image-get-input
                               input-model)]
    (if (not= ::s/invalid conformed-input-model)
      [conformed-input-model nil]
      [nil (error-domain/input-data-is-invalid
            (s/explain-str ::pics-domain/pic-image-get-input input-model))])))
