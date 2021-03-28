(ns picture-gallery.infrastructure.router.pics
  (:require [picture-gallery.domain.openapi.pics :as pics-openapi]
            [picture-gallery.interface.controller.api.pic-post :as pic-post-controller]
            [picture-gallery.interface.presenter.api.pic-post :as pic-post-presenter]
            [picture-gallery.usecase.pic-post :as pic-post-usecase]
            [picture-gallery.interface.controller.api.pic-get :as pic-get-controller]
            [picture-gallery.interface.presenter.api.pic-get :as pic-get-presenter]
            [picture-gallery.usecase.pic-get :as pic-get-usecase]
            [picture-gallery.interface.controller.api.pic-delete :as pic-delete-controller]
            [picture-gallery.interface.presenter.api.pic-delete :as pic-delete-presenter]
            [picture-gallery.usecase.pic-delete :as pic-delete-usecase]
            [picture-gallery.utils.error :refer [err->>]]))

(defn pic-post-handler [db auth image-db input-data]
  (pic-post-presenter/->http
   (err->> input-data
           pic-post-controller/http->
           (partial pic-post-usecase/pic-post db auth image-db))))

(defn pic-get-handler [db input-data]
  (pic-get-presenter/->http
   (err->> input-data
           pic-get-controller/http->
           (partial pic-get-usecase/pic-get db))))

(defn pic-delete-handler [db auth input-data]
  (pic-delete-presenter/->http
   (err->> input-data
           pic-delete-controller/http->
           (partial pic-delete-usecase/pic-delete db auth))))

(defn pics-router [db auth image-db]
  ["/pics"
   {:swagger {:tags ["pics"]}}
   [""
    {:swagger {:tags ["pics"]}
     :post {:summary "post pic"
            :swagger {:security [{:Bearer []}]}
            :parameters {:multipart pics-openapi/pics-post-parameters-multipart}
            :responses {200 {:body ::pics-openapi/pics-post-response}}
            :handler (partial pic-post-handler db auth image-db)}}]
   ["/:pic-id"
    [""
     {:get {:summary "get a pic"
            :parameters {:path {:pic-id ::pics-openapi/id}}
            :responses {200 {:body ::pics-openapi/pic-get-response}}
            :handler (partial pic-get-handler db)}
      :delete {:summary "delete a pic"
               :swagger {:security [{:Bearer []}]}
               :parameters {:path {:pic-id ::pics-openapi/id}}
               :responses {204 {}}
               :handler (partial pic-delete-handler db auth)}}]]])
