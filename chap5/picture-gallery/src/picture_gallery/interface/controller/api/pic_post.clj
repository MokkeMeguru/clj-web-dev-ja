(ns picture-gallery.interface.controller.api.pic-post
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
        {{:keys [title description images]} :multipart} parameters
        input-model (cond->
                     {:encrypted-id-token authorization
                      :image-files (mapv :tempfile images)
                      :title title}
                      description (assoc :description description))
        conformed-input-model (s/conform
                               ::pics-domain/pic-post-input
                               input-model)]
    (if (not= ::s/invalid conformed-input-model)
      [conformed-input-model nil]
      [nil (error-domain/input-data-is-invalid (s/explain-str ::pics-domain/pic-post-input input-model))])))
