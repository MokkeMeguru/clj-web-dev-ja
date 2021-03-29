(ns pic-gallery-web.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [pic-gallery-web.services.main.events :as main-events]
   [pic-gallery-web.services.main.views :as main-views]
   [pic-gallery-web.config :as config]
   [pic-gallery-web.routers :as routers]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (routers/init-routes!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [main-views/main-panel] root-el)))

(defn init []
  (re-frame/dispatch-sync [::main-events/initialize-db])
  (dev-setup)
  (mount-root))
