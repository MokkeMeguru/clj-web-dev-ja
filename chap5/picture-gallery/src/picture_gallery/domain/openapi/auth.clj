(ns picture-gallery.domain.openapi.auth
  (:require [clojure.spec.alpha :as s]))

(s/def ::user-id string?)

(s/def ::signin-response (s/keys :req-un [::user-id]))
(s/def ::signup-response (s/keys :req-un [::user-id]))
