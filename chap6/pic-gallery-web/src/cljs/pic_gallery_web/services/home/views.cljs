(ns pic-gallery-web.services.home.views)

(defn about []
  [:<>
   [:p.subtitle "Pic Gallery とは"]])

(defn how-to-use []
  [:div.content
   [:p.title "使い方"]])

(def home-body
  [:<>
   [about]
   [:hr]
   [how-to-use]])

(def home-content
  {:title "Welcome to Pic Gallery"
   :body home-body})

(def home-page
  [:div.container.pt-5 {:style {:max-width "640px"}}
   [:div.titles
    [:p.title (:title home-content)]
    (:body home-content)]])
