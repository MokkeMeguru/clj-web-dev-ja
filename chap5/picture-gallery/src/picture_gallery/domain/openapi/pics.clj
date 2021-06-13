(ns picture-gallery.domain.openapi.pics
  (:require
   [reitit.ring.middleware.multipart :as reitit-multipart]
   [spec-tools.data-spec :as ds]
   [spec-tools.core :as stc]
   [clojure.spec.alpha :as s]
   [orchestra.spec.test :as st]))

;; multipart cannot define as spec
(def image
  (stc/spec
   {:spec (s/keys :req-un [::filename ::content-type ::tempfile ::size])
    :swagger/type "file"}))

;; (s/def ::images
;;   (stc/spec
;;    {:spec (s/coll-of multipart/temp-file-part)
;;     :description "images"}))

(s/def ::id string?)
(s/def ::user-id string?)
(s/def ::title string?)
(s/def ::description string?)
(s/def ::images (s/coll-of reitit-multipart/temp-file-part))
(s/def ::image-id string?)
(s/def ::image-url string?)
(s/def ::image-urls (s/coll-of ::image-url))
(s/def ::created-at pos-int?)

(s/def ::user-pic
  (s/keys :req-un [::id ::title ::created-at ::image-urls] :opt-un [::description]))

(s/def ::user-pics (s/coll-of ::user-pic))

(def pics-post-parameters-multipart
  {:images [reitit-multipart/temp-file-part]
   :title ::title
   (ds/opt :description) ::description})

(s/def ::pics-post-response
  (s/keys :req-un [::id]))

(s/def ::pics-get-parameters-query
  (s/keys :req-un [::id]))

(s/def ::pic-get-response
  (s/keys :req-un [::id ::user-id ::title ::image-urls ::created-at] :opt-un [::description]))

(s/def ::user-pics-get-response ::user-pics)

(s/def ::pic-image-get-response reitit-multipart/temp-file-part)
