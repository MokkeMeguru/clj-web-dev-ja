(ns picture-gallery.interface.gateway.database.users-repository
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.users :as users-domain]
            [integrant.core :as ig]
            [orchestra.spec.test :as st]))

(defprotocol Users
  (get-users [db])
  (get-user-by-user-id [db user-id])
  (get-exist-user-by-auth-token [db auth-token])
  (create-user [db user-create-model])
  (delete-user [db user-id logical?]))

(defn users-repository? [inst]
  (satisfies? Users inst))

(s/def ::users-repository users-repository?)

(s/fdef get-users
  :args (s/cat :db ::users-repository)
  :ret ::users-domain/users-model)

(s/fdef get-user-by-user-id
  :args (s/cat :db ::users-repository :user-id ::users-domain/user-id)
  :ret (s/or :exist ::users-domain/user-model
             :not-exist empty?))

(s/fdef get-exist-user-by-auth-token
  :args (s/cat :db ::users-repository :auth-token ::users-domain/id-token)
  :ret (s/or :exist ::users-domain/user-model
             :not-exist empty?))

(s/fdef create-user
  :args (s/cat :db ::users-repository :user-create-model ::users-domain/user-create-model)
  :ret ::users-domain/user-model)

(s/fdef delete-user
  :args (s/cat :db ::users-repository :user-id ::users-domain/user-id :logical? boolean?)
  :ret (s/and int? (partial <= 0)))

;; (st/instrument)
;; (def system (ig/init {:picture-gallery.infrastructure.env/env {}
;;                       :picture-gallery.infrastructure.sql.sql/sql {:env (ig/ref :picture-gallery.infrastructure.env/env)}}))

;; (def sample-user {:user-id "000000000000000" :id-token "sample-token"})
;; (create-user (:picture-gallery.infrastructure.sql.sql/sql system) sample-user)

;; (get-users (:picture-gallery.infrastructure.sql.sql/sql system))
;; (get-user-by-user-id (:picture-gallery.infrastructure.sql.sql/sql system) "000000000000000")
;; (get-exist-user-by-auth-token (:picture-gallery.infrastructure.sql.sql/sql system) "sample-token")
;; (delete-user (:picture-gallery.infrastructure.sql.sql/sql system) "000000000000000" true)
;; (delete-user (:picture-gallery.infrastructure.sql.sql/sql system) "000000000000000" false)
;; (ig/halt! system)
