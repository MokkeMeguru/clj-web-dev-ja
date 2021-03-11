(defproject picture-gallery "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  ;; :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
  ;;           :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :resource-paths ["resources" "target/resources"]

  :main ^:skip-aot picture-gallery.core
  :target-path "target/%s"
  :profiles
  {:dev [:project/dev]
   :repl {:prep-tasks ^:replace ["javac" "compile"]
          :repl-options {:init-ns user}}
   :project/dev {:source-paths ["dev/src"]
                 :resource-paths ["dev/resources"]}
   :uberjar {:aot :all
             :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
