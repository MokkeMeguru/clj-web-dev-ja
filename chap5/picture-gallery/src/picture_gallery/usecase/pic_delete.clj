(ns picture-gallery.usecase.pic-delete
  (:require [picture-gallery.utils.error :refer [err->> border-error]]
            [picture-gallery.domain.error :as error-domain]
            [clojure.spec.alpha :as s]
            [picture-gallery.interface.gateway.database.users-repository :as users-repository]
            [picture-gallery.interface.gateway.database.pics-repository :as pics-repository]
            [picture-gallery.interface.gateway.auth.auth-service :as auth-service]
            [picture-gallery.domain.pics :as pics-domain]))

(s/fdef pic-delete
  :args (s/cat :db (s/and ::users-repository/users-repository
                          ::pics-repository/pics-repository)
               :auth ::auth-service/auth-service
               :input-model ::pics-domain/pic-delete-input)
  :ret (s/or :success (s/tuple ::pics-domain/pic-delete-output nil?)
             :failure (s/tuple nil? ::error-domain/error)))

(defn decode-id-token "
  decode encrypted id-token
  "
  [{:keys [input-model auth] :as m}]
  (let [[[status body] err] (border-error {:function #(auth-service/decode-id-token auth (:encrypted-id-token input-model))
                                           :error-wrapper error-domain/auth-error})]
    (cond
      err [nil err]
      (= :failure status) [nil body]
      :else [(assoc m :id-token (:id-token body)) nil])))

(defn get-exist-user-has-id-token "
  get active (not logical deleted) user
  which has id-token"
  [{:keys [id-token db] :as m}]
  (let [[active-user err] (border-error {:function #(users-repository/get-exist-user-by-auth-token db id-token)
                                         :error-wrapper error-domain/database-error})]
    (cond
      err [nil err]
      (empty? active-user) [nil error-domain/signin-failed-by-user-not-found]
      :else [(assoc m :exist-user active-user) nil])))

(defn get-pic "
  get pic which has pic-id
  "
  [{:keys [input-model db] :as m}]
  (let [{:keys [pic-id]} input-model
        [pic err] (border-error {:function #(pics-repository/get-pic db pic-id)
                                 :error-wrapper error-domain/database-error})]
    (cond
      err [nil err]
      :else [(assoc m :pic pic) err])))

(defn check-owner-is-the-user "
  check the pic's owner is as same as the user
  "
  [{:keys [pic exist-user] :as m}]
  (let [pic-owner (:user-id pic)
        user-id (:user-id exist-user)]
    (cond
      (not= user-id pic-owner) [nil error-domain/pic-delete-failed-by-user-is-invalid]
      :else [m nil])))

(defn delete-pic "
  logical delete pic
  "
  [{:keys [pic db] :as m}]
  (let [[_ err] (border-error {:function #(pics-repository/delete-pic db (:pic-id pic) true)
                               :error-wrapper error-domain/database-error})]
    (cond
      err [nil err]
      :else [(assoc m :delete-succeed? true) nil])))

(defn ->output-model [{:keys [delete-succeed?]}]
  (if delete-succeed?
    [true nil]
    [nil error-domain/pic-delete-failed-by-unknown-reason]))

(defn pic-delete [db auth input-model]
  (err->>
   {:db db
    :auth auth
    :input-model input-model}
   decode-id-token
   get-exist-user-has-id-token
   get-pic
   check-owner-is-the-user
   delete-pic
   ->output-model))
