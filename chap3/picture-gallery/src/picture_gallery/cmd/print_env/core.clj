(ns picture-gallery.cmd.print-env.core
  (:gen-class)
  (:require
   [picture-gallery.core :as pg-core]
   [integrant.core :as ig]))

(defn -main
  [& args]
  (let [config-file  "cmd/print_env/config.edn"]
    (println "print environment variables")
    (-> config-file
        pg-core/load-config
        ig/init)))
(-main)
