(ns picture-gallery.infrastructure.sql.migrate
  (:require [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [integrant.core :as ig]
            [taoensso.timbre :as timbre]))

(defn build-config [database-options migration-folder]
  (let [{:keys [adapter database-name username password server-name port-number]} database-options]
    {:datastore (jdbc/sql-database {:dbtype adapter
                                    :dbname database-name
                                    :user username
                                    :password password
                                    :port port-number
                                    :host server-name})
     :migrations (jdbc/load-resources migration-folder)}))

(defmethod ig/init-key ::migration [_ {:keys [env operation rollback-amount]}]
  (let [{:keys [database-options migrations-folder]} env
        migration-config (build-config database-options migrations-folder)]
    (timbre/info "run migration with operation" operation "(rollback-amount is " rollback-amount ")")
    (condp = operation
      :migrate  (repl/migrate migration-config)
      :rollback (repl/rollback migration-config (or rollback-amount 1))
      (let [message  (str "invalid migration operation " operation " is not in #{:migrate :rollback}")]
        (timbre/error message)
        (throw (ex-info message {}))))
    {}))

;; (def config
;;   {:datastore (jdbc/sql-database {:dbtype "postgresql"
;;                                   :dbname "pic_gallery"
;;                                   :user "meguru"
;;                                   :password "emacs"
;;                                   :port 5432
;;                                   :host "dev_db"})
;;    :migrations (jdbc/load-resources "migrations")})

;;  (repl/migrate config)
;;  (repl/rollback config 1)
