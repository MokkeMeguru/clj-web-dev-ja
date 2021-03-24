(ns picture-gallery.interface.gateway.database.pics-repository
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.users :as users-domain]
            [picture-gallery.domain.base :as base-domain]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [orchestra.spec.test :as st]))

(defprotocol Pics
  (get-pics [db])
  (get-pics-by-user [db user-id page-id])
  (get-pic [db pic-id])
  (get-blob [db blob])
  (create-pic [db pic-create-model state])
  (update-pic-state [db pic-id state])
  (delete-pic [db pic-id logical?]))

(defn pics-repository? [inst]
  (satisfies? Pics inst))

(s/def ::pics-repository pics-repository?)

(s/fdef get-pics
  :args (s/cat :db ::pics-repository)
  :ret ::pics-domain/pics-model)

(s/fdef get-pics-by-user
  :args (s/cat :db ::pics-repository
               :user-id ::users-domain/user-id
               :page-id pos-int?)
  :ret ::pics-domain/pics-model)

(s/fdef get-pic
  :args (s/cat :db ::pics-repository
               :pic-id ::pics-domain/pic-id)
  :ret (s/or :exist ::pics-domain/pic-model
             :not-exist empty?))

(s/fdef get-blob
  :args (s/cat :db ::pics-repository
               :blob ::pics-domain/image-url))

(s/fdef create-pic
  :args (s/cat :db ::pics-repository
               :pic-create-model ::pics-domain/pic-create-model
               :state ::base-domain/tcc-flag)
  :ret (s/tuple ::pics-domain/pic-model ::base-domain/tcc-flag))

(s/fdef update-pic-state
  :args (s/cat :db ::pics-repository
               :pic-id ::pics-domain/pic-id
               :state ::base-domain/tcc-flag)
  :ret (s/and int? (partial <= 0)))

(s/fdef delete-pic
  :args (s/cat :db ::pics-repository
               :pic-id ::pics-domain/pic-id
               :logical? boolean?)
  :ret (s/and int? (partial <= 0)))

;; (st/instrument)

;; (def system (ig/init {:picture-gallery.infrastructure.env/env {}
;;                       :picture-gallery.infrastructure.sql.sql/sql {:env (ig/ref :picture-gallery.infrastructure.env/env)}}))

;; (def sample-user {:user-id "000000000000000" :id-token "sample-token"})

;; (s/valid? ::pics-domain/pic-create-model
;;           {:user-id "000000000000000"
;;            :pic-id (java.util.UUID/randomUUID)
;;            :image-files [(io/file (io/resource "sample.jpg"))]
;;            :title "Sample"
;;            :description "Description"})

;; (create-pic
;;  (:picture-gallery.infrastructure.sql.sql/sql system)
;;  {:user-id "000000000000000"
;;   :pic-id (java.util.UUID/randomUUID)
;;   :image-files [(io/file (io/resource "sample.jpg"))]
;;   :title "Sample"
;;   :description "Description"})

;; (get-pics
;;  (:picture-gallery.infrastructure.sql.sql/sql system))

;; (get-pics-by-user
;;  (:picture-gallery.infrastructure.sql.sql/sql system)
;;  "000000000000000"
;;  1)
