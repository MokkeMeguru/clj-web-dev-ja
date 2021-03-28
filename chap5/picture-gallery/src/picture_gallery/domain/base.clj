(ns picture-gallery.domain.base
  (:require [clojure.spec.alpha :as s]))

;; base model
(s/def ::created-at pos-int?)
(s/def ::updated-at pos-int?)
(s/def ::is-deleted boolean?)

;; the flag of process
(s/def ::success (partial = :success))
(s/def ::failure (partial = :failure))

;; tcc flag
(s/def ::tcc-state #{:try :confirm :cancel})
