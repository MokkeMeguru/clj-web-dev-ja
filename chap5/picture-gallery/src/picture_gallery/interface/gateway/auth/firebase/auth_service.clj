(ns picture-gallery.interface.gateway.auth.firebase.auth-service
  (:require [clojure.string]
            [picture-gallery.domain.error :as error-domain]
            [picture-gallery.utils.error :refer [err->>]]
            [picture-gallery.interface.gateway.auth.auth-service :refer [Auth]]))

(defn decode-token [firebase-auth encrypted-id-token]
  (-> firebase-auth
      (.verifyIdToken encrypted-id-token)
      .getUid))

(defn expired-id-token? [cause]
  (if (clojure.string/includes? cause "expired")
    [nil error-domain/expired-id-token]
    [cause nil]))

(defn invalid-id-token? [cause]
  (if  (clojure.string/includes? cause "Failed to parse")
    [nil error-domain/invalid-id-token]
    [cause nil]))

(defn unknown-id-token? [_]
  [nil error-domain/unknown-id-token])

(defn safe-decode-token [firebase-auth encrypted-id-token]
  (try
    [:success
     {:id-token (decode-token firebase-auth encrypted-id-token)}]
    (catch Exception e
      [:failure
       (second
        (err->>
         (or (.getMessage e) "unknown")
         expired-id-token?
         invalid-id-token?
         unknown-id-token?))])))

(extend-protocol Auth
  picture_gallery.infrastructure.firebase.core.FirebaseBoundary
  (decode-id-token [{:keys [firebase]} encrypted-id-token]
    (safe-decode-token (:firebase-auth firebase) encrypted-id-token)))
