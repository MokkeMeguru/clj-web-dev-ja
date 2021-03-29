(ns pic-gallery-web.services.main.views
  (:require [re-frame.core :as re-frame]
            [pic-gallery-web.services.main.subs :as main-subs]
            [pic-gallery-web.services.home.views :as home-views]))

(defn main-panel []
  (let [name (re-frame/subscribe [::main-subs/name])]
    [:div
     [:h1 "Hello from " @name]
     home-views/home-page]))
