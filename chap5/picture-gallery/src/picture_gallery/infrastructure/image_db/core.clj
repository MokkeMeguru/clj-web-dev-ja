(ns picture-gallery.infrastructure.image-db.core
  (:require [integrant.core :as ig]))

(defrecord LocalImageDBBoundary [image-db])

(defmethod ig/init-key ::image-db
  [_ {:keys [env]}]
  (let [parent-dir (:local-image-db-parent-dir env)]
    (->LocalImageDBBoundary {:parent-dir parent-dir})))
