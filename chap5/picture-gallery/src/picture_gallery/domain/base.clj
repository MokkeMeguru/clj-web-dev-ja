(ns picture-gallery.domain.base
  (:require [clojure.spec.alpha :as s]))

;; base model
(s/def ::created_at pos-int?)

;; the flag of process
(s/def ::success (partial = :success))
(s/def ::failure (partial = :failure))

;; tcc flag
(s/def ::tcc-flag #{:try :confirm :cancel})
