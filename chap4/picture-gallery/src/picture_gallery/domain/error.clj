(ns picture-gallery.domain.error
  (:require [clojure.spec.alpha :as s]))

(s/def ::status pos-int?)
(s/def ::code pos-int?)
(s/def ::message string?)
(s/def ::body (s/keys :req-un [::code ::message]))
(s/def ::error (s/keys :req-un [::status ::body]))

(defn input-data-is-invalid [explain-str]
  {:status 422 :body {:code 1 :message (str "input data is invalid: " explain-str)}})

