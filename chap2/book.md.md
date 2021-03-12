- [開発環境の Dockerize](#org7bb2cba)
  - [Port の開放](#orge334110)
  - [Directory のマウント](#org65eaa0a)
  - [動作確認](#orgf710af6)
- [ライブラリの追加](#org81b818e)
- [エディタとの接続](#org6355f7d)
- [integrant のセットアップ](#orga513c8f)
  - [integrant と REPL](#org60422c8)
  - [環境変数を読み込む](#orged5b1e5)
  - [環境変数を読み込む CLI の作成](#orgcdcc5a4)
- [付録](#orgf1ee87a)
  - [ここまでのディレクトリの確認](#org7cbdb10)
  - [Docker コンテナ内で tmux を走らせる フロー](#org5d00284)
  - [Emacs で Clojure 開発を行う Tips](#org86136ce)

本稿では、Web API サーバを書いていくにあたり必要な、1. 開発環境の Dockerize、2. 基礎的なライブラリの列挙、3. integrant のセットアップを行います。

<a id=&ldquo;org7bb2cba&rdquo;></a>

開発を進めていく上で、デプロイやスケーリングの観点から、Docker という選択肢はかなり受け入れられたものになっています。

今回は Docker を利用して実行環境を構築し、更に RDB もまとめて管理できるよう、docker-compose の利用を行います。 ディレクトリとファイルを次のように追加します。

picture<sub>gallery</sub> ├── README.md ├── containers (各 Docker コンテナの設定) │   ├── api-server │   │   └── Dockerfile │   └── postgres │   └── Dockerfile ├── dev ├── doc ├── docker-compose.yml (docker-compose の設定) ├── project.clj ├── resources ├── src ├── target └── test

Clojure の API Server 用の Dockerfile (api-server/Dockerfile) は次の通り

\`\`\`dockerfile FROM clojure:openjdk-11-lein MAINTAINER MokkeMeguru <meguru.mokke@gmail.com> ENV LANG C.UTF-8 ENV APP<sub>HOME</sub> /app RUN apt-get update RUN apt-get -y install tmux RUN mkdir $APP<sub>HOME</sub> WORKDIR $APP<sub>HOME</sub> \`\`\`

PostgreSQL の Dockerfile (postgres/Dockerfile) は次の通り

\`\`\`dockerfile FROM postgres:10.5 MAINTAINER MokkeMeguru <meguru.mokke@gmail.com> \`\`\`

docker-compose.yml は次の通り

\`\`\`yaml version: &ldquo;3&rdquo; services: dev<sub>db</sub>: build: containers/postgres ports:

- 5566:5432

volumes:

- &ldquo;dev<sub>db</sub><sub>volume</sub>:/var/lib/postgresql/data&rdquo;

environment: POSTGRES<sub>USER</sub>: meguru POSTGERS<sub>PASSWORD</sub>: emacs POSTGRES<sub>INITDB</sub><sub>ARGS</sub>: &ldquo;&#x2013;encoding=UTF-8&rdquo; POSTGRES<sub>DB</sub>: pic<sub>gallery</sub> restart: always repl: build: containers/api-server command: /bin/bash ports:

- 3000:3000
- 39998:39998

volumes:

- &ldquo;.:/app&rdquo;
- &ldquo;lib<sub>data</sub>:/root/.m2&rdquo;

depends<sub>on</sub>:

- dev<sub>db</sub>

volumes: dev<sub>db</sub><sub>volume</sub>: lib<sub>data</sub>: \`\`\`

dev\\\_\\\\}db<sub>volume</sub>、lib<sub>data</sub> は docker-compose のデータ永続化の機能 (named volume) を用いるために記述されています。

<a id=&ldquo;orge334110&rdquo;></a>

\## Port の開放

docker-compose で走る Docker コンテナの内部と交信するために、 port の開放をすることができます。

- \`localhost:5566\` で内部の DB へ接続するため、dev<sub>db</sub>/ports に \`5566:5432\` を追加しています。

- \`localhost:3000\` を通して API サーバとやり取りするために、 repl/ports に \`3000:3000\` を追加しています。

- \`localhost:39998\` を通して repl コンテナ内の Clojure インタプリタへ接続するために、 repl/ports に \`39998:39998\` を追加しています。

<a id=&ldquo;org65eaa0a&rdquo;></a>

\## Directory のマウント

今回作るサーバ picture-gallery のソースコードをそのまま repl コンテナで読み込むために、repl/volumes に \`.:/app\` としてコンテナ内部の \`/app\` に picture-gallery フォルダをそのままマウントさせています。

<a id=&ldquo;orgf710af6&rdquo;></a>

\## 動作確認

試しに動かしてみましょう。

\$ docker-compose build

\$ docker-compose run &#x2013;service-port repl bash

root:@xxx:/app# lein repl user=> (+ 1 2) 3 user=> exit Bye for now!

root:@xxx:/app# \$

ちなみに、今回は \`Ctrl-p Ctrl-q\` で Docker コンテナから抜け出しましたが、これに復帰するには、 \`docker ps\` コマンドで実行していた CONTAINER ID (e.g. \`5b6d5b45e8aa\`) を確認し、

\$ docker exec -it 5b6d5b45e8aa bash

とします。

管理のために、 Docker コンテナ内で tmux や byobu といったツールを利用すると良いでしょう。 [5.2](#org5d00284)

<a id=&ldquo;org81b818e&rdquo;></a>

いよいよ具体的な API サーバ開発を進めていくわけですが、それに伴っていくつかのライブラリを追加する必要性があります。 Rails や Spring といったより便利なフレームワークを用いたサーバ開発ではこの工程は不要ですが、ライブラリ選定を自分で行うことで、 \***\*よくわからないけど動く\*\*** を減らすことができます。 (本ガイドでは以下に紹介するライブラリを用いましたが、勿論別のライブラリで代替することが可能です。)

<details><summary>追加するライブラリ一覧</summary><div> 簡単のため、追加するライブラリの詳細については省き、一覧と捕捉のみ紹介します。 これらのライブラリの追加は、Clojure x ClojureScript で深める Web 開発 (1) で紹介される \`project.clj\` に追加されています。

\`\`\`clojure ;; integrant [integrant &ldquo;0.8.0&rdquo;][integrant/repl &ldquo;0.3.2&rdquo;]

;; firebase auth のためのライブラリ [com.google.firebase/firebase-admin &ldquo;7.1.0&rdquo; :exclusions [com.google.http-client/google-http-client]]

;; ルーティング、HTTP ハンドラ のためのライブラリ [ring/ring-jetty-adapter &ldquo;1.9.1&rdquo; :exclusions [commons-codec]] [metosin/reitit &ldquo;0.5.12&rdquo; :exclusions [mvxcvi/puget]][metosin/reitit-swagger &ldquo;0.5.12&rdquo;] [metosin/reitit-swagger-ui &ldquo;0.5.12&rdquo;]

[ring-cors &ldquo;0.1.13&rdquo;][ring-logger &ldquo;1.0.1&rdquo;] [com.fasterxml.jackson.core/jackson-core &ldquo;2.12.2&rdquo;]

;; 暗号化通信のためのライブラリ [buddy/buddy-hashers &ldquo;1.7.0&rdquo; :exclusions [commons-codec]]

;; 環境変数の読み込みのためのライブラリ [environ &ldquo;1.2.0&rdquo;]

;; ロギング処理のためのライブラリ [com.taoensso/timbre &ldquo;5.1.2&rdquo;]

;; データベースとの通信を行うためのライブラリ [honeysql &ldquo;1.0.461&rdquo;]seancorfield/next.jdbc &ldquo;1.1.643&rdquo; :exclusions [org.clojure/tools.logging]] [hikari-cp &ldquo;2.13.0&rdquo;][org.postgresql/postgresql &ldquo;42.2.19&rdquo;] [net.ttddyy/datasource-proxy &ldquo;1.7&rdquo;]

;; マイグレーションを行うためのライブラリ [ragtime &ldquo;0.8.1&rdquo;]

;; テスト、 Spec のためのライブラリ [orchestra &ldquo;2021.01.01-1&rdquo;][org.clojure/test.check &ldquo;1.1.0&rdquo;]

;; CLI コマンドの実行のためのライブラリ [org.clojure/tools.cli &ldquo;1.0.206&rdquo;]

;; JSON 処理、時刻処理、文字列処理のためのライブラリ [clj-time &ldquo;0.15.2&rdquo;][cheshire &ldquo;5.10.0&rdquo;] [camel-snake-kebab &ldquo;0.4.2&rdquo;] \`\`\`

</div></details>

なお、注意する点として、ライブラリを追加したら、 \***\*REPL は再起動が必要です\*\*** 。 \`exit\` から \`lein repl\` で再接続して下さい。

<a id=&ldquo;org6355f7d&rdquo;></a>

ここまでで、Docker コンテナ内で REPL が立ち上がりました。

REPL は各エディタと連携することでより開発を快適にすることができます。 具体的には、コードを書いたところから環境に反映して動かすことができるようになります。

Clojure の REPL と連携できるエディタは Emacs、Vim、VSCode、InteliJ などありますが、今回は多くの人が使っているという理由で VSCode での使い方を紹介します。

まず \`project.clj\` に以下の設定を追加します。

\`\`\`clojure :repl-options {:host &ldquo;0.0.0.0&rdquo; :port 39998} \`\`\`

これで REPL が開いているポートが、 39998 に固定されます。 先程 docker-compose で port 39998 を開放しているので、 Docker コンテナの外部から REPL のポートへ接続できるようになります。

1.  lein repl を Docker コンテナ内で実行します。

2.  VSCode に拡張機能 Calva をインストールします。

3.  左下のボタン nREPL → connect to a running server in your project → Leiningen → localhost:39998

4.  output.calva-repl という画面が出て来ます。

    clj::user=> (+ 1 1) ;; (ここで ctrl+enter で評価) 2 clj::user=>

    VSCode 上で、 Docker コンテナ内の REPL へ接続することができました。

なお、Calva そのものの詳細な使い方は、 <https://calva.io/> を参考にして下さい。

<a id=&ldquo;orga513c8f&rdquo;></a>

> integrant (<https://github.com/weavejester/integrant>) は Data-Driven Architecture で アプリケーションを構築するための Clojure および ClojureScript のマイクロフレームワークです。

integrant で重要となるファイルに、 config と呼ばれるシステムの内部構成を記述したものがあります。

例えば、次のようなサーバの例を考えます。 登場人物は、環境変数、データベースのコネクションプール、そしてサーバです。 それぞれには依存関係があり、例えば、

- データベースのコネクションプールには環境変数から得られるアドレスが必要となり、
- サーバには環境変数と DB のコネクションプールの両方が必要になります。

これを、integrant の config 、 \`config.edn\` を用いて記述すると次のようになります。

\`\`\`clojure {:env {} :db-connector {:ref-env #ig/ref :env} :server {:ref-port 3000 :ref-env #ig/ref :env :ref-db-connector #ig/ref :db-connector}} \`\`\`

環境変数 \`:env\` に対しては、特に必要要素がないので空辞書 \`{}\` が与えられています。 コネクションプール \`:db-connector\` に対しては、環境変数が必要となるので \`:ref-env\` として先に宣言した \`:env\` を \`{:ref-env #ig/ref :env}\` として追加します。

この静的なシステム構成ファイルはプログラムコードとは独立であり、 \***\*設計と実装を分離\*\*** することができます。

さらに、例えばサーバの起動が不要な CLI コマンドを書く際に、 \`:server\` を省いた config を別に作ることで、 \`:db-connector\` をはじめとする他の実装をそのまま再利用することもできます。 この仕組みは Clean Architecture の他要素を変えずに UI や DB を置き換えられる、という考え方と合致しています。

開発時には、コード編集後に config を再読込みすることで、全体のシステムをアップデートすることができます。

以降では、integrant に慣れる、ということで 環境変数を読み込むというコンポーネントを作っていきます。

<a id=&ldquo;org60422c8&rdquo;></a>

\## integrant と REPL

integrant を使うためには、 config を書き、読み込む機構を書く必要があります。 さらに、 REPL 開発と組み合わせるための機構も書いておくと便利です。 幸い、この部分は非常にシンプルに書くことができるので、ここですべて紹介します。

最初に integrant の config を作ります。 まだ何も作っていないので何も要素がありません。

\`\`\`clojure {} \`\`\`

次に config を読み込むためのコードを作ります。

まずはコマンドで実行する用。 コマンド \`lein run\` によって 関数 \`-main\` が実行され、サーバが立ち上がります。

\`\`\`clojure (ns picture-gallery.core (:gen-class) (:require [environ.core :refer [env]][taoensso.timbre :as timbre] [clojure.java.io :as io][integrant.core :as ig]))

(def config-file (if-let [config-file (env :config-file)] config-file &ldquo;config.edn&rdquo;))

(defn load-config [config] (-> config io/resource slurp ig/read-string (doto ig/load-namespaces)))

(defn -main [& args] (-> config-file load-config ig/init)) \`\`\`

次に REPL で実行する用。 REPL を起動して、 \`(start)\` で実行、 \`(restart)\` で再読込して実行、 \`(stop)\` で停止します。

\`\`\`clojure (ns user)

(defn dev &ldquo;Load and switch to the &lsquo;dev&rsquo; namespace&rdquo; [] (require &rsquo;dev) (in-ns &rsquo;dev) (println &ldquo;:switch to the develop namespace&rdquo;) :loaded) \`\`\`

\`\`\`clojure (ns dev (:require [picture-gallery.core :as pg-core][integrant.repl :as igr]))

(defn start ([] (start pg-core/config-file)) ([config-file](igr/set-prep! "constantly (pg-core/load-config config-file")) (igr/prep) (igr/init)))

(defn stop [](igr/halt))

(defn restart [](igr/reset-all)) \`\`\`

試しに REPL で実行してみましょう。

user> (dev) :switch to the develop namespace ;; => :loaded dev> (start) ;; => :initiated dev> (restart) :reloading () ;; => :resumed dev> (stop) ;; => :halted dev> (in-ns &rsquo;user) ;; => #namespace[user] user>

<a id=&ldquo;orged5b1e5&rdquo;></a>

\## 環境変数を読み込む

環境変数を読み込むための機構を作ります。

まずはコード。 具体的には、環境変数を読み込むライブラリ \`environ\` を用いて環境変数を読み込み、それを辞書として返す、ということを行っています。

\`\`\`clojure (ns picture-gallery.infrastructure.env (:require [environ.core :refer [env]][integrant.core :as ig] [orchestra.spec.test :as st]))

(defn decode-log-level [str-log-level] (condp = str-log-level &ldquo;trace&rdquo; :trace &ldquo;debug&rdquo; :debug &ldquo;info&rdquo; :info &ldquo;warn&rdquo; :warn &ldquo;error&rdquo; :error &ldquo;fatal&rdquo; :fatal &ldquo;report&rdquo; :report :info))

(defmethod ig/init-key ::env [\_ \_] (println &ldquo;loading environment via environ&rdquo;) (let [database-url (env :database-url) running (env :env) log-level (decode-log-level (env :log-level))] (println &ldquo;running in &rdquo; running) (println &ldquo;database-url &rdquo; database-url) (println &ldquo;log-level &rdquo; log-level) (when (.contains [&ldquo;test&rdquo; &ldquo;dev&rdquo;] running) (println &ldquo;orchestra instrument is active&rdquo;) (st/instrument)) {:database-url database-url :running running :log-level log-level}))

(defmethod ig/halt-key! ::env [\_ \_] {}) \`\`\`

次に config の更新。

\`\`\`clojure {:picture-gallery.infrastructure.env/env {}} \`\`\`

実際に動かしてみましょう。

user> (dev) :switch to the develop namespace ;; => :loaded dev> (start) loading environment via environ running in nil database-url nil log-level :info ;; => :initiated dev>

なんの環境変数も設定していないので、nil ばかり返ってきますね。

環境変数の設定を書いてみましょう。

環境変数は、1. \`export\` コマンドを使って宣言する 2. \`profiles.clj\` に記述する の手段を用いることができますが、今回は 2. を用います。

まず、 \`project.clj\` の profiles を次のように編集します。

\`\`\`clojure {;;&#x2026; :profiles {:dev [:project/dev :profiles/dev] :repl {:prep-tasks ^:replace [&ldquo;javac&rdquo; &ldquo;compile&rdquo;] :repl-options {:init-ns user}} :project/dev {:source-paths [&ldquo;dev/src&rdquo;] :resource-paths [&ldquo;dev/resources&rdquo;]} :profiles/dev {} :uberjar {:aot :all :jvm-opts [&ldquo;-Dclojure.compiler.direct-linking=true&rdquo;]} } \`\`\`

次に、 \`profiles.clj\` を用いて、 profiles/dev を上書きします。

\`\`\`clojure {:profiles/dev {:env {:env &ldquo;dev&rdquo; :database-url &ldquo;jdbc:postgresql://dev<sub>db</sub>:5432/picture<sub>gallery</sub><sub>db</sub>?user=meguru&password=emacs&rdquo; :log-level &ldquo;info&rdquo;}}} \`\`\`

これで準備は完了です。 REPL で実行してみましょう。 \***\*環境変数を更新したので、REPL を立ち上げ直して下さい。\*\***

user=> (dev) :switch to dev :loaded dev=> (start) loading environment via environ running in dev database-url jdbc:postgresql://dev<sub>db</sub>:5432/picture<sub>gallery</sub><sub>db</sub>?user=meguru&password=emacs log-level :info orchestra instrument is active :initiated dev=> exit Bye for now!

次に、 \`lein run\` を用いて実行してみましょう。 with-profile で \***\*dev\*\*** profile を指定します。

$ export LOG<sub>LEVEL</sub>=error $ lein with-profile dev run Warning: environ value info for key :log-level has been overwritten with error loading environment via environ running in dev database-url jdbc:postgresql://dev<sub>db</sub>:5432/picture<sub>gallery</sub><sub>db</sub>?user=meguru&password=emacs log-level :error orchestra instrument is active

<a id=&ldquo;orgcdcc5a4&rdquo;></a>

\## 環境変数を読み込む CLI の作成

今までは REPL ないしサーバ本体の実行コードで環境変数の読み込みができるようになっていました。 しかし、実用上、サーバ本体の実行コードではなく別の CLI コマンドで機能を実行したいケースが出てくると思います。

別の CLI コマンドで実行できるようにするためのコードを書くには、次の手順が必要です。

1.  該当の config を記述する

    \`\`\`clojure:resources/cmd/print<sub>env</sub>/config.edn {:picture-gallery.infrastructure.env/env {}} \`\`\`

2.  該当の config を読み込んで動かすロジックを書く

    \`\`\`clojure (ns picture-gallery.cmd.print-env.core (:gen-class) (:require [picture-gallery.core :as pg-core][integrant.core :as ig]))

    (defn -main [& args] (let [config-file "cmd/print<sub>env</sub>/config.edn&rdquo;] (println &ldquo;print environment variables&rdquo;) (-> config-file pg-core/load-config ig/init))) (-main) \`\`\`

3.  実行スクリプトを書く

    \`\`\`sh \#!/usr/bin/env bash set -euo pipefail

    lein with-profile dev env -p src/picture<sub>gallery</sub>/cmd/print<sub>env</sub>/core.clj \`\`\`

以上です。 実際に動かしてみましょう。

$ chmod +x ./scripts/print<sub>env.sh</sub> $ ./scripts/print<sub>env.sh</sub> print environment variables loading environment via environ running in dev database-url jdbc:postgresql://dev<sub>db</sub>:5432/picture<sub>gallery</sub><sub>db</sub>?user=meguru&password=emacs log-level :info orchestra instrument is active

動いていることが確認できますね。

<a id=&ldquo;orgf1ee87a&rdquo;></a>

<a id=&ldquo;org7cbdb10&rdquo;></a>

\## ここまでのディレクトリの確認

ここまででできたディレクトリ構造を再確認します。 \`src/picture<sub>gallery</sub>\` 以下が Clean Architecture を踏襲したソースコード部分です。

. ├── CHANGELOG.md ├── LICENSE ├── README.md ├── containers │   ├── api-server │   │   └── Dockerfile │   └── postgres │   └── Dockerfile ├── dev │   ├── resources │   └── src │   ├── dev.clj │   └── user.clj ├── doc ├── docker-compose.yml ├── profiles.clj ├── project.clj ├── resources │   ├── cmd │   │   └── print<sub>env</sub> │ │    └── config.edn │   └── config.edn ├── scripts │   └── print<sub>env.sh</sub> ├── src │   └── picture<sub>gallery</sub> │   ├── cmd │   ├── core.clj │   ├── domain │   ├── infrastructure │   ├── interface │   ├── usecase │   └── utils ├── target └── test └── picture<sub>gallery</sub>

<a id=&ldquo;org5d00284&rdquo;></a>

\## Docker コンテナ内で tmux を走らせる フロー

\$ docker exec -it 5b6d5b45e8aa bash root@5b6d5b45e8aa:/app# apt update root@5b6d5b45e8aa:/app# apt install tmux root@5b6d5b45e8aa:/app# tmux

root@5b6d5b45e8aa:/app# export $PATH=/usr/local/openjdk-11/bin:$PATH root@5b6d5b45e8aa:/app# lein repl user=> (dev) dev=> (go) :initialized dev=>

[detached (from session repl)]

root:@xxx:/app# \$ docker exec -it 5b6d5b45e8aa bash root:@xxx:/app# tmux a -t repl

<a id=&ldquo;org86136ce&rdquo;></a>

\## Emacs で Clojure 開発を行う Tips

Emacs で Clojure 開発を行う際には Cider <https://github.com/clojure-emacs/cider> が有名であり、例えば Doom Emacs <https://github.com/hlissner/doom-emacs> と組み合わせて用いることができます。

Vim や Emacs を使ったことのある人であれば、 Doom Emacs を利用するほうが良いでしょう。

Emacs で Docker コンテナ内の REPL と接続するには、 \`M-x cider-connect\` より \`localhost:39998\` で接続することができます。
