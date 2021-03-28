(ns picture-gallery.domain.openapi.sample
  (:require [clojure.spec.alpha :as s]))

(s/def ::ping string?)
(s/def ::comment string?)
(s/def ::pong string?)

(s/def ::post-ping-query-parameters (s/keys :req-un [::ping] :opt-un [::comment]))
(s/def ::post-ping-response (s/keys :req-un [::pong] :opt-un [::comment]))
