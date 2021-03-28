(ns picture-gallery.usecase.pic-image-get
  (:require [picture-gallery.interface.gateway.image-db.pics-service :as pics-service]
            [picture-gallery.utils.error :refer [err->> border-error]]
            [clojure.spec.alpha :as s]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.error :as error-domain]))

(s/fdef pic-image-get
  :args (s/cat :image-db ::pics-service/pics-service
               :input-model ::pics-domain/pic-image-get-input)
  :ret (s/or :success (s/tuple ::pics-domain/pic-image-get-output nil?)
             :failure (s/tuple nil? ::error-domain/error)))

(defn get-pic-image [{:keys [image-db input-model] :as m}]
  (let [{:keys [image-url]} input-model
        [image err] (border-error {:function #(pics-service/get-pic-image image-db image-url)})]
    (cond
      err [nil err]
      :else [(assoc m :image-file image) nil])))

(defn ->output-model [{:keys [image-file]}]
  [{:image-file image-file} nil])

(defn pic-image-get [image-db input-model]
  (err->>
   {:input-model input-model
    :image-db image-db}
   get-pic-image
   ->output-model))
