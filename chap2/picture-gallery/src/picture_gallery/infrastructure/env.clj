(ns picture-gallery.infrastructure.env
  (:require [environ.core :refer [env]]
            [integrant.core :as ig]
            [orchestra.spec.test :as st]))

(defn decode-log-level [str-log-level]
  (condp = str-log-level
    "trace" :trace
    "debug" :debug
    "info" :info
    "warn" :warn
    "error" :error
    "fatal" :fatal
    "report" :report
    :info))

(defmethod ig/init-key ::env [_ _]
  (println "loading environment via environ")
  (let [database-url (env :database-url)
        running (env :env)
        log-level (decode-log-level (env :log-level))]
    (println "running in " running)
    (println "database-url " database-url)
    (println "log-level " log-level)
    (when (.contains ["test" "dev"] running)
      (println "orchestra instrument is active")
      (st/instrument))
    {:database-url database-url
     :running running
     :log-level log-level}))

(defmethod ig/halt-key! ::env [_ _]
  {})
