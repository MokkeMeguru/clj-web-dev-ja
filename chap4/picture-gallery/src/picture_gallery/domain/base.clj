(ns picture-gallery.domain.base
  (:require [clojure.spec.alpha :as s]))

;; the flag of process
(s/def ::success (partial = :success))
(s/def ::failure (partial = :failure))
