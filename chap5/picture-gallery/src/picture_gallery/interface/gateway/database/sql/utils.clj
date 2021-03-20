(ns picture-gallery.interface.gateway.database.sql.utils
  (:require
   [next.jdbc :as jdbc]
   [clj-time.core :as t]
   [clj-time.coerce :as tc]
   [next.jdbc.sql :as njs]
   [next.jdbc.result-set :as rs]
   [clojure.spec.alpha :as s]))

;; for datetime
(s/fdef long-to-sql
  :args (s/cat :long-time pos-int?)
  :ret  (partial instance? java.sql.Timestamp))

(s/fdef sql-to-long
  :args (s/cat :sql-time (partial instance? java.sql.Timestamp))
  :ret  pos-int?)

(s/fdef sql-now
  :ret (partial instance? java.sql.Timestamp))

(defn long-to-sql [long-time]
  (-> long-time
      tc/from-long
      tc/to-sql-time))

(defn sql-to-long [sql-time]
  (-> sql-time
      tc/from-sql-time
      tc/to-long))

(defn sql-now []
  (tc/to-sql-time (t/now)))

;; sql helper
(def default-jdbc-option
  {:return-keys true :builder-fn rs/as-unqualified-maps})

(defn get-all
  ([spec table-key]
   (get-all spec table-key false))
  ([spec table-key limit]
   (with-open [conn (jdbc/get-connection (:datasource spec))]
     (->> (jdbc/execute! conn [(str "SELECT * FROM " (name table-key) (when limit " limit 100"))] default-jdbc-option)
          (mapv #(into {} %))))))

(defn insert! [spec table-key m]
  (with-open [conn (jdbc/get-connection (:datasource spec))]
    (njs/insert! conn table-key m default-jdbc-option)))

(defn update! [spec table-key m idm]
  (with-open [conn (jdbc/get-connection (:datasource spec))]
    (:next.jdbc/update-count (njs/update! conn table-key (assoc m :updated_at (sql-now)) idm))))

(defn physical-delete! [spec table-key idm]
  (with-open [conn (jdbc/get-connection (:datasource spec))]
    (:next.jdbc/update-count (njs/delete! conn table-key idm))))

(defn logical-delete! [spec table-key idm]
  (update! spec table-key {:is_deleted true} idm))

(defn find-by-m [spec table-key m]
  (with-open [conn (jdbc/get-connection (:datasource spec))]
    (njs/find-by-keys conn table-key m default-jdbc-option)))

(defn get-by-id [spec table-key primary-key primary-key-value]
  (with-open [conn (jdbc/get-connection (:datasource spec))]
    (njs/get-by-id conn table-key primary-key-value primary-key default-jdbc-option)))
