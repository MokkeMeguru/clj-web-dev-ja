(ns picture-gallery.usecase.signup
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.utils.error :refer [err->> border-error]]
            [picture-gallery.domain.auth :as auth-domain]
            [picture-gallery.domain.error :as error-domain]
            [picture-gallery.interface.gateway.database.users-repository :as users-repository]
            [picture-gallery.interface.gateway.auth.auth-service :as auth-service]
            [picture-gallery.domain.users :as users-domain]))

(s/fdef signup
  :args (s/cat
         :db ::users-repository/users-repository
         :auth ::auth-service/auth-service
         :input-model ::auth-domain/signup-input)
  :ret (s/or :success (s/tuple ::auth-domain/signin-output nil?)
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

(defn validate-duplicate-account "
  validate duplicate account
  by checking the active (not logical deleted) user which has the id-token
  "
  [{:keys [id-token db] :as m}]
  (let [[active-user err] (border-error {:function #(users-repository/get-exist-user-by-auth-token db id-token)
                                         :error-wrapper error-domain/database-error})]
    (cond
      err [nil err]
      active-user [nil error-domain/duplicate-account-exist]
      :else [m nil])))

(defn give-new-user-id "
  generate new unique user-id.
  if it fails over 10 times, raise error
  "
  [{:keys [db] :as m}]
  (loop [try-time 1
         suggested-new-user-id (users-domain/gen-user-id)]
    (let [[exist-user err] (border-error  {:function #(users-repository/get-user-by-user-id db suggested-new-user-id)
                                           :error-wrapper error-domain/database-error})]
      (cond
        err [nil err]
        (empty? exist-user) [(assoc m :new-user-id suggested-new-user-id) nil]
        (> try-time 10) [nil error-domain/user-generation-error-by-user-id-allocation]
        :else (recur (inc try-time)
                     (users-domain/gen-user-id))))))

(defn create-new-user "
  create new user
  "
  [{:keys [id-token new-user-id db] :as m}]
  (let [new-user {:user-id new-user-id
                  :id-token id-token}
        [saved-new-user err] (border-error {:function #(users-repository/create-user db new-user)
                                            :error-wrapper error-domain/database-error})]
    (cond
      err [nil err]
      :else [(assoc m :saved-new-user saved-new-user) nil])))

(defn ->output-model "
  format as output model
  "
  [{:keys [saved-new-user]}]
  [{:user-id (:user-id saved-new-user)} nil])

(defn signup [db auth input-model]
  (err->>
   {:input-model input-model
    :db db
    :auth auth}
   decode-id-token
   validate-duplicate-account
   give-new-user-id
   create-new-user
   ->output-model))
