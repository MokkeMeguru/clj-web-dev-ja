(ns picture-gallery.infrastructure.router.users
  (:require [picture-gallery.domain.openapi.pics :as pics-openapi]
            [picture-gallery.domain.openapi.users :as users-openapi]
            [picture-gallery.interface.presenter.api.user-pics-get :as user-pics-get-presenter]
            [picture-gallery.interface.controller.api.user-pics-get :as user-pics-get-controller]
            [picture-gallery.usecase.user-pics-get :as user-pics-get-usecase]
            [picture-gallery.utils.error :refer [err->>]]
            [clojure.spec.alpha :as s]))

(defn user-pics-get-handler [db input-data]
  (user-pics-get-presenter/->http
   (err->> input-data
           user-pics-get-controller/http->
           (partial user-pics-get-usecase/user-pics-get db))))

(defn users-router [db auth]
  ["/users"
   {:swagger {:tags ["users"]}}
   ["/:user-id"
    ["/pics"
     {:get {:summary "get pics per user"
            :parameters {:query {:page-id pos-int?}
                         :path {:user-id ::users-openapi/user-id}}
            :responses {200 {:body ::pics-openapi/user-pics-get-response}}
            :handler (partial user-pics-get-handler db)}}]]])
