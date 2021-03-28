(ns picture-gallery.interface.gateway.image-db.pics-service
  (:import (java.io File))
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.pics :as pics-domain]
            [integrant.core :as ig]
            [clojure.java.io :as io]))

(defprotocol Pics
  (get-pic-image [image-db blob])
  (save-pic-image [image-db ^File image])
  (delete-pic-image [image-db blob]))

(defn pics-service? [inst]
  (satisfies? Pics inst))

(s/def ::pics-service pics-service?)

(s/fdef get-pic-image
  :args (s/cat :image-db ::pics-service
               :blob ::pics-domain/image-url)
  :ret (s/or :exist ::pics-domain/image-file
             :not-exist empty?))

(s/fdef save-pic-image
  :args (s/cat :image-db ::pics-service
               :image ::pics-domain/image-file)
  :ret ::pics-domain/image-url)

(s/fdef delete-pic-image
  :args (s/cat :image-db ::pics-service
               :blob ::pics-domain/image-url)
  :ret (s/and int? (partial <= 0)))

;; (def system (ig/init {:picture-gallery.infrastructure.env/env {}
;;                       :picture-gallery.infrastructure.image-db.core/image-db {:env (ig/ref :picture-gallery.infrastructure.env/env)}}))

;; (def image-db
;;   (:picture-gallery.infrastructure.image-db.core/image-db system))

;; (def sample-pic (io/file (io/resource "sample.jpg")))

;; (def image-id (save-pic-image image-db sample-pic))

;; (get-pic-image
;;  image-db
;;  image-id)

;; (delete-pic-image
;;  image-db
;;  image-id)
