(ns picture-gallery.interface.gateway.database.sql.users-repository
  (:require [picture-gallery.interface.gateway.database.sql.utils :as sql-utils]
            [picture-gallery.interface.gateway.database.users-repository :refer [Users]]))

(defn user-create-model->sql [{:keys [user-id id-token]}]
  {:id user-id
   :auth_token id-token})

(defn sql->user-model [{:keys [id auth_token created_at updated_at is_deleted]}]
  {:user-id id
   :id-token auth_token
   :created-at (sql-utils/sql-to-long created_at)
   :updated-at (when updated_at (sql-utils/sql-to-long updated_at))
   :is-deleted is_deleted})

(extend-protocol Users
  picture_gallery.infrastructure.sql.sql.Boundary

  (get-users [{:keys [spec]}]
    (->> (sql-utils/get-all spec :users)
         (mapv sql->user-model)))

  (get-user-by-user-id [{:keys [spec]} user-id]
    (let [sql-model (sql-utils/get-by-id spec :users :id user-id)]
      (if sql-model (sql->user-model sql-model) nil)))

  (get-exist-user-by-auth-token [{:keys [spec]} auth-token]
    (let [sql-model (first (sql-utils/find-by-m spec :users {:auth_token auth-token :is_deleted false}))]
      (if sql-model (sql->user-model sql-model) nil)))

  (create-user [{:keys [spec]} user-create-model]
    (sql->user-model
     (sql-utils/insert! spec :users
                        (user-create-model->sql user-create-model))))

  (delete-user [{:keys [spec]} user-id logical?]
    (if logical?
      (sql-utils/logical-delete! spec :users {:id user-id})
      (sql-utils/physical-delete! spec :users {:id user-id}))))
