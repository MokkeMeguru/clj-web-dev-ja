(ns picture-gallery.infrastructure.router.auth
  (:require
   [picture-gallery.usecase.signin :as signin-usecase]
   [picture-gallery.usecase.signup :as signup-usecase]
   [picture-gallery.interface.controller.api.signin-post :as signin-post-controller]
   [picture-gallery.interface.controller.api.signup-post :as signup-post-controller]
   [picture-gallery.interface.presenter.api.signin-post :as signin-post-presenter]
   [picture-gallery.interface.presenter.api.signup-post :as signup-post-presenter]
   [picture-gallery.domain.openapi.auth :as auth-openapi]
   [picture-gallery.utils.error :refer [err->>]]))

;; handlers
(defn signin-post-handler [db auth input-data]
  (signin-post-presenter/->http
   (err->> input-data
           signin-post-controller/http->
           (partial signin-usecase/signin db auth))))

(defn signup-post-handler [db auth input-data]
  (signup-post-presenter/->http
   (err->> input-data
           signup-post-controller/http->
           (partial signup-usecase/signup db auth))))

;; router
(defn auth-router [db auth]
  ["/auth"
   ["/signin"
    {:swagger {:tags ["auth"]}
     :post {:summary "signin with firebase-auth token"
            :swagger {:security [{:Bearer []}]}
            :responses {201 {:body ::auth-openapi/signin-response}}
            :handler (partial signin-post-handler db auth)}}]
   ["/signup"
    {:swagger {:tags ["auth"]}
     :post {:summary "signup with firebase-auth token"
            :swagger {:security [{:Bearer []}]}
            :responses {201 {:body ::auth-openapi/signup-response}}
            :handler (partial signup-post-handler db auth)}}]])
