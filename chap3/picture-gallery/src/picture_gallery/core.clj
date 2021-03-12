(ns picture-gallery.core
  (:gen-class)
  (:require [environ.core :refer [env]]
            [taoensso.timbre :as timbre]
            [clojure.java.io :as io]
            [integrant.core :as ig]))

(def config-file
  (if-let [config-file (env :config-file)]
    config-file
    "config.edn"))

(defn load-config [config]
  (-> config
      io/resource
      slurp
      ig/read-string
      (doto
       ig/load-namespaces)))

(defn -main
  [& args]
  (-> config-file
      load-config
      ig/init))
