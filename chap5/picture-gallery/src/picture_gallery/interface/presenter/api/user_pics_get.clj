(ns picture-gallery.interface.presenter.api.user-pics-get
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.openapi.pics :as pics-openapi]
            [picture-gallery.domain.openapi.base :as base-openapi]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.user-pics :as user-pics-domain]
            [picture-gallery.domain.error :as error-domain]
            [orchestra.spec.test :as st]))

(s/def ::body ::pics-openapi/user-pics-get-response)
(s/def ::http-output-data (s/keys :req-un [::base-openapi/status ::body]))
(s/fdef ->http
  :args (s/cat :args
               (s/or :success (s/tuple ::user-pics-domain/user-pics-get-output nil?)
                     :failure (s/tuple nil? ::error-domain/error)))
  :ret (s/or :success ::http-output-data
             :failure ::error-domain/error))

(defn ->http "
  usecase output model -> http request
"
  [[output-data error]]
  (if (nil? error)
    {:status 200
     :body (mapv
            (fn [{:keys [user-id pic-id image-urls title created-at description]}]
              (cond->
               {:id (.toString pic-id)
                :title title
                :created_at created-at
                :image-urls image-urls}
                description (assoc :description description)))
            output-data)}
    error))

;; (st/instrument)

;; (def output-data-error [[{:user-id "123123123123123" :pic-id (java.util.UUID/randomUUID)
;;                           :image-urls ["sample.png"]
;;                           :title "Hello"
;;                           :created-at 120}] nil])

;; (->http output-data-error)

;; (s/explain
;;  ::user-pics-domain/user-pics-get-output
;;  (first output-data-error))
