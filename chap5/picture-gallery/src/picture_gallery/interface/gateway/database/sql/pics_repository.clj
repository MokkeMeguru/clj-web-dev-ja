(ns picture-gallery.interface.gateway.database.sql.pics-repository
  (:require [picture-gallery.interface.gateway.database.pics-repository :refer [Pics]]
            [picture-gallery.interface.gateway.database.sql.utils :as sql-utils]
            [next.jdbc :as jdbc]
            [clojure.string]
            [next.jdbc.sql :as njs]
            [next.jdbc.types :refer [as-other]]
            [clojure.spec.alpha :as s]))

(defn pic-create-model->sql [{:keys [user-id title description]} state]
  (cond->
   {:user_id user-id
    :title title
    :tcc_state (as-other (name state))}
    description (assoc :description description)))

(defn pic-image-urls->sql [image-urls]
  (vec (map-indexed  (fn [idx image-url]
                       {:blob image-url
                        :index idx}) image-urls)))

(defn sql->pic-model [sql-pic sql-pic-image]
  (let [{:keys [id user_id title description created_at updated_at is_deleted tcc_state]} sql-pic
        image-urls (mapv #(:blob %) (sort-by :index sql-pic-image))]
    (if-not id
      nil
      (cond->
       {:pic-id id
        :user-id user_id
        :title title
        :image-urls image-urls
        :created-at (sql-utils/sql-to-long created_at)
        :is-deleted is_deleted
        :tcc-state tcc_state}
        description (assoc :description description)
        updated_at (assoc :updated-at (sql-utils/sql-to-long updated_at))))))

(def sql-basic-selection
  "SELECT * FROM pics INNER JOIN pic_images ON (pics.id = pic_images.id)")

(extend-protocol Pics
  picture_gallery.infrastructure.sql.sql.Boundary

  (get-pics [{:keys [spec]}]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (let [pics (jdbc/execute! conn [(clojure.string/join " " [sql-basic-selection "limit 100"])] sql-utils/default-jdbc-option)
            pics-images (mapv #(jdbc/execute! conn ["SELECT * FROM pic_images WHERE id = ?" (:id %)] sql-utils/default-jdbc-option) pics)]
        (mapv sql->pic-model pics pics-images))))

  (get-pics-by-user [{:keys [spec]} user-id page-id]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (let [sql-offset (* 20 (dec page-id))
            pics (jdbc/execute! conn ["SELECT * FROM pics WHERE user_id = ? AND is_deleted = false AND tcc_state = ? limit 20 offset ?" user-id (as-other "confirm") sql-offset] sql-utils/default-jdbc-option)
            pics-head-images (mapv #(jdbc/execute! conn ["SELECT * FROM pic_images WHERE id = ? AND index = 0" (:id %)] sql-utils/default-jdbc-option) pics)]
        (mapv sql->pic-model pics pics-head-images))))

  (get-pic [{:keys [spec]} pic-id]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (sql->pic-model
       (jdbc/execute-one! conn ["SELECT * FROM pics WHERE id = ? AND is_deleted = false AND tcc_state = ?" pic-id (as-other "confirm")] sql-utils/default-jdbc-option)
       (jdbc/execute! conn ["SELECT * FROM pic_images WHERE id = ?" pic-id] sql-utils/default-jdbc-option))))

  (create-pic [{:keys [spec]} pic-create-model state]
    (let [sql-pic-create-model (pic-create-model->sql pic-create-model state)
          sql-pic-image-urls (pic-image-urls->sql (:image-urls pic-create-model))]
      (jdbc/with-transaction [tx (:datasource spec)]
        (let [pic-id (loop [pic-id (java.util.UUID/randomUUID) retry 0]
                       (cond
                         (> retry 10) (throw (ex-info "pic's unique random uuid generation failed" {:pic-create-model pic-create-model}))
                         (nil? (jdbc/execute-one! tx ["SELECT * FROM pics WHERE id = ?" pic-id])) pic-id
                         :else (recur (java.util.UUID/randomUUID) (inc retry))))
              pic-result (njs/insert! tx :pics (assoc sql-pic-create-model :id pic-id) sql-utils/default-jdbc-option)
              pic-image-result (njs/insert-multi! tx :pic_images [:blob :id :index] (mapv (fn [{:keys [blob index]}] [blob pic-id index]) sql-pic-image-urls) sql-utils/default-jdbc-option)]
          [(sql->pic-model pic-result pic-image-result) (keyword (:tcc_state pic-result))]))))

  (update-pic-state [{:keys [spec]} pic-id state]
    (sql-utils/update! spec :pics {:tcc_state (as-other (name state))} {:id pic-id}))

  (delete-pic [{:keys [spec]} pic-id logical?]
    (if logical?
      (sql-utils/logical-delete! spec :pics {:id pic-id})
      (sql-utils/physical-delete! spec :pics {:id pic-id}))))
