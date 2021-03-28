(ns picture-gallery.interface.controller.api.user-pics-get
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.user-pics :as user-pics-domain]
            [picture-gallery.domain.error :as error-domain]))

(defn http-> "
  http request -> usecase input model
  "
  [input-data]
  (let [{:keys [parameters]} input-data
        {{:keys [user-id]} :path} parameters
        {{:keys [page-id]} :query} parameters
        input-model (cond->
                     {:user-id user-id
                      :page-id page-id})
        conformed-input-model (s/conform
                               ::user-pics-domain/user-pics-get-input
                               input-model)]
    (if (not= ::s/invalid conformed-input-model)
      [conformed-input-model nil]
      [nil (error-domain/input-data-is-invalid (s/explain-str ::user-pics-domain/user-pics-get-input input-model))])))

;; (http->
;;  {:parameters {:path {:user-id "313536300183910"}
;;                :query {:page-id 1}}})
