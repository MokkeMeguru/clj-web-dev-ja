(ns picture-gallery.domain.openapi.users
  (:require [clojure.spec.alpha :as s]))

(s/def ::user-id string?)
