(ns picture-gallery.infrastructure.router.auth
  (:require [picture-gallery.usecase.signin :as signin-usecase]
            [picture-gallery.domain.openapi.auth :as auth-openapi]
            [clojure.walk :as w]
            [picture-gallery.utils.error :refer [err->>]]))

(defn signin-post-handler [input-data]
  (println "apiKey: " (-> input-data :headers w/keywordize-keys :authorization))
  {:status 201
   :body {:user-id "123123123123"}})

(defn signup-post-handler [input-data]
  {:status 201
   :body {:user-id "123123123123"}})

(defn auth-router []
  ["/auth"
   ["/signin"
    {:swagger {:tags ["auth"]}
     :post {:summary "signin with firebase-auth token"
            :swagger {:security [{:Bearer []}]}
            :responses {201 {:body ::auth-openapi/signin-response}}
            :handler signin-post-handler}}]
   ["/signup"
    {:swagger {:tags ["auth"]}
     :post {:summary "signup with firebase-auth token"
            :swagger {:security [{:Bearer []}]}
            :responses {201 {:body ::auth-openapi/signup-response}}
            :handler signup-post-handler}}]])
