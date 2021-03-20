(ns picture-gallery.usecase.signin
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.utils.error :refer [err->> border-error]]
            [picture-gallery.domain.auth :as auth-domain]
            [picture-gallery.domain.error :as error-domain]
            [picture-gallery.interface.gateway.auth.auth-service :as auth-service]
            [picture-gallery.interface.gateway.database.users-repository :as users-repository]))

(s/fdef signin
  :args (s/cat :input-model ::auth-domain/signin-input)
  :ret (s/or :success (s/cat :signin-output ::auth-domain/signin-output :error nil)
             :failure (s/cat :signin-output nil? :error ::error-domain/error)))

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

(defn ->output-model "
  format as output model
  "
  [{:keys [exist-user]}]
  [{:user-id (:user-id exist-user)} nil])

(defn signin [db auth input-model]
  (err->>
   {:input-model input-model
    :db db
    :auth auth}
   decode-id-token
   get-exist-user-has-id-token
   ->output-model))
