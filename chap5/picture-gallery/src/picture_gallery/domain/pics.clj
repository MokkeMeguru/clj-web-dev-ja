(ns picture-gallery.domain.pics
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string]
            [picture-gallery.domain.users :as users-domain]
            [picture-gallery.domain.base :as base-domain]
            [picture-gallery.domain.auth :as auth-domain])
  (:import javax.imageio.ImageIO))

(def invalid-title-substrs
  ["fuck"])

(defn image-file? [image-file]
  (and (instance? java.io.File image-file)
       (some? (ImageIO/read image-file))))

(def min-title-length 0)
(def max-title-length 128)
(def min-description-length 0)
(def max-description-length 1024)
(def max-images-per-one-pic 3)

(defn acceptable-title? [title]
  (apply
   = false
   (mapv (partial clojure.string/includes? title)
         invalid-title-substrs)))

(s/def ::pic-id uuid?)
(s/def ::image-file image-file?)
(s/def ::title (s/and string?
                      #(< min-title-length (count %) max-title-length)
                      acceptable-title?))
(s/def ::description (s/and string?
                            #(< min-description-length (count %) max-description-length)))

(s/def ::image-url  string?)
(s/def ::image-files (s/coll-of ::image-file :min-count 1 :max-count max-images-per-one-pic))
(s/def ::image-urls (s/coll-of ::image-url :min-count 1 :max-count max-images-per-one-pic))

;; model
(s/def ::pic-images-create-model ::image-files)

(s/def ::pic-create-model
  (s/keys :req-un [::users-domain/user-id ::image-urls ::title]
          :opt-un [::description]))

(s/def ::pic-model
  (s/keys :req-un [::users-domain/user-id ::pic-id ::image-urls ::title ::base-domain/created-at]
          :opt-un [::description]))

(s/def ::pics-model
  (s/coll-of ::pic-model))

;; usecase
(s/def ::pic-post-input
  (s/keys :req-un [::auth-domain/encrypted-id-token ::image-files ::title]
          :opt-un [::description]))

(s/def ::pic-post-output
  (s/keys :req-un [::pic-id]))

(s/def ::pic-get-input
  (s/keys :req-un [::pic-id]))

(s/def ::pic-get-output ::pic-model)

(s/def ::pic-delete-input
  (s/keys :req-un [::auth-domain/encrypted-id-token ::pic-id]))

(s/def ::pic-delete-output true?)

(s/def ::pic-image-get-input
  (s/keys :req-un [::image-url]))

(s/def ::pic-image-get-output
  (s/keys :req-un [::image-file]))

;; (s/explain ::title "fuck")
;; (s/explain ::title "fucker")
;; (s/explain ::title "fuckest")

;; (image-file? (io/file (io/resource "sample.jpg")))
;; (image-file? (io/file (io/resource "sample.json")))

;; (javax.imageio.ImageIO/read
;;  (io/file (io/resource "sample.jpg")))

;; (io/file (io/resource "sample.jpg"))
