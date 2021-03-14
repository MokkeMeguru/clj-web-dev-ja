(ns picture-gallery.infrastructure.logger
  (:require [taoensso.timbre :as timbre]
            [integrant.core :as ig]))

(defmethod ig/init-key ::logger [_ {:keys [env]}]
  (println "set logger with log-level" (:log-level env))
  (timbre/set-level! (:log-level env))
  {})
