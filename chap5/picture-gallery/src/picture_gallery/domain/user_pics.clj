(ns picture-gallery.domain.user-pics
  (:require [picture-gallery.domain.users :as users-domain]
            [picture-gallery.domain.pics :as pics-domain]
            [clojure.spec.alpha :as s]))
;; usecase
(s/def ::user-pics-get-input
  (s/keys :req-un [::users-domain/user-id]))

(s/def ::user-pics-get-output
  (s/keys :req-un [::pics-domain/pics-model]))
