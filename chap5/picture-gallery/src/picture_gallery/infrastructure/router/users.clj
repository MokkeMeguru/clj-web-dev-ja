(ns picture-gallery.infrastructure.router.users
  (:require [picture-gallery.domain.openapi.pics :as pics-openapi]
            [picture-gallery.domain.openapi.users :as users-openapi]))

(defn users-router [db auth]
  ["/users"
   {:swagger {:tags ["users"]}}
   ["/:user-id"
    ["/pics"
     {:get {:summary "get pics per user"
            :parameters {:query {:page-id pos-int?}
                         :path {:user-id ::users-openapi/user-id}}
            :responses {200 {:body ::pics-openapi/user-pics-get-response}}
            :handler (fn [input-data]
                       {:status 200
                        :body {}})}}]]])
