(ns picture-gallery.interface.gateway.image-db.local.pics-service
  (:require [picture-gallery.interface.gateway.image-db.pics-service :refer [Pics]]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]))

(extend-protocol Pics
  picture_gallery.infrastructure.image_db.core.LocalImageDBBoundary

  (get-pic-image [{{:keys [parent-dir]} :image-db} blob]
    (let [file (io/file parent-dir "pic" blob)]
      (if (.isFile file) file nil)))

  (save-pic-image [{{:keys [parent-dir]} :image-db} image]
    (try
      ;; check duplicate
      (loop [blob (java.util.UUID/randomUUID)
             retry 0]
        (let [file (io/file parent-dir "pic" (.toString blob))]
          (cond
            (> retry 10) (throw (ex-info "save pic's image failed: at apply unique random uuid"))
            (and file (.isFile file)) (recur (java.util.UUID/randomUUID) (inc retry))
            :else (do (io/copy image file)
                      (.toString blob)))))
      (catch java.io.IOException e
        (timbre/error "Pics save image Error: " (.getMessage e))
        (throw (ex-info "failed to save image" {:parent-dir parent-dir :image image})))))

  (delete-pic-image [{{:keys [parent-dir]} :image-db} blob]
    (try
      (io/delete-file (io/file parent-dir "pic" blob)) 1
      (catch Exception e
        (timbre/warn "Pics delete image Error: " (.getMessage e)) 0))))
