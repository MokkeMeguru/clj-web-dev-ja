(ns picture-gallery.domain.error
  (:require [clojure.spec.alpha :as s]))

(s/def ::status pos-int?)
(s/def ::code pos-int?)
(s/def ::message string?)
(s/def ::body (s/keys :req-un [::code ::message]))
(s/def ::error (s/keys :req-un [::status ::body]))

(defn input-data-is-invalid [explain-str]
  {:status 422 :body {:code 1 :message (str "input data is invalid: " explain-str)}})

(defn database-error [message]
  {:status 500 :body {:code 1000 :message (str "error caused from sql query : " message)}})

(defn auth-error [message]
  {:status 500 :body {:code 1001 :message (str "error caused from authorization process : " message)}})

(defn image-db-error [message]
  {:status 500 :body {:code 1002 :message (str "error caused from image db process : " message)}})

(def expired-id-token {:status 400 :body {:code 1701 :message "the firebase token is expired"}})
(def invalid-id-token {:status 400 :body {:code 1702 :message "the firebase token is invalid"}})
(def unknown-id-token {:status 400 :body {:code 1703 :message "the firebase token is something wrong"}})

(def user-generation-error-by-user-id-allocation
  {:status 500 :body {:code 2001 :message "server cannot allocate new user id"}})

(def duplicate-account-exist
  {:status 400 :body {:code 2002 :message "duplicate account is exist. please signin as the user account"}})

(def signin-failed-by-user-not-found
  {:status 400 :body {:code 2101 :message "signin failed because the user has id-token is not found"}})

(def image-delete-failed {:status 500 :body {:code 2200 :message "image delete failed"}})

(def pic-delete-failed-by-user-is-invalid {:status 400 :body {:code 2300 :message "this user cannot delete this pic because of user permission deneid"}})
(def pic-delete-failed-by-unknown-reason {:status 400 :body {:code 2301 :message "this pic cannot delete because of unknown reason"}})
