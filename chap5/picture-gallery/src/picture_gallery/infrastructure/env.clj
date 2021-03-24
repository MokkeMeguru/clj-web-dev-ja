(ns picture-gallery.infrastructure.env
  (:require [environ.core :refer [env]]
            [integrant.core :as ig]
            [orchestra.spec.test :as st]
            [clojure.spec.alpha :as s])
  (:import (com.google.auth.oauth2 GoogleCredentials)))

(s/fdef decode-log-level
  :args (s/cat :str-log-level string?)
  :ret #{:trace :debug :info :warn :error :fatal :report})

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

(defn get-database-options []
  {:adapter (env :database-adapter)
   :database-name (env :database-name)
   :username (env :database-username)
   :password (env :database-password)
   :server-name (env :database-server-name)
   :port-number (Integer/parseInt (env :database-port-number))})

(defmethod ig/init-key ::env [_ _]
  (println "loading environment via environ")
  (let [database-options (get-database-options)
        running (env :env)
        migrations-folder (env :migrations-folder)
        log-level (decode-log-level (env :log-level))
        local-image-db-parent-dir (env :local-image-db-parent-dir)]
    (println "running in " running)
    (println "log-level " log-level)
    (println "migrations-folder " migrations-folder)
    (println "database options " database-options)
    (println "local image-db parent dir " local-image-db-parent-dir)
    (when (.contains ["test" "dev"] running)
      (println "orchestra instrument is active")
      (st/instrument))
    {:database-options database-options
     :running running
     :migrations-folder migrations-folder
     :log-level log-level
     :firebase-credentials (GoogleCredentials/getApplicationDefault)
     :local-image-db-parent-dir local-image-db-parent-dir}))

;; (defmethod ig/halt-key! ::env [_ _]
;;   nil)
