(ns user)

(println "Hello")

(defn dev []
  (println "switch dev?")
  (require 'dev)
  (in-ns 'dev)
  :loaded)
