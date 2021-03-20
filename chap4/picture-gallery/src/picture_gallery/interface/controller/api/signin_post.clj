(ns picture-gallery.interface.controller.api.signin-post
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :as w]
            [picture-gallery.domain.auth :as auth-domain]
            [picture-gallery.domain.error :as error-domain]))

(defn http-> "
  http request -> usecase input model
  "
  [input-data]
  (let [{:keys [headers]} input-data
        {:keys [authorization]} (w/keywordize-keys headers)
        input-model {:encrypted-id-token authorization}
        conformed-input-model (s/conform
                               ::auth-domain/signin-input
                               input-model)]
    (if (not= ::s/invalid conformed-input-model)
      [conformed-input-model nil]
      [nil (error-domain/input-data-is-invalid (s/explain-str ::auth-domain/signin-input input-model))])))

;; (http-> {:headers {"authorization" "hello world"}})
;; (http-> {:headers {"authorization" nil}})
