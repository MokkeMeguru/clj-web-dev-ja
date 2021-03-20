(ns picture-gallery.cmd.migration.core
  (:gen-class)
  (:require
   [clojure.string]
   [picture-gallery.core :as pg-core]
   [integrant.core :as ig]
   [clojure.tools.cli :refer [parse-opts]]))

(def cli-options
  [["-o" "--operation OPERATION" "operation key in #{:migrate :rollback}"
    :parse-fn keyword
    :validate [#{:migrate :rollback} "Invalid key not be in #{:migrate :rollback}"]]
   ["-d" "--rollback-amount N" "rollback amount when it uses in :rollback opts"
    :parse-fn #(Integer/parseInt %)
    :default 1
    :validate [pos-int?]]
   ["-h" "--help"]])

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n"
       (clojure.string/join \newline errors)
       "\n\nPlease refer the docs by running this program with the option -h"))

(defn usage [options-summary]
  (->> ["This is the migration program"
        "" "Options:" ""
        options-summary]
       (clojure.string/join \newline)))

(defn migration [config-file operation rollback-amount]
  (try
    (-> config-file
        pg-core/load-config
        (assoc-in [:picture-gallery.infrastructure.sql.migrate/migration :operation] operation)
        (assoc-in [:picture-gallery.infrastructure.sql.migrate/migration :rollback-amount] rollback-amount)
        ig/init)
    (println "migration operation is succeed")
    (catch clojure.lang.ExceptionInfo e
      (println "exception:" (.getMessage e)))))

(defn -main
  [& args]
  (let [config-file "cmd/migration/config.edn"
        {:keys [options _ errors summary]} (parse-opts args cli-options)]
    (cond
      errors (println (error-msg errors))
      (:help options) (println (usage summary))
      (:operation options) (migration config-file (:operation options) (:rollback-amount options))
      :else (println (usage summary)))))
