(defproject picture-gallery "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  ;; :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
  ;;           :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 ;; integrant
                 [integrant "0.8.0"]
                 [integrant/repl "0.3.2"]

                 ;; firebase auth のためのライブラリ
                 [com.google.firebase/firebase-admin "7.1.0"]

                 ;; ルーティング、HTTP ハンドラ のためのライブラリ
                 [ring/ring-jetty-adapter "1.9.1"]
                 [metosin/reitit "0.5.12"]
                 [metosin/reitit-swagger "0.5.12"]
                 [metosin/reitit-swagger-ui "0.5.12"]

                 [ring-cors "0.1.13"]
                 [ring-logger "1.0.1"]
                 [com.fasterxml.jackson.core/jackson-core "2.12.2"]

                 ;; 暗号化通信のためのライブラリ
                 [buddy/buddy-hashers "1.7.0"]

                 ;; 環境変数の読み込みのためのライブラリ
                 [environ "1.2.0"]

                 ;; ロギング処理のためのライブラリ
                 [com.taoensso/timbre "5.1.2"]
                 [com.fzakaria/slf4j-timbre "0.3.20"]

                 ;; データベースとの通信を行うためのライブラリ
                 [honeysql "1.0.461"]
                 [seancorfield/next.jdbc "1.1.643"]
                 [hikari-cp "2.13.0"]
                 [org.postgresql/postgresql "42.2.19"]
                 [net.ttddyy/datasource-proxy "1.7"]

                 ;; マイグレーションを行うためのライブラリ
                 [ragtime "0.8.1"]

                 ;; テスト、 Spec のためのライブラリ
                 [orchestra "2021.01.01-1"]
                 [org.clojure/test.check "1.1.0"]

                 ;; CLI コマンドの実行のためのライブラリ
                 [org.clojure/tools.cli "1.0.206"]

                 ;; JSON 処理、時刻処理、文字列処理のためのライブラリ
                 [clj-time "0.15.2"]
                 [cheshire "5.10.0"]
                 [camel-snake-kebab "0.4.2"]]
  :resource-paths ["resources" "target/resources"]

  :plugins
  [;; 開発のためのプラグイン
   [lein-ancient "0.6.15"]
   ;; cider (emacs development tool)
   [cider/cider-nrepl "0.25.4"]
   [refactor-nrepl "2.5.0"]
   ;; cli command's execution helper
   [lein-exec "0.3.7"]
   ;; test coverage
   [lein-cloverage "1.2.2"]
   ;; environ in leiningen
   [lein-environ "1.1.0"]]

  :main ^:skip-aot picture-gallery.core
  :target-path "target/%s"
  :profiles
  {:dev [:project/dev :profiles/dev]
   :repl {:prep-tasks ^:replace ["javac" "compile"]
          :repl-options {:init-ns user}}
   :project/dev {:source-paths ["dev/src"]
                 :resource-paths ["dev/resources"]}
   :profiles/dev {}
   :uberjar {:aot :all
             :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}

  :repl-options
  {:host "0.0.0.0"
   :port 39998}

  ;; alias for coverage
  ;; see. https://qiita.com/lagenorhynque/items/f1e3c75439c1625756f3
  :aliases
  {"coverage" ["cloverage"
               "--ns-exclude-regex" "^(:?dev|user)$"
               "--ns-exclude-regex" "picture-gallery.core$"
               "--codecov"
               "--summary"]})
