(ns picture-gallery.interface.gateway.database.sql.pics-repository
  (:require [picture-gallery.interface.gateway.database.pics-repository :refer [Pics]]
            [picture-gallery.interface.gateway.database.sql.utils :as sql-utils]
            [next.jdbc :as jdbc]
            [clojure.string]
            [next.jdbc.sql :as njs]))

(def sql-basic-selection
  "SELECT * FROM pics INNER JOIN pic_image ON (pics.id = pic_image.id)")

(extend-protocol Pics
  picture_gallery.infrastructure.sql.sql.Boundary

  (get-pics [{:keys [spec]}]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute! conn [(clojure.string/join " " [sql-basic-selection "limit 100"])]
                          sql-utils/default-jdbc-option)
           (mapv #(into {} %)))))

  (get-pics-by-user [{:keys [spec]} user-id page-id]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute! conn [(clojure.string/join
                                 " "
                                 [sql-basic-selection
                                  "where" "user_id = ?" "limit 20" "offset" (str (* 20 (dec page-id)))])
                                user-id]
                          sql-utils/default-jdbc-option)
           (mapv #(into {} %)))))

  (get-pic [{:keys [spec]} pic-id]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (->> (jdbc/execute! conn [(clojure.string/join
                                 " "
                                 sql-basic-selection
                                 "where" "id = ?")
                                pic-id]
                          sql-utils/default-jdbc-option)
           (into {}))))

  (create-pic [{:keys [spec]} pic-create-model state]
    (let [sql-pic-create-model (dissoc pic-create-model :image-urls)
          sql-pic-image-urls (:image-urls pic-create-model)]
      (jdbc/with-transaction [tx (:datasource spec)]
        (let [pic-result (njs/insert! tx :pics sql-pic-create-model sql-utils/default-jdbc-option)
              pic-image-result (njs/insert-multi! tx :pic_images sql-pic-image-urls)]))))

  (update-pic-state [{:keys [spec]} pic-id state])

  (delete-pic [{:keys [spec]} pic-id logical?]))
