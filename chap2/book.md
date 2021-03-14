- [開発環境の Dockerize](#orgf3a9071)
  - [Port の開放](#org8084b4e)
  - [Directory のマウント](#orgb36083e)
  - [動作確認](#org6f43941)
- [ライブラリの追加](#orgf2d7127)
- [エディタとの接続](#org5297b99)
- [integrant のセットアップ](#orgfdd4b23)
  - [integrant と REPL](#orge17151f)
  - [環境変数を読み込む](#org742fd5d)
  - [環境変数を読み込む CLI の作成](#orgcb58e6c)
- [付録](#orgad40d15)
  - [ここまでのディレクトリの確認](#org26e00d7)
  - [Docker コンテナ内で tmux を走らせる フロー](#org7ce8d70)
  - [Emacs で Clojure 開発を行う Tips](#org498251b)

本稿では、Web API サーバを書いていくにあたり必要な、1. 開発環境の Dockerize、2. 基礎的なライブラリの列挙、3. integrant のセットアップを行います。

<a id="orgf3a9071"></a>

# 開発環境の Dockerize

開発を進めていく上で、デプロイやスケーリングの観点から、Docker という選択肢はかなり受け入れられたものになっています。

今回は Docker を利用して実行環境を構築し、更に RDB もまとめて管理できるよう、docker-compose の利用を行います。 ディレクトリとファイルを次のように追加します。

    picture_gallery
    ├── README.md
    ├── containers           (各 Docker コンテナの設定)
    │   ├── api-server
    │   │   └── Dockerfile
    │   └── postgres
    │       └── Dockerfile
    ├── dev
    ├── doc
    ├── docker-compose.yml  (docker-compose の設定)
    ├── project.clj
    ├── resources
    ├── src
    ├── target
    └── test

Clojure の API Server 用の Dockerfile (api-server/Dockerfile) は次の通り

```dockerfile
FROM clojure:openjdk-11-lein
MAINTAINER MokkeMeguru <meguru.mokke@gmail.com>
ENV LANG C.UTF-8
ENV APP_HOME /app
RUN apt-get update
RUN apt-get -y install tmux
RUN mkdir $APP_HOME
WORKDIR $APP_HOME
```

PostgreSQL の Dockerfile (postgres/Dockerfile) は次の通り

```dockerfile
FROM postgres:10.5
MAINTAINER MokkeMeguru <meguru.mokke@gmail.com>
```

docker-compose.yml は次の通り

```yaml
version: "3"
services:
  dev_db:
    build: containers/postgres
    ports:
      - 5566:5432
    volumes:
      - "dev_db_volume:/var/lib/postgresql/data"
    environment:
      POSTGRES_USER: meguru
      POSTGERS_PASSWORD: emacs
      POSTGRES_INITDB_ARGS: "--encoding=UTF-8"
      POSTGRES_DB: pic_gallery
    restart: always
  repl:
    build: containers/api-server
    command: /bin/bash
    ports:
      - 3000:3000
      - 39998:39998
    volumes:
      - ".:/app"
      - "lib_data:/root/.m2"
    depends_on:
      - dev_db
volumes:
  dev_db_volume:
  lib_data:
```

dev_db_volume、lib_data は docker-compose のデータ永続化の機能 (named volume) を用いるために記述されています。

<a id="org8084b4e"></a>

## Port の開放

docker-compose で走る Docker コンテナの内部と交信するために、 port の開放をすることができます。

- `localhost:5566` で内部の DB へ接続するため、dev_db/ports に `5566:5432` を追加しています。

- `localhost:3000` を通して API サーバとやり取りするために、 repl/ports に `3000:3000` を追加しています。

- `localhost:39998` を通して repl コンテナ内の Clojure インタプリタへ接続するために、 repl/ports に `39998:39998` を追加しています。

<a id="orgb36083e"></a>

## Directory のマウント

今回作るサーバ picture-gallery のソースコードをそのまま repl コンテナで読み込むために、repl/volumes に `.:/app` としてコンテナ内部の `/app` に picture-gallery フォルダをそのままマウントさせています。

<a id="org6f43941"></a>

## 動作確認

試しに動かしてみましょう。

    # ビルド
    $ docker-compose build
    # 立ち上げ
    $ docker-compose run --service-port repl bash
    # REPL 環境の立ち上げ
    root:@xxx:/app# lein repl
    user=> (+ 1 2)
    3
    user=> exit
    Bye for now!
    # 環境から抜け出す (Ctrl-p Ctrl-q)
    root:@xxx:/app#
    $

ちなみに、今回は `Ctrl-p Ctrl-q` で Docker コンテナから抜け出しましたが、これに復帰するには、 `docker ps` コマンドで実行していた CONTAINER ID (e.g. `5b6d5b45e8aa`) を確認し、

    $ docker exec -it 5b6d5b45e8aa bash

とします。

管理のために、 Docker コンテナ内で tmux や byobu といったツールを利用すると良いでしょう。 [5.2](#org7ce8d70)

<a id="orgf2d7127"></a>

# ライブラリの追加

いよいよ具体的な API サーバ開発を進めていくわけですが、それに伴っていくつかのライブラリを追加する必要性があります。 Rails や Spring といったより便利なフレームワークを用いたサーバ開発ではこの工程は不要ですが、ライブラリ選定を自分で行うことで、 **よくわからないけど動く** を減らすことができます。 (本ガイドでは以下に紹介するライブラリを用いましたが、勿論別のライブラリで代替することが可能です。)

<details><summary>追加するライブラリ一覧</summary><div> 簡単のため、追加するライブラリの詳細については省き、一覧と捕捉のみ紹介します。 これらのライブラリの追加は、Clojure x ClojureScript で深める Web 開発 (1) で紹介される `project.clj` に追加されています。

```clojure
;; integrant
[integrant "0.8.0"]
[integrant/repl "0.3.2"]

;; firebase auth のためのライブラリ
[com.google.firebase/firebase-admin "7.1.0" :exclusions [com.google.http-client/google-http-client]]

;; ルーティング、HTTP ハンドラ のためのライブラリ
[ring/ring-jetty-adapter "1.9.1" :exclusions [commons-codec]]
[metosin/reitit "0.5.12"]
[metosin/reitit-swagger "0.5.12"]
[metosin/reitit-swagger-ui "0.5.12"]

[ring-cors "0.1.13"]
[ring-logger "1.0.1"]
[com.fasterxml.jackson.core/jackson-core "2.12.2"]

;; 暗号化通信のためのライブラリ
[buddy/buddy-hashers "1.7.0" :exclusions [commons-codec]]

;; 環境変数の読み込みのためのライブラリ
[environ "1.2.0"]

;; ロギング処理のためのライブラリ
[com.taoensso/timbre "5.1.2"]

;; データベースとの通信を行うためのライブラリ
[honeysql "1.0.461"]
[seancorfield/next.jdbc "1.1.643" :exclusions [org.clojure/tools.logging]]
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
[camel-snake-kebab "0.4.2"]
```

</div></details>

なお、注意する点として、ライブラリを追加したら、 **REPL は再起動が必要です** 。 `exit` から `lein repl` で再接続して下さい。

<a id="org5297b99"></a>

# エディタとの接続

ここまでで、Docker コンテナ内で REPL が立ち上がりました。

REPL は各エディタと連携することでより開発を快適にすることができます。 具体的には、コードを書いたところから環境に反映して動かすことができるようになります。

Clojure の REPL と連携できるエディタは Emacs、Vim、VSCode、InteliJ などありますが、今回は多くの人が使っているという理由で VSCode での使い方を紹介します。

まず `project.clj` に以下の設定を追加します。

```clojure
:repl-options
{:host "0.0.0.0"
 :port 39998}
```

これで REPL が開いているポートが、 39998 に固定されます。 先程 docker-compose で port 39998 を開放しているので、 Docker コンテナの外部から REPL のポートへ接続できるようになります。

1.  `lein repl` を Docker コンテナ内で実行します。

2.  VSCode に拡張機能 Calva をインストールします。

3.  左下のボタン nREPL → connect to a running server in your project → Leiningen → localhost:39998

4.  output.calva-repl という画面が出て来ます。

        clj::user=>
        (+ 1 1) ;; (ここで ctrl+enter で評価)
        2
        clj::user=>

    VSCode 上で、 Docker コンテナ内の REPL へ接続することができました。

なお、Calva そのものの詳細な使い方は、 <https://calva.io/> を参考にして下さい。

<a id="orgfdd4b23"></a>

# integrant のセットアップ

> integrant (<https://github.com/weavejester/integrant>) は Data-Driven Architecture で アプリケーションを構築するための Clojure および ClojureScript のマイクロフレームワークです。

integrant で重要となるファイルに、 システムの内部構成を記述したものである、config があります。

例えば、次のようなサーバの例を考えます。 登場人物は、環境変数、データベースのコネクションプール、そしてサーバです。 それぞれには依存関係があり、例えば、

- データベースのコネクションプールには環境変数から得られるアドレスが必要となり、
- サーバには環境変数と DB のコネクションプールの両方が必要になります。

これを、integrant の config 、 `config.edn` を用いて記述すると次のようになります。

```clojure
;; config.edn
{:env {}
 :db-connector {:ref-env #ig/ref :env}
 :server {:ref-port 3000
          :ref-env #ig/ref :env
          :ref-db-connector #ig/ref :db-connector}}
```

環境変数 `:env` に対しては、特に必要要素がないので空辞書 `{}` が与えられています。 コネクションプール `:db-connector` に対しては、環境変数が必要となるので `:ref-env` として先に宣言した `:env` を `{:ref-env #ig/ref :env}` として追加します。

この静的なシステム構成ファイルはプログラムコードとは独立であり、 **設計と実装を分離** することができます。

さらに、例えばサーバの起動が不要な CLI コマンドを書く際に、 `:server` を省いた config を別に作ることで、 `:db-connector` をはじめとする他の実装をそのまま再利用することもできます。 この仕組みは Clean Architecture の他要素を変えずに UI や DB を置き換えられる、という考え方と合致しています。

開発時には、コード編集後に config を再読込みすることで、全体のシステムをアップデートすることができます。

以降では、integrant に慣れる、ということで 環境変数を読み込むというコンポーネントを作っていきます。

<a id="orge17151f"></a>

## integrant と REPL

integrant を使うためには、 config を書き、読み込む機構を書く必要があります。 さらに、 REPL 開発と組み合わせるための機構も書いておくと便利です。 幸い、この部分は非常にシンプルに書くことができるので、ここですべて紹介します。

最初に integrant の config を作ります。 まだ何も作っていないので何も要素がありません。

```clojure
{}
```

次に config を読み込むためのコードを作ります。

まずはコマンドで実行する用。 コマンド `lein run` によって 関数 `-main` が実行され、サーバが立ち上がります。

```clojure
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
```

次に REPL で実行する用。 REPL を起動して、 `(start)` で実行、 `(restart)` で再読込して実行、 `(stop)` で停止します。

```clojure
(ns user)

(defn dev
  "Load and switch to the 'dev' namespace"
  []
  (require 'dev)
  (in-ns 'dev)
  (println ":switch to the develop namespace")
  :loaded)
```

```clojure
(ns dev
  (:require
   [picture-gallery.core :as pg-core]
   [integrant.repl :as igr]))

(defn start
  ([]
   (start pg-core/config-file))
  ([config-file]
   (igr/set-prep! (constantly (pg-core/load-config config-file)))
   (igr/prep)
   (igr/init)))

(defn stop []
  (igr/halt))

(defn restart []
  (igr/reset-all))
```

試しに REPL で実行してみましょう。

    user> (dev)
    :switch to the develop namespace
    ;; => :loaded
    dev> (start)
    ;; => :initiated
    dev> (restart)
    :reloading ()
    ;; => :resumed
    dev> (stop)
    ;; => :halted
    dev> (in-ns 'user)
    ;; => #namespace[user]
    user>

<a id="org742fd5d"></a>

## 環境変数を読み込む

環境変数を読み込むための機構を作ります。

まずはコード。 具体的には、環境変数を読み込むライブラリ `environ` を用いて環境変数を読み込み、それを辞書として返す、ということを行っています。

この部分は入力になるので、 `infrastructure` に含められます。

```clojure
(ns picture-gallery.infrastructure.env
  (:require [environ.core :refer [env]]
            [integrant.core :as ig]
            [orchestra.spec.test :as st]))

(defn decode-log-level [str-log-level]
  (condp = str-log-level
    "trace" :trace
    "debug" :debug
    "info" :info
    "warn" :warn
    "error" :error
    "fatal" :fatal
    "report" :report
    :info))

;; (start) で実行される部分
(defmethod ig/init-key ::env [_ _]
  (println "loading environment via environ")
  (let [database-url (env :database-url)
        running (env :env)
        log-level (decode-log-level (env :log-level))]
    (println "running in " running)
    (println "database-url " database-url)
    (println "log-level " log-level)
    (when (.contains ["test" "dev"] running)
      (println "orchestra instrument is active")
      (st/instrument))
    {:database-url database-url
     :running running
     :log-level log-level}))

;; (stop) で実行される部分
(defmethod ig/halt-key! ::env [_ _]
  {})
```

次に config の更新。

```clojure
;; resources/config.edn
{:picture-gallery.infrastructure.env/env {}}
```

実際に動かしてみましょう。

    user> (dev)
    :switch to the develop namespace
    ;; => :loaded
    dev> (start)
    loading environment via environ
    running in  nil
    database-url  nil
    log-level  :info
    ;; => :initiated
    dev>

なんの環境変数も設定していないので、nil ばかり返ってきますね。

環境変数の設定を書いてみましょう。

環境変数は、1. `export` コマンドを使って宣言する 2. `profiles.clj` に記述する の手段を用いることができますが、今回は 2. を用います。

まず、 `project.clj` の profiles を次のように編集し、plugin を追加します。

```clojure
;; project.clj
{;;...
 :plugins
 [;; 開発のためのプラグイン
  [lein-ancient "0.6.15"]
  ;; cli command's execution helper (後の章で必要)
  [lein-exec "0.3.7"]
  ;; test coverage
  [lein-cloverage "1.2.2"]
  ;; environ in leiningen (leiningen と environ を組み合わせるために必要な plugin)
  [lein-environ "1.1.0"]]

 :profiles
  {:dev [:project/dev :profiles/dev]
   :repl {:prep-tasks ^:replace ["javac" "compile"]
          :repl-options {:init-ns user}}
   :project/dev {:source-paths ["dev/src"]
                 :resource-paths ["dev/resources"]}
   :profiles/dev {}
   :uberjar {:aot :all
             :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
   }
```

次に、 `profiles.clj` を用いて、 profiles/dev を上書きします。

```clojure
;; profiles.clj
{:profiles/dev
 {:env
  {:env "dev"
   :database-url "jdbc:postgresql://dev_db:5432/picture_gallery_db?user=meguru&password=emacs"
   :log-level "info"}}}
```

これで準備は完了です。 REPL で実行してみましょう。 **環境変数を更新したので、REPL を立ち上げ直して下さい。**

    user=> (dev)
    :switch to dev
    :loaded
    dev=> (start)
    loading environment via environ
    running in  dev
    database-url  jdbc:postgresql://dev_db:5432/picture_gallery_db?user=meguru&password=emacs
    log-level  :info
    orchestra instrument is active
    :initiated
    dev=> exit
    Bye for now!

次に、 `lein run` を用いて実行してみましょう。 with-profile で **dev** profile を指定します。

    # せっかくなので、 log-level を変えてみます。
    $ export LOG_LEVEL=error
    $ lein with-profile dev run
    Warning: environ value info for key :log-level has been overwritten with error
    loading environment via environ
    running in  dev
    database-url  jdbc:postgresql://dev_db:5432/picture_gallery_db?user=meguru&password=emacs
    log-level  :error
    orchestra instrument is active

<a id="orgcb58e6c"></a>

## 環境変数を読み込む CLI の作成

今までは REPL ないしサーバ本体の実行コードで環境変数の読み込みができるようになっていました。 しかし、実用上、サーバ本体の実行コードではなく別の CLI コマンドで機能を実行したいケースが出てくると思います。

別の CLI コマンドで実行できるようにするためのコードを書くには、次の手順が必要です。

1.  該当の config を記述する

    ```clojure
       ;; resources/cmd/print_env/config.edn
       {:picture-gallery.infrastructure.env/env {}}
    ```

    1.  該当の config を読み込んで動かすロジックを書く

        ```clojure
        ;; src/picture_gallery/cmd/print_env/core.clj
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
        ```

2.  実行スクリプトを書く

    ```sh
    # scripts/print_env.sh
    #!/usr/bin/env bash
    set -euo pipefail

    lein with-profile dev env -p src/picture_gallery/cmd/print_env/core.clj
    ```

以上です。 実際に動かしてみましょう。

    $ chmod +x ./scripts/print_env.sh
    $ ./scripts/print_env.sh
    print environment variables
    loading environment via environ
    running in  dev
    database-url  jdbc:postgresql://dev_db:5432/picture_gallery_db?user=meguru&password=emacs
    log-level  :info
    orchestra instrument is active

動いていることが確認できますね。

<a id="orgad40d15"></a>

# 付録

<a id="org26e00d7"></a>

## ここまでのディレクトリの確認

ここまででできたディレクトリ構造を再確認します。 `src/picture_gallery` 以下が Clean Architecture を踏襲したソースコード部分です。

    .
    ├── CHANGELOG.md
    ├── LICENSE
    ├── README.md
    ├── containers           (Dockerize に利用しました)
    │   ├── api-server
    │   │   └── Dockerfile
    │   └── postgres
    │       └── Dockerfile
    ├── dev
    │   ├── resources
    │   └── src              (integrant と REPL を組み合わせるために利用しました)
    │       ├── dev.clj
    │       └── user.clj
    ├── doc
    ├── docker-compose.yml   (Dockerize に利用しました)
    ├── profiles.clj         (環境変数の設定に利用しました)
    ├── project.clj          (ライブラリの追加/環境変数の設定に利用しました)
    ├── resources
    │   ├── cmd              (CLIのために利用しました)
    │   │   └── print_env
    │   │       └── config.edn
    │   └── config.edn       (integrant の config に利用しました)
    ├── scripts              (CLI のために利用しました)
    │   └── print_env.sh
    ├── src
    │   └── picture_gallery
    │       ├── cmd          (CLI のために利用しました)
    │       │   └── print_env
    │       │       └── core.clj
    │       ├── core.clj     (integrant の実行のために利用しました)
    │       ├── domain
    │       ├── infrastructure
    │       ├── interface
    │       ├── usecase
    │       └── utils
    ├── target
    └── test
        └── picture_gallery

<a id="org7ce8d70"></a>

## Docker コンテナ内で tmux を走らせる フロー

    $ docker exec -it 5b6d5b45e8aa bash
    root@5b6d5b45e8aa:/app# apt update
    root@5b6d5b45e8aa:/app# apt install tmux
    root@5b6d5b45e8aa:/app# tmux
    # (以下 tmux コンソール)
    # (Ctrl-b $ より session 名を repl に変更)
    # PATH の設定
    root@5b6d5b45e8aa:/app# export $PATH=/usr/local/openjdk-11/bin:$PATH
    root@5b6d5b45e8aa:/app# lein repl
    user=> (dev)
    dev=> (go)
    :initialized
    dev=>
    # (Ctrl-b Ctrl-d より デタッチ)
    [detached (from session repl)]
    # (以降 コンテナ内のシェル)
    # 環境から抜け出す (Ctrl-p Ctrl-q)
    root:@xxx:/app#
    $ docker exec -it 5b6d5b45e8aa bash
    root:@xxx:/app# tmux a -t repl
    # (repl session へ復帰)

<a id="org498251b"></a>

## Emacs で Clojure 開発を行う Tips

Emacs で Clojure 開発を行う際には Cider <https://github.com/clojure-emacs/cider> が有名であり、例えば Doom Emacs <https://github.com/hlissner/doom-emacs> と組み合わせて用いることができます。

Vim や Emacs を使ったことのある人であれば、 Doom Emacs を利用するほうが良いでしょう。

Emacs で Docker コンテナ内の REPL と接続するには、 `M-x cider-connect` より `localhost:39998` で接続することができます。
