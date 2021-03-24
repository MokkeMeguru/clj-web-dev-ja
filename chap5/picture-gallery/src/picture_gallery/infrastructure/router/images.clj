(ns picture-gallery.infrastructure.router.images
  (:require [picture-gallery.domain.openapi.pics :as pics-openapi]))

(defn images-router [db image-db]
  ["/img"
   {:swagger {:tags ["images"]}}
   ["/pics/:image-id"
    {:get {:summary "get a image of pic"
           :parameters {:path {:image-id ::pics-openapi/image-id}}
           :swagger {:produces ["image/png"]}
           :handler (fn [input-data]
                      {:status 200
                       :body {}})}}]])
