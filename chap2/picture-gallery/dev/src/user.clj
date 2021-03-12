(ns user)

(defn dev
  "Load and switch to the 'dev' namespace"
  []
  (println ":switch to dev")
  (require 'dev)
  (in-ns 'dev)
  :loaded)
