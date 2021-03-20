(ns picture-gallery.infrastructure.router.sample
  (:require [picture-gallery.domain.openapi.sample :as sample-openapi]
            [picture-gallery.interface.controller.api.sample.ping-post :as ping-post-controller]
            [picture-gallery.usecase.sample.ping-pong :as ping-pong-usecase]
            [picture-gallery.interface.presenter.api.sample.ping-post :as ping-post-presenter]
            [picture-gallery.utils.error :refer [err->>]]))

;; handlers
(defn ping-post-handler [input-data]
  (ping-post-presenter/->http
   (err->> input-data
           ping-post-controller/http->
           ping-pong-usecase/ping-pong)))

;; router
(defn sample-router []
  ["/samples"
   ["/ping"
    {:swagger {:tags ["sample_API"]}
     :post {:summary "ping - pong"
            :parameters {:query ::sample-openapi/post-ping-query-parameters}
            :responses {200 {:body ::sample-openapi/post-ping-response}}
            :handler ping-post-handler}}]])
