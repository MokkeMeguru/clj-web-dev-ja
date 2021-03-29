(ns pic-gallery-web.routers
  (:require
   ;; re-frame
   [re-frame.core :as re-frame]

   ;; reitit
   [reitit.coercion :as coercion]
   [reitit.coercion.spec]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend :as rf]

   [pic-gallery-web.services.home.views :as home-views]
   [pic-gallery-web.services.main.events :as main-events]
   [pic-gallery-web.domain.routes :as routes-domain]))

(def home-controllers
  [{:start (fn [_]
             (println "entering home"))
    :stop (fn [_]
            (println "exit home"))}])

(def routes
  ["/"
   [""
    {:name ::routes-domain/home
     :view home-views/home-page
     :link-text "app-home"
     :controllers home-controllers}]])

(def router (rf/router routes))

(defn on-navigate [new-match]
  (when new-match
    (re-frame/dispatch [::main-events/navigated new-match])))

(defn init-routes! []
  (rfe/start!
   router
   on-navigate
   {:use-fragment false
    ;; if true, use # routing
    ;; if false, use http-histroy API
    }))

;; (= ::routes-domain/home (-> (rf/match-by-path router "/") :data :name))
