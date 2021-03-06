- [Integrant](#org1029249)
- [Clean Architecture と Directory Structure](#org110c25e)
  - [Clean Architecture](#org4a0d91b)
    - [依存関係を意識したサービス開発の例](#orge9856fe)
  - [Clean Architecture のために Directory Structure を考える](#orge1b3e1e)
- [余談: threading Macro と エラーハンドリング](#org80c02cb)
  - [Threading Macro](#orge18f9af)
  - [エラーハンドリング](#orgd789f8c)

本稿は、Clojure における アプリ開発フレームワーク integrant をベースとして Clean Architecture を採用した API サーバ開発の基礎を紹介します。

<a id="org1029249"></a>

# Integrant

> integrant (<https://github.com/weavejester/integrant>) は Data-Driven Architecture で アプリケーションを構築するための Clojure および ClojureScript のマイクロフレームワークです。

Integrant (<https://github.com/weavejester/integrant>) は Clojure (ClojureScript) のアプリ開発のためのフレームワークです。

類似する他言語で有名なフレームワークというと、Rails や Django あたりになるのかな、という気持ちもありますが、 Integrant はそれに比べると \***\*非常に薄い\*\*** フレームワークです。

Integrant が提供してくれるのは、REPL 開発の支援のみで、より具体的には、 0. システム構成の読み込み 1. アプリの立ち上げ 2. 動的なアプリの更新 3. アプリの停止 が主になります。 HTTP ハンドラやルーティング、ORM などの親切かつ **魔法のような** 機能は提供していません。

Integrant はフレームワークといえどもファイル構造に一切口出ししないため、プロジェクトの立ち上げから初期ディレクトリ構造の決定がややコストになります。 (逆に言えば、ディレクトリ構造を純粋な Clean Architecture ないし別のアーキテクチャにすることができます)

```shell
# picture-gallery は本ガイドで作るアプリ名
lein new app picture-gallery
cd picture-gallery
```

以上の shell コードにより、プロジェクト picture-gallery が生成されます。

余談ですが、Integrant を更に Web サーバ開発向けにしたフレームワーク Duct というものが、 Clojure を書いている人々からは人気があります。 Duct は非常に薄いフレームワークながら、ルーティングや DB とのやり取りまで面倒を見てくれる非常に便利なフレームワークなので、まず何かを作ってみたいという方は Clojure の Duct で Web API 開発してみた (<https://qiita.com/lagenorhynque/items/57d5aa086c4a080a1c54>) を参考にすることをおすすめします。

※今回 Duct を用いていない理由は、Duct の詳細な実装を理解・説明するのが困難であること、 Integrant を活用する場面が多いことを挙げることができます。

<a id="org110c25e"></a>

# Clean Architecture と Directory Structure

先の章で picture-gallery という API サーバの骨子を初期化しました。 現在のディレクトリ構造は次のようになっています。

    picture_gallery
    ├── CHANGELOG.md
    ├── LICENSE
    ├── README.md
    ├── doc
    │   └── intro.md
    ├── project.clj
    ├── resources
    ├── src
    │   └── picture_gallery
    │       └── core.clj
    └── test
        └── picture_gallery
            └── core_test.clj

ここから例えば API のハンドラを生やしたり、 DB への接続コードを書いたり、Swagger との連携を考えたりすると、どうファイルを作っていけばよいのか指針がよくわからないことになります。

今回はここに Clean Architecture という概念を導入して開発を進めていきます。

<a id="org4a0d91b"></a>

## Clean Architecture

Clean Architecture とは、アプリケーション内の モデル、ロジック、UI、DB といった要素を切り分け、上下関係を作った上で、依存関係を一方向に矯正するアーキテクチャです。

![img](./CleanArchitecture.jpg)

上図において、中央がコアであり、外側は内側の要素に依存しています。(逆に言えば、内側は外側の実装に左右されません。)

本アーキテクチャの利点はいくつかあり、例えば

- 要素ごとに独立したテストができる

  例えばロジック (Use Cases) 部分はテスト用の DB を用意せずともテストできる

- UI や DB を特定させる必要がない

  例えば API サーバから CLI のアプリに置換する際に、ロジック (Use Cases) や DB 部分のコードをいじる必要がない。同様に、DB の接続先を PostgreSQL から MySQL や MongoDB に変えるとして、ロジック (Use Cases) 部分や UI 部分のコードをいじる必要がない。

といったものを挙げることができます。

参考: Clean Architecture で API Server を構築してみる(<https://qiita.com/hirotakan/items/698c1f5773a3cca6193e>)

<a id="orge9856fe"></a>

### 依存関係を意識したサービス開発の例

前章で、Clean Architecture は要素分割をして依存関係を特定の方向に矯正することが特徴であることを紹介しました。 とはいえ概念のみでは理解しづらいので、画像投稿の簡単な例を紹介します。

まず、登場人物を整理します。

- Entities

  画像投稿を行う際のデータの仕様です。

      ID:           uuid
      ユーザID:      投稿したユーザの ID
      Title:        タイトル (1 ~ 255 文字)
      Description： 詳細情報 (0 ~ 1023 文字)
      Image:        画像
      Thumbnail:    サムネイル画像

- Use Cases

  画像投稿をする という機能を実現するためのロジックです。

- Controllers、Gateways、Presenters (Interfaces)

  データ加工、SQL の実行を行います。例えば API でやり取りするための JSON encode / decode は、この部分に入ります。

- Web、 UI、Devices、DB、External Interfaces (Infrastructure)

  ルーティングや、DB への接続を行います。

<a id="orge1b3e1e"></a>

## Clean Architecture のために Directory Structure を考える

Clean Architecture は要素ごとに分割、という点が重要なので、ディレクトリ構造から要素分割を行う必要があります。 いくつかパターンはありますが、近年では golang を用いて Clean Architecture をベースにしたサーバ開発が行われている (あるいはそれに関する知見が多く紹介されている) ことから、特に Clean Architecture で API Server を構築してみる (<https://qiita.com/hirotakan/items/698c1f5773a3cca6193e>) を参考に次のようなディレクトリ構造を適用します。

なお、他様々なパターンがあるので、自分の書きやすい形に応用して下さい。

    picture_gallery/dev
    |-- resources                   (開発用の素材)
    `-- src                         (開発だけに使うコード)
        `-- user.clj


    picture_gallery/src
    `-- picture_gallery
        |-- core.clj                (エントリポイント)
        |-- cmd                     (パッチなどの CLI コマンド用)
        |-- domain                  (Entities)
        |-- infrastructure
        |   |-- env.clj             (環境変数の読み込み)
        |   |-- firebase            (firebase との接続)
        |   |-- image_db            (画像保存 DB との接続)
        |   |-- router              (API ルーティング)
        |   |-- server.clj          (サーバの起動 / 終了、ポート設定など)
        |   `-- sql                 (DB との接続、マイグレーション)
        |-- interface
        |   |-- controller
        |   |   `-- api             (入力 json へのデシリアライズ)
        |   |-- gateway
        |   |   |-- database        (DB に対する クエリ実行)
        |   |   |-- image_db        (画像 に対する クエリ実行)
        |   |   `-- auth            (認証処理 (firebase を用いる))
        |   `-- presenter
        |       `-- api             (出力 json へのシリアライズ)
        |-- usecase
        `-- utils
            `-- error.clj           (後述するエラーハンドリングのためのコード)

dev フォルダを利用するために、 `project.clj` を次のように修正します。

```clojure
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
```

(このあたりのコードはかなり Duct の構造を意識しています)

<a id="org80c02cb"></a>

# 余談: threading Macro と エラーハンドリング

<a id="orge18f9af"></a>

## Threading Macro

Clojure には便利なマクロとして threading macro があります。一般的な Lisp 構文では、データ x に対して関数 A -> 関数 B -> 関数 C と適用する際に `(C (B (A x)))` と記述します。これは処理の流れとして

```clojure
(C (B (A x)))
(C (B y)) ;; y = (A x)
(C z)     ;; z = (B y) = (B (A x))
```

となるため、内側の括弧から順番に処理されるという考え方を持てば自然なことと言えます。 x を A -> B -> C と適用するならば、視認性を高めるためにも A, B, C と書いていきたいものがあります。

Clojure では threading macro がこの要望を答えるものとしてあります。先程の例ですと、

```clojure
(C (B (A x)))
;; is equivalent with
(-> x A B C)
```

と threading macro `->` を用いて書くことができます。

ここで画像投稿の API サーバ側の処理を考えてみると、

1.  データを受け取る
2.  データのデシリアライズ
3.  ユーザの認証
4.  画像のチェック
5.  画像の加工
6.  画像の保存
7.  DB へ投稿情報の保存
8.  レスポンスの生成
9.  レスポンスのシリアライズ
10. レスポンスの返却

という処理の流れを想定することができます。これを Clojure の threading macro を使って書くと、

```clojure
(-> data
    receive-data
    json->image-topic
    check-user
    check-image
    process-image
    insert-image
    insert-image-topic
    ->image-topic-response
    image-topic-response->json
    reply-data)
```

という形に書くことができます。

<a id="orgd789f8c"></a>

## エラーハンドリング

threading macro が可読性を高める手法であることを見ていただけられたところで、一つ、実務上の問題が発生します。 そう、エラーハンドリングです。

各処理工程で何らかのエラーがあった際に、それ以降の処理をするのは非効率だと言えます。なので、例えば golang などでは `return` を用いて処理を打ち切る手法が多く取られます。 ところが Clojure では、 `if-else` はあっても途中で処理を切り上げる `return` を実現するのは難しいです。仮に `if-else` を用いて処理を記述すると、括弧を処理単位とする性質上、ネストが深くなってしまい、可読性を下げてしまいます。

そのため、次のような関数とマクロ `bind-error` 、 `err->>` を用いることで、エラーハンドリングを行います。

```clojure
(defn bind-error [f [val err]]
  (if (nil? err)
    (f val)
    [nil err]))

(defmacro err->> [val & fns]
  (let [fns (for [f fns] `(bind-error ~f))]
    `(->> [~val nil]
          ~@fns)))
```

やや複雑な関数のため詳細の説明は省略し、例を用いて使い方を説明すると次のような形になります。

```clojure
(defn start-with-H? [param]
  (if (.startsWith (:call param) "H")
    [param nil]
    [nil "is not start of H"]))

(defn end-with-!? [param]
  (if (.endsWith (:call param) "!")
    [param nil]
    [nil "is not end of !"]))

;; 実行例
;; success
(err->>
  {:call "Hello!"}
  start-with-H?
  end-with-!?)
;; -> [{:call "Hello!"} nil]

;; failure 1
(err->>
  {:call "hello"}
  start-with-H?
  end-with-!?)
;; -> [nil "is not start of H"]

;;failure 2
(err->>
  {:call "Hello"}
  start-with-H?
  end-with-!?)
;; -> [nil "is not end of !"]
```

重要なところは返り値が `[success-response failure-error-or-nil]` となっていることです。 2 番目の要素 `failure-error-or-nil` がエラーの判定とエラー内容を表しており、関数 `bind-error` によって、エラーがあれば以降の処理を実行しない機能が実現されています。
