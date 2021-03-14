(ns picture-gallery.interface.controller.api.sample.ping-post
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.sample :as sample-domain]
            [picture-gallery.domain.error :as error-domain]
            [picture-gallery.domain.openapi.sample :as sample-openapi]))

(s/def ::query ::sample-openapi/post-ping-query-parameters)
(s/def ::parameters (s/keys :req-un [::query]))
(s/def ::http-input-data (s/keys :req-un [::parameters]))

(s/fdef http->
  :args (s/cat :input-data ::http-input-data)
  :ret (s/or :success (s/tuple ::sample-domain/ping-pong-input nil?)
             :failure (s/tuple nil? ::error-domain/error)))

(defn http-> "
  http request -> usecase input model
  "
  [input-data]
  (let [{:keys [parameters]} input-data
        {:keys [query]} parameters
        {:keys [ping comment]} query
        input-model   (cond-> {:ping (:ping query)}
                        comment (assoc :comment comment))
        conformed-input-model (s/conform
                               ::sample-domain/ping-pong-input
                               input-model)]
    (if (not= ::s/invalid conformed-input-model)
      [conformed-input-model nil]
      [nil (error-domain/input-data-is-invalid (s/explain-str ::sample-domain/ping-pong-input input-model))])))

;; (st/instrument)
;; (http-> {:parameters {:query {:ping "hello"}}})
;; (http-> {:parameters {:query {:ping "ping" :comment "hello"}}})
