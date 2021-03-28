(ns picture-gallery.usecase.user-pics-get
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.user-pics :as user-pics-domain]
            [picture-gallery.interface.gateway.database.pics-repository :as pics-repository]
            [picture-gallery.domain.error :as error-domain]

            [picture-gallery.utils.error :refer [err->> border-error]]
            [orchestra.spec.test :as st]
            [integrant.core :as ig]))

(s/fdef user-pics-get
  :args (s/cat :db ::pics-repository/pics-repository
               :input-model ::user-pics-domain/user-pics-get-input)
  :ret (s/or :success (s/tuple ::user-pics-domain/user-pics-get-output nil?)
             :failure (s/tuple nil? ::error-domain/error)))

(defn get-user-pics-per-page "
  get user pics per page
  "
  [{:keys [db input-model] :as m}]
  (let [{:keys [user-id page-id]} input-model
        [pics-model  err] (border-error {:function #(pics-repository/get-pics-by-user db user-id page-id)
                                         :error-wrapper error-domain/database-error})]
    (cond
      err [nil err]
      :else [(assoc m :user-pics pics-model) nil])))

(defn ->output-model [{:keys [user-pics]}]
  [user-pics nil])

(defn user-pics-get [db input-model]
  (err->>
   {:input-model input-model
    :db db}
   get-user-pics-per-page
   ->output-model))

;; (st/instrument)

;; (def system (ig/init {:picture-gallery.infrastructure.env/env {}
;;                       :picture-gallery.infrastructure.logger/logger {:env (ig/ref :picture-gallery.infrastructure.env/env)}
;;                       :picture-gallery.infrastructure.sql.sql/sql {:env (ig/ref :picture-gallery.infrastructure.env/env)
;;                                                                    :logger (ig/ref :picture-gallery.infrastructure.logger/logger)}}))

;; (user-pics-get
;;  (:picture-gallery.infrastructure.sql.sql/sql system)
;;  {:user-id "313536300183910"
;;   :page-id 1})

;; (ig/halt! system)
