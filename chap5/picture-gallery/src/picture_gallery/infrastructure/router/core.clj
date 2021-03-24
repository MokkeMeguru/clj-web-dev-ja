(ns picture-gallery.infrastructure.router.core
  (:require
   [reitit.ring :as ring]
   [reitit.core :as reitit]
   [reitit.coercion.spec]

   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]

   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.exception :as exception]
   [picture-gallery.infrastructure.router.utils.multipart :as multipart]
   [reitit.ring.middleware.multipart :as reitit-multipart]
   [reitit.ring.middleware.parameters :as parameters]

   [muuntaja.core :as m]

   [ring.logger :refer [wrap-with-logger]]
   [integrant.core :as ig]
   [taoensso.timbre :as timbre]

   [reitit.dev.pretty :as pretty]

   [picture-gallery.infrastructure.router.sample :as sample-router]
   [picture-gallery.infrastructure.router.auth :as auth-router]
   [picture-gallery.infrastructure.router.users :as users-router]
   [picture-gallery.infrastructure.router.pics :as pics-router]
   [picture-gallery.infrastructure.router.images :as images-router]
   [clojure.spec.alpha :as s]
   [spec-tools.core :as stc]
   [spec-tools.swagger.core :as swagger-spec]))

(def temp-file-part
  (stc/spec {:spec (s/keys :req-un [::filename ::content-type ::tempfile ::size])
             :swagger/type "file"}))
(def sample-string
  (stc/spec
   {:spec string?
    :swagger/type "string"}))

(swagger-spec/transform
 (s/* temp-file-part))

(swagger-spec/transform
 (s/* string?))

(swagger-spec/transform
 (s/* sample-string))

(defn app [db auth]
  (ring/ring-handler
   (ring/router
    [["/swagger.json"
      {:get {:no-doc true
             :swagger {:info {:title "picture-gallery-api"}
                       :securityDefinitions
                       {:Bearer
                        {:type "apiKey"
                         :in "header"
                         :name "Authorization"}}
                       :basePath "/"}

             :handler (swagger/create-swagger-handler)}}]

     ["/api"
      (sample-router/sample-router)
      (auth-router/auth-router db auth)
      (users-router/users-router db auth)
      (pics-router/pics-router db auth nil)
      (images-router/images-router db nil)]]

    {:exception pretty/exception
     :data {:coercion reitit.coercion.spec/coercion
            :muuntaja m/instance
            :middleware
            [;; swagger feature
             swagger/swagger-feature
             ;; query-params & form-params
             parameters/parameters-middleware
             ;; content-negotiation
             muuntaja/format-negotiate-middleware
             ;; encoding response body
             muuntaja/format-response-middleware
             ;; exception handling
             exception/exception-middleware
             ;; decoding request body
             muuntaja/format-request-middleware
             ;; coercing response bodys
             coercion/coerce-response-middleware
             ;; coercing request parameters
             coercion/coerce-request-middleware
             ;; multipart
             (multipart/create-multipart-middleware
              {:force-vectorize-keys [:images]})
             reitit-multipart/multipart-middleware]}})

   (ring/routes
    (swagger-ui/create-swagger-ui-handler {:path "/api"})
    (ring/create-default-handler))
   {:middleware [wrap-with-logger]}))

(defmethod ig/init-key ::router [_ {:keys [env db auth]}]
  (timbre/info "router got: env" env)
  (timbre/info "router got: db" db)
  (timbre/info "router got: auth" auth)
  (app db auth))
