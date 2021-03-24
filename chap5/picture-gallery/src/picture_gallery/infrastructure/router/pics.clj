(ns picture-gallery.infrastructure.router.pics
  (:require [picture-gallery.domain.openapi.pics :as pics-openapi]))

(defn pics-router [db auth image-db]
  ["/pics"
   {:swagger {:tags ["pics"]}}
   [""
    {:swagger {:tags ["pics"]}
     :post {:summary "post pic"
            :swagger {:security [{:Bearer []}]}
            :parameters {:multipart pics-openapi/pics-post-parameters-multipart}
            :responses {200 {:body ::pics-openapi/pics-post-response}}
            :handler (fn [input-data]
                       {:status 200
                        :body {:id "1"}})}}]
   ["/:pic-id"
    [""
     {:get {:summary "get a pic"
            :parameters {:path {:pic-id ::pics-openapi/id}}
            :responses {200 {:body ::pics-openapi/pic-get-response}}
            :handler (fn [input-data]
                       {:statsu 200
                        :body {}})}
      :delete {:summary "delete a pic"
               :parameters {:path {:pic-id ::pics-openapi/id}}
               :responses {204 {}}
               :handler (fn [input-data]
                          {:status 204
                           :body {}})}}]]])
