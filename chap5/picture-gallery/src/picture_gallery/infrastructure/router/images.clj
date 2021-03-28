(ns picture-gallery.infrastructure.router.images
  (:require [picture-gallery.domain.openapi.pics :as pics-openapi]
            [picture-gallery.interface.controller.api.pic-image-get :as pic-image-get-controller]
            [picture-gallery.interface.presenter.api.pic-image-get :as pic-image-get-presenter]
            [picture-gallery.usecase.pic-image-get :as pic-image-get-usecase]
            [picture-gallery.utils.error :refer [err->>]]))

(defn pic-image-handler [image-db input-data]
  (pic-image-get-presenter/->http
   (err->> input-data
           pic-image-get-controller/http->
           (partial pic-image-get-usecase/pic-image-get image-db))))

(defn images-router [db image-db]
  ["/img"
   {:swagger {:tags ["images"]}}
   ["/pics/:image-id"
    {:get {:summary "get a image of pic"
           :parameters {:path {:image-id ::pics-openapi/image-id}}
           :swagger {:produces ["image/png"]}
           :handler (partial pic-image-handler image-db)}}]])
