(ns picture-gallery.infrastructure.server
  (:require [taoensso.timbre :as timbre]
            [ring.adapter.jetty :as jetty]
            [integrant.core :as ig]))

(defmethod ig/init-key ::server [_ {:keys [env router port]}]
  (timbre/info "server is running in port" port)
  (timbre/info "router is " router)
  (jetty/run-jetty router {:port port :join? false}))

(defmethod ig/halt-key! ::server [_ server]
  (timbre/info "stop server")
  (.stop server))
