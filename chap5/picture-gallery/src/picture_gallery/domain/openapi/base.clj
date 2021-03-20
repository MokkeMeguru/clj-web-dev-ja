(ns picture-gallery.domain.openapi.base
  (:require [clojure.spec.alpha :as s]))

(s/def ::status pos-int?)
