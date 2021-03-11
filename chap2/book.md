- [開発環境の Dockerize](#orgf8d3efa)
  - [Port の開放](#org74b74f4)
  - [Directory のマウント](#org400cc59)
  - [動作確認](#org41651c8)
- [ライブラリの追加](#org60b389f)
- [エディタとの接続](#org887ac26)
- [Test API の作成](#orgad0ecad)
  - [Swagger のある生活](#orgb30f5c8)
  - [ping - pong フローの確認](#orgbee7da0)
  - [domain の作成](#org19a6b14)
- [付録](#org32bf168)
  - [Docker コンテナ内で tmux を走らせる フロー](#orgb2db540)
  - [Emacs で Clojure 開発を行う Tips](#orgaf814a6)

本稿では、Web API サーバを書いていくにあたり必要な、1. 開発環境の Dockerize、2. 基礎的なライブラリの列挙、3. Test API の作成 を行います。

<a id="orgf8d3efa"></a>

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
    ├── docker-compose.yml  (docker-compose の設定)
    ├── profiles.clj
    ├── project.clj
    ├── resources
    │   └── picture_gallery
    │       └── config.edn
    ├── src
    ├── target
    └── test
        └── picture_gallery

Clojure の API Server 用の Dockerfile (api-server/Dockerfile) は次の通り

```dockerfile
FROM clojure:openjdk-11-lein
MAINTAINER MokkeMeguru <meguru.mokke@gmail.com>
ENV LANG C.UTF-8
ENV APP_HOME /app
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

dev<sub>db</sub><sub>volume</sub>、lib<sub>data</sub> は docker-compose のデータ永続化の機能 (named volume) を用いるために記述されています。

<a id="org74b74f4"></a>

## Port の開放

docker-compose で走る Docker コンテナの内部と交信するために、 port の開放をすることができます。

- `localhost:5566` で内部の DB へ接続するため、dev<sub>db</sub>/ports に `5566:5432` を追加しています。

- `localhost:3000` を通して API サーバとやり取りするために、 repl/ports に `3000:3000` を追加しています。

- `localhost:39998` を通して repl コンテナ内の Clojure インタプリタへ接続するために、 repl/ports に `39998:39998` を追加しています。

<a id="org400cc59"></a>

## Directory のマウント

今回作るサーバ picture-gallery のソースコードをそのまま repl コンテナで読み込むために、repl/volumes に `.:/app` としてコンテナ内部の `/app` に picture-gallery フォルダをそのままマウントさせています。

<a id="org41651c8"></a>

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

管理のために、 Docker コンテナ内で tmux や byobu といったツールを利用すると良いでしょう。 [5.1](#orgb2db540)

<a id="org60b389f"></a>

# ライブラリの追加

いよいよ具体的な API サーバ開発を進めていくわけですが、それに伴っていくつかのライブラリを追加する必要性があります。 Rails や Spring といったより便利なフレームワークを用いたサーバ開発ではこの工程は不要ですが、ライブラリ選定を自分で行うことで、 **よくわからないけど動く** を減らすことができます。 (本ガイドでは以下に紹介するライブラリを用いましたが、勿論別のライブラリで代替することが可能です。)

<details><summary>追加するライブラリ一覧</summary><div> 簡単のため、追加するライブラリの詳細については省き、一覧と捕捉のみ紹介します。 これらのライブラリの追加は、Clojure x ClojureScript で深める Web 開発 (1) で紹介される `project.clj` に追加されています。

```clojure
;; firebase auth のためのライブラリ
[com.google.firebase/firebase-admin "7.1.0" :exclusions [com.google.http-client/google-http-client]]

;; ルーティング、HTTP ハンドラ のためのライブラリ
[ring/ring-jetty-adapter "1.9.1" :exclusions [commons-codec]]
[metosin/reitit "0.5.12" :exclusions [mvxcvi/puget]]
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

</div><details>

なお、注意する点として、ライブラリを追加したら、 **REPL は再起動が必要です** 。 `exit` から `lein repl` で再接続して下さい。

<a id="org887ac26"></a>

# エディタとの接続

ここまでで、Docker コンテナ内で REPL が立ち上がりました。

しかし REPL は各エディタと連携することでより開発を快適にすることができます。 具体的には、コードを書いたところから環境に反映して動かすことができるようになります。

Clojure の REPL と連携できるエディタは Emacs、Vim、VSCode、InteliJ などありますが、今回は **多くの人が使っているという理由だけで** VSCode での使い方を紹介します。 (~~多くのファイルを眺める必要のある規模の開発で VSCode を使うんですか？~~)

まず `project.clj` に以下の設定を追加します。

```clojure
:repl-options
{:host "0.0.0.0"
 :port 39998}
```

これで REPL が開いているポートが、 39998 に固定されます。 先程 docker-compose で port 39998 を開放しているので、 Docker コンテナの外部から REPL のポートへ接続できるようになります。

1.  lein repl を Docker コンテナ内で実行します。

2.  VSCode に拡張機能 Calva をインストールします。

3.  左下のボタン nREPL → connect to a running server in your project → Leiningen → localhost:39998

4.  output.calva-repl という画面が出て来ます。

        clj::user=>
        (+ 1 1) ;; (ここで ctrl+enter で評価)
        2
        clj::user=>

    VSCode 上で、 Docker コンテナ内の REPL へ接続することができました。

なお、Calva そのものの詳細な使い方は、 <https://calva.io/> を参考にして下さい。

<a id="orgad0ecad"></a>

# Test API の作成

今回は Test API として ping - pong API を作ってみることにします。

ping - pong API とは /ping へリクエストを投げると &ldquo;pong&rdquo; という文字が返ってくる API です。 DB への接続もないので、非常にシンプルに作ることができます。

更に今後の開発のために、 Swagger と呼ばれる API の仕様記述のためのツールを使ってブラウザ上で ping - pong API をテストできるようにします。

<a id="orgb30f5c8"></a>

## Swagger のある生活

サーバとクライアントの接続部分の情報共有をどのように行うのか。サーバ・クライアントアプリケーション (サービス) を開発する際にこの議題がしばしば挙がります。 (※ Rails / Django のようなサーバとクライアントを一つのプログラムで完結させるものを除く)

一般にサーバとクライアントは JSON を始めとする何らかのフォーマットにエンコードされたデータをやり取りし、それらを仕様として各プログラムは認識 / デコードします。

近年では、Swagger (OpenAPI) というツールがこの仕様共有のために注目されています。 Swagger は API のエンドポイントとそのエンドポイントで通信する際のデータ仕様をブラウザを用いて確認できるツールで、また、Swagger を Web クライアントとしてサーバとデータのやり取りをテストすることができます。

本ガイドでは、Swagger をサーバ側のコードから自動生成することで、Swagger の利用を行っていきます。

<a id="orgbee7da0"></a>

## ping - pong フローの確認

今回扱う ping - pong API のフローを確認します。今回は練習のため comment という optional な 値を導入しました。

    client                               server
      |       +--------------------+       |
      |  ---  | /ping              |  -->  |
      |       |  'ping-message     |       |
      |       +--------------------+       |
      |                                    |
      |       +----------<success>-+       |
      |  <--  |  'pong-message     |  ---  |
      |       +--------------------+       |
      ~                                    ~
      |       +----------<failure>-+       |
      |  <--  |  'error-message    |  ---  |
      |       +--------------------+       |

- &rsquo;ping-message (query)

  ```clojure
    {:ping "ping"
     :comment "<optional string>"}
  ```

- &rsquo;pong-message (response body)

  ```clojure
  {:pong "pong"
   :comment "<optional string>"}
  ```

<a id="org19a6b14"></a>

## domain の作成

<a id="org32bf168"></a>

# 付録

<a id="orgb2db540"></a>

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

<a id="orgaf814a6"></a>

## Emacs で Clojure 開発を行う Tips

Emacs で Clojure 開発を行う際には Cider <https://github.com/clojure-emacs/cider> が有名であり、例えば Doom Emacs <https://github.com/hlissner/doom-emacs> と組み合わせて用いることができます。

Vim や Emacs を使ったことのある人であれば、 Doom Emacs を利用するほうが良いでしょう。

Emacs で Docker コンテナ内の REPL と接続するには、 `M-x cider-connect` より `localhost:39998` で接続することができます。
