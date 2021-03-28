(ns picture-gallery.interface.gateway.database.pics-repository
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.users :as users-domain]
            [picture-gallery.domain.base :as base-domain]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [orchestra.spec.test :as st]
            [next.jdbc :as jdbc]))

(defprotocol Pics
  (get-pics [db])
  (get-pics-by-user [db user-id page-id])
  (get-pic [db pic-id])
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

(s/fdef create-pic
  :args (s/cat :db ::pics-repository
               :pic-create-model ::pics-domain/pic-create-model
               :state ::base-domain/tcc-state)
  :ret (s/tuple ::pics-domain/pic-model ::base-domain/tcc-state))

(s/fdef update-pic-state
  :args (s/cat :db ::pics-repository
               :pic-id ::pics-domain/pic-id
               :state ::base-domain/tcc-state)
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
;; (def image-url (.toString (java.util.UUID/randomUUID)))
;; (def image-url2 (.toString (java.util.UUID/randomUUID)))

;; (s/valid? ::pics-domain/pic-create-model
;;           {:user-id "000000000000000"
;;            :pic-id (java.util.UUID/randomUUID)
;;            :image-urls [image-url image-url2]
;;            :title "Sample"
;;            :description "Description"})

;; (create-pic
;;  (:picture-gallery.infrastructure.sql.sql/sql system)
;;  {:user-id "000000000000000"
;;   :pic-id (java.util.UUID/randomUUID)
;;   :image-urls [image-url image-url2]
;;   :title "Sample"
;;   :description "Description"}
;;  :try)

;; (s/explain
;;  (s/tuple ::pics-domain/pic-model ::base-domain/tcc-state)
;;  [{:pic-id #uuid "b88c9142-918f-41a0-9e16-147d011f06d5", :user-id "000000000000000", :title "Sample", :image-urls ["fecd892a-f87b-4f79-97dd-2e6229f129dd" "36b0f0fe-ab4f-4cbf-ab54-a08ea24bb831"], :created-at 1616651019542, :is-deleted false, :tcc-state "try", :description "Description"} :try])

;; (def pic-id
;;   (:pic-id (first (get-pics
;;                   (:picture-gallery.infrastructure.sql.sql/sql system)))))
;; (s/valid?
;;  ::pics-domain/pics-model
;;  (get-pics (:picture-gallery.infrastructure.sql.sql/sql system)))

;; => [{:description "Description", :index 0, :tcc_state "try", :is_deleted false, :title "Sample", :updated_at nil, :blob "ad1b3e63-06ba-475c-98b3-e7c8ecb880ab", :id #uuid "89e9813c-10c8-461c-9da1-fc2b4ea3d911", :user_id "000000000000000", :created_at #inst "2021-03-25T02:46:35.378247000-00:00"}]
;; (get-pics-by-user
;;  (:picture-gallery.infrastructure.sql.sql/sql system)
;;  "000000000000000"
;;  1)

;; (update-pic-state
;;  (:picture-gallery.infrastructure.sql.sql/sql system)
;;  pic-id
;;  :confirm)

;; (s/valid? ::pics-domain/pic-model
;;           (get-pic
;;            (:picture-gallery.infrastructure.sql.sql/sql system)
;;            pic-id))

;; (s/valid? empty?
;;           (get-pic
;;            (:picture-gallery.infrastructure.sql.sql/sql system)
;;            pic-id))

;; (delete-pic
;;  (:picture-gallery.infrastructure.sql.sql/sql system)
;;  pic-id true)

;; (delete-pic
;;  (:picture-gallery.infrastructure.sql.sql/sql system)
;;  pic-id false)

;; (ig/halt! system)
