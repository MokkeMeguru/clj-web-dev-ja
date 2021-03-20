(ns picture-gallery.domain.auth
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.users :as users-domain]
            [picture-gallery.domain.error :as error-domain]
            [picture-gallery.domain.base :as base-domain]))

;; model
(s/def ::encrypted-id-token string?)

;; usecase
(s/def ::signin-input
  (s/keys :req-un [::encrypted-id-token]))

(s/def ::signin-output
  (s/keys :req-un [::users-domain/user-id]))

(s/def ::signup-input
  (s/keys :req-un [::encrypted-id-token]))

(s/def ::signup-output
  (s/keys :req-un [::users-domain/user-id]))

;; interface
(s/def ::decode-id-token-succeed
  (s/tuple ::base-domain/success ::users-domain/id-token))

(s/def ::decode-id-token-failed
  (s/tuple ::base-domain/failure ::error-domain/error))

(s/def ::decode-id-token-result
  (s/or :success ::decode-id-token-succeed
        :failure ::decode-id-token-failed))
