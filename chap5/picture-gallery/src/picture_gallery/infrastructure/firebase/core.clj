(ns picture-gallery.infrastructure.firebase.core
  (:import (com.google.firebase FirebaseApp FirebaseOptions)
           (com.google.firebase.auth FirebaseAuth))
  (:require [integrant.core :as ig]
            [taoensso.timbre :as timbre]
            [reitit.ring.middleware.dev :as dev]))

(defrecord FirebaseBoundary [firebase])

(defmethod ig/init-key ::firebase
  [_ {:keys [env]}]
  (let [firebase-credentials (:firebase-credentials env)
        firebase-options (FirebaseOptions/builder)
        firebaseApp (-> firebase-options
                        (.setCredentials firebase-credentials)
                        .build
                        FirebaseApp/initializeApp)]
    (timbre/info "connectiong to firebase with " firebase-credentials)
    (->FirebaseBoundary {:firebase-app firebaseApp
                         :firebase-auth (FirebaseAuth/getInstance)})))

(defmethod ig/halt-key! ::firebase
  [_ boundary]
  (->
   boundary
   .firebase
   :firebase-app
   .delete))
