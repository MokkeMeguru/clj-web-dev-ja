- [Firebase Auth の準備](#org1238cdc)
- [仮フロントエンドの作成](#org863cc72)
- [サインアップ・サインイン・サインアウトフローの確認](#org2326190)
  - [サインアップ](#orgf80eb4d)
  - [サインイン](#org11fd8d6)
  - [サインアウト](#org8e9ee5b)
- [ドメイン・ハンドラの作成](#orgcbded59)
  - [domain](#org7d40eba)
  - [ルータ & ハンドラ](#org977ae51)
- [infrastructure の実装](#org8821c3c)
  - [Firebase Auth の token 読み込み](#org1e60ea0)
  - [DB の接続](#orgd77ed35)
  - [マイグレーション](#orgac32f93)
    - [実装方針](#org006244c)
    - [マイグレーションファイルを書く](#orgf2a2b2f)
    - [integrant のコードを書く](#org27b4481)
    - [CLI スクリプトを書く](#org76c2680)
    - [サーバ用コードに埋め込む](#orga5e777a)
- [interface の実装](#orgf5bb353)
  - [Firebase Auth の token デコード機構](#org66d8fac)
  - [SQL の実行機構](#orgea8d8f9)
    - [PostgreSQL との接続](#orge86d440)
- [interface の組み込み](#orga53a509)
- [動作確認](#org526fd55)
- [捕捉](#orgce21e80)
  - [実装してみます](#org860b3cb)

本稿では、Web API を作っていく上で頻出する認証・認可周りの話を、Firebase Auth を用いて片付けます。 一般的に パスワード認証などが基礎のガイドでは紹介されますが、 refresh token を代表とする罠が多すぎるので、外部サービスを利用します。

ただし、この手法は、(同等の機能を自前の認証サーバを用いることで実装することはできるとはいえ) Firebase への依存度が極めて高いため、 **技術的負債になる** 点に注意して下さい。

<a id="org1238cdc"></a>

# Firebase Auth の準備

1.  Firebase Project よりプロジェクトを追加します。 アナリティクスの追加の有無を聞かれますが、必要に応じて切り替えて下さい。
2.  左メニューの構築タブにある、Authentication より、 Auth のための設定を行います。

    1.  `Sign-in method` より、 Google を有効にします。 Twitter や Yahoo など他のプロバイダもありますが、 Google が以降の設定について一番楽だと思われます。

        アプリの公開名は、わかりやすい名前 (e.g. 本ガイドで言えば、picture-gallery)を設定すると良いでしょう。

    2.  承認済みドメインに `localhost` が指定されていることを確認して下さい。

        公開時には、公開するドメイン (github pages ならば、 xxx.github.io など) を設定する必要があります。

    3.  プロジェクトの設定 → 全般より、 `プロジェクト名` と `プロジェクトID` を入手して下さい。

        ![img](./img/prep-firebase-auth.png)

    4.  プロジェクトの設定 → サービスアカウントより、新しい秘密鍵を生成して下さい。生成した秘密鍵の入った JSON ファイルは、 `resources/secrets` に `firebase_secrets.json` として保存して下さい (後の説明のため必要です)。

    5.  `firebase_secrets.json` を Github ないし **外部へ共有しないように設定して下さい** 。

        ファイルは gpg コマンドを使って暗号化するなどの処理をしましょう。

        ```shell
        # 共通鍵暗号化方式で暗号化する例
        gpg -c firebase_secrets.json
        ```

参考:

- <https://firebase.google.com/docs/auth/web/google-signin?authuser=1#before_you_begin>
- <https://firebase.google.com/docs/admin/setup?hl=ja#initialize-sdk>

<a id="org863cc72"></a>

# 仮フロントエンドの作成

Firebase Auth はフロントエンドと Firebase の Auth サーバとの通信を繋げて認証情報を獲得します。 そのため、フロントエンドの実装が必須となります。

今回はまず、認証情報を自前の DB に API サーバを通してに持ち込んでいく流れを実装していくので、仮のフロントエンドを作成します。

仮フロントエンドは、http-server (<https://github.com/http-party/http-server>) と １枚の `index.html` を用いて作成します。 まずは仮フロントエンドのプロジェクト作成をします。

```shell
# npm > 5.2.0
mkdir fba_front_sample
cd fba_front_sample
npm init -y
npm install -D http-server
```

次に、index.html を作成します。

<details><summary>index.html (`set your values from firebase project` 部を編集して下さい)</summary>

```html
<!doctype html>
<html>
    <head>
        <meta charset="utf-8">
        <meta http-equiv="x-ua-compatible" content="ie=edge">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <script src="https://www.gstatic.com/firebasejs/ui/4.6.1/firebase-ui-auth.js"></script>
        <link type="text/css" rel="stylesheet" href="https://www.gstatic.com/firebasejs/ui/4.6.1/firebase-ui-auth.css" />
    </head>
    <body>
        <!-- The surrounding HTML is left untouched by FirebaseUI.
             Your app may use that space for branding, controls and other customizations.-->
        <h1>Sample Firebase Auth Page</h1>
        <div>see. developer console</div>
        <div id="firebaseui-auth-container"></div>
        <div id="loader">Loading...</div>
        <button id="signout">SignOut</div>

        <script src="https://www.gstatic.com/firebasejs/8.2.9/firebase-app.js"></script>
        <script src="https://www.gstatic.com/firebasejs/8.2.9/firebase-auth.js"></script>
        <script type="text/javascript">

         // set your values from firebase project
         // --------------------------------------------
         var apiKey = <your apiKey>
         var projectId = <your project id>
         // --------------------------------------------

         var authDomain = projectId + ".firebaseapp.com"
         var firebaseConfig = {
             apiKey: apiKey,
             authDomain:  authDomain,
             projectId: projectId,
         }
         firebase.initializeApp(firebaseConfig);

         // Initialize the FirebaseUI Widget using Firebase.
         var uiConfig = {
             callbacks: {
                 signInSuccessWithAuthResult: function(authResult, redirectUrl){ return true;},
                 uiShown: function() { document.getElementById("loader").style.display='none'; }
             },
             signInFlow: 'redirect',
             signInSuccessUrl: '/',
             signInOptions: [
                 firebase.auth.GoogleAuthProvider.PROVIDER_ID,
             ]
         }

         var ui = new firebaseui.auth.AuthUI(firebase.auth());
         var signOutButton = document.getElementById("signout");
         // default state
         ui.start('#firebaseui-auth-container', uiConfig);
         signOutButton.style.display='none'

         // already signIned
         firebase.auth().onAuthStateChanged((user) => {
             if (user) {
                 firebase.auth().currentUser.getIdToken(true).then(function(idToken) {
                     console.log("id token is below:")
                     console.log(idToken);
                 })
                 ui.delete()
                 signOutButton.style.display='block'
             }
         })

         // signout
         signOutButton.addEventListener('click', function() {
             console.log("signout")
             firebase.auth().signOut().then(_ => {
                 location.reload()
             })
         })


        </script>

    </body>
</html>
```

</details>

ここまでのプロジェクトのディレクトリ構造は次のようになります。

    .
    ├── index.html
    ├── node_modules
    ├── package-lock.json
    └── package.json

`npx run http-server .` より、http サーバを立ち上げ、 `localhost:8080` より `index.html` へアクセスします。

![img](./img/sample_html.png)

ログインすると、開発者コンソールに idToken が表示されます。この idToken がサーバへ受け渡したい認証情報となります。

なお、この **認証情報は有効期限がある** ため、 API をテストする際には最新のものを利用する必要があります。

<a id="org2326190"></a>

# サインアップ・サインイン・サインアウトフローの確認

実装をする前に、今回作る機能の利用フローを考えます。

<a id="orgf80eb4d"></a>

## サインアップ

    client                                server
       |                                    |
       |     +------------------------+     |
       | --- | /signup                | --> |
       |     |  'signup-param         |     |
       |     +------------------------+     |
       |                                    |
       |       +----------<success>-+       |
       |  <--  |  'signup-success   |  ---  |
       |       +--------------------+       |
       ~                                    ~
       |       +----------<failure>-+       |
       |  <--  |  'error-message    |  ---  |
       |       +--------------------+       |

- &rsquo;signup-param

  今後作る機能と一貫性を持たせるために、認証情報 (`idToken`) はクエリやボディではなく、ヘッダに乗せます。

  ```clojure
    {:header {:bearer "<idToken>"}}
  ```

- &rsquo;signup-success

  user-id はユーザに与えられる一意な数列です (e.g. `019012323149`) 。(今回は 12 桁の数字列としましたがスケールなど考えると uuid などのほうが良いです。)

  ```clojure
    {:user-id "<uuid>"}
  ```

<a id="org11fd8d6"></a>

## サインイン

    client                                server
       |                                    |
       |     +------------------------+     |
       | --- | /signin                | --> |
       |     |  'signin-param         |     |
       |     +------------------------+     |
       |                                    |
       |       +----------<success>-+       |
       |  <--  |  'signin-success   |  ---  |
       |       +--------------------+       |
       ~                                    ~
       |       +----------<failure>-+       |
       |  <--  |  'error-message    |  ---  |
       |       +--------------------+       |

- &rsquo;signin-param

  signin と同様です。

  ```clojure
    {:header {:bearer "<idToken>"}}
  ```

- &rsquo;signup-success

  こちらも signup と同様ですが、 signup の `user-id` は生成されるものですが、こちらは検索して得られるものです。

  ```clojure
    {:user-id "<uuid>"}
  ```

<a id="org8e9ee5b"></a>

## サインアウト

サインイン状態の管理は Firebase Auth 側が受け持っているので、こちらが行うことはありません。 (他アプリ開発をしている上で必要となるケースもあるかもしれませんが、今回は扱いません。)

<a id="orgcbded59"></a>

# ドメイン・ハンドラの作成

今回も見通しを良くするために usecase の詳細を省いた実装を先に行います。

<a id="org7d40eba"></a>

## domain

※ **domain は ORM ではない** ので、SQL のテーブルを意識して domain を作るのはおすすめできません。(ORM を意識すると domain が SQL に依存してしまう)

今回問題になるのは、 firebase auth の `id-token` です。firebase auth の id-token は 外部ライブラリによって復号され、一意のユーザトークン (`decoded-id-token`) になります。

- firebase auth の domain

  ```clojure
  (ns picture-gallery.domain.firebase
    (:require [clojure.spec.alpha :as s]))

  (s/def ::id-token string?)
  (s/def ::decoded-id-token string?)
  ```

- user の domain

  ```clojure
  (ns picture-gallery.domain.users
    (:require [clojure.spec.alpha :as s]
              [picture-gallery.domain.firebase :as firebase-domain]))

  (defn user-id? [num-str]
    (re-matches #"^[0-9]{12}" num-str))

  (s/def ::user-id (s/and string? user-id?))
  (s/def ::created_at pos-int?)

  (s/def ::user-create-model
    (s/keys :req-un [::user-id ::firebase-domain/decoded-id-token]))

  (s/def ::user-model
    (s/keys :req-un [::user-id ::firebase-domain/decoded-id-token ::created_at]))
  ```

- auth (signin/signup) の domain

  ```clojure
  (ns picture-gallery.domain.auth
    (:require [clojure.spec.alpha :as s]
              [picture-gallery.domain.users :as users-domain]
              [picture-gallery.domain.firebase :as firebase-domain]))

  (s/def ::signin-input
    (s/keys :req-un [::firebase-domain/id-token]))

  (s/def ::signin-output
    (s/keys :req-un [::users-domain/user-id]))

  (s/def ::signup-input
    (s/keys :req-un [::firebase-domain/id-token]))

  (s/def ::signup-output
    (s/keys :req-un [::users-domain/user-id]))
  ```

- swagger での auth (signin/signup) の domain

  ```clojure
  (ns picture-gallery.domain.openapi.auth
    (:require [clojure.spec.alpha :as s]))

  (s/def ::user-id string?)

  (s/def ::signin-response (s/keys :req-un [::user-id]))
  (s/def ::signup-response (s/keys :req-un [::user-id]))
  ```

<a id="org977ae51"></a>

## ルータ & ハンドラ

controller、 usecase、 presenter など詳細な実装は、この後実装するので省略します。

```clojure
(ns picture-gallery.infrastructure.router.auth
  (:require [picture-gallery.usecase.signin :as signin-usecase]
            [picture-gallery.domain.openapi.auth :as auth-openapi]
            [clojure.walk :as w]
            [picture-gallery.utils.error :refer [err->>]]))

(defn signin-post-handler [input-data]
  (println (-> input-data :headers w/keywordize-keys :authorization))
  {:status 201
   :body {:user-id "123123123123"}})

(defn signup-post-handler [input-data]
  {:status 201
   :body {:user-id "123123123123"}})

(defn auth-router []
  ["/auth"
   ["/signin"
    {:swagger {:tags ["auth"]}
     :post {:summary "signin with firebase-auth token"
            :swagger {:security [{:Bearer []}]}
            :responses {201 {:body ::auth-openapi/signin-response}}
            :handler signin-post-handler}}]
   ["/signup"
    {:swagger {:tags ["auth"]}
     :post {:summary "signup with firebase-auth token"
            :swagger {:security [{:Bearer []}]}
            :responses {201 {:body ::auth-openapi/signup-response}}
            :handler signup-post-handler}}]])
```

ルータのルートに組み込みましょう。

```clojure
(ns picture-gallery.infrastructure.router.core
  (:require
   ;; ...
   [picture-gallery.infrastructure.router.sample :as sample-router]
   [picture-gallery.infrastructure.router.auth :as auth-router]))

(defn app []
  (ring/ring-handler
   (ring/router
    [["/swagger.json"]
      ;; ...
     ["/api"
      (sample-router/sample-router)
      (auth-router/auth-router) ;; add here!
      ]]
    ;; ...
    )))
```

`(restart)` して、Swagger を確認します。

![img](./img/swagger_auth.png)

右上に `Authorize` というボタンがあります。 Swagger では、このボタンより、header の `apiKey` の入力ができるようになっています。 試しに、 &ldquo;sample&rdquo; と入力し、 `/api/auth/signin` を実行すると、REPL のログに次の行が記録されます。

    apiKey: sample

繰り返しますが、今回はこの apiKey に firebase auth の id-token を入力していくことになります。

<a id="org8821c3c"></a>

# infrastructure の実装

Firebase や DB とやり取りをするためにそれぞれとの接続を作る必要があります。この部分は Clean Architecture 的には infrastructure にあたります。

<a id="org1e60ea0"></a>

## Firebase Auth の token 読み込み

[1](#org1238cdc) で用意した、 `resources/secrets/firebase_secrets.json` を読み込んで id-token をデコードするための準備を行います。 今回はライブラリのドキュメントを信用して説明を省略していますが、時間があれば API ドキュメントを読んだほうが良いです。

```clojure
(ns picture-gallery.infrastructure.firebase.core
  (:import (com.google.firebase FirebaseApp FirebaseOptions))
  (:require [integrant.core :as ig]
            [taoensso.timbre :as timbre]))

;; いわゆる型、firebaseApp という値をコンストラクタに取る、と考えると良い
(defrecord FirebaseBoundary [firebaseApp])

(defmethod ig/init-key ::firebase
  [_ {:keys [env]}]
  (let [firebase-credentials (:firebase-credentials env)
        firebase-options (FirebaseOptions/builder)
        firebaseApp (-> firebase-options
                        (.setCredentials firebase-credentials)
                        .build
                        FirebaseApp/initializeApp)]
    (timbre/info "connectiong to firebase with " firebase-credentials)
    (->FirebaseBoundary {:firebase-app firebaseApp
                         :firebase-auth (FirebaseAuth/getInstance)})))


(defmethod ig/halt-key! ::firebase
  [_ boundary]
  (->
   boundary
   .firebase
   :firebase-app
   .delete))
```

参考: <https://firebase.google.com/docs/admin/setup?hl=ja#initialize-sdk>

config を編集します。

```clojure
{:picture-gallery.infrastructure.env/env {}
 :picture-gallery.infrastructure.logger/logger {:env #ig/ref :picture-gallery.infrastructure.env/env}
 :picture-gallery.infrastructure.firebase.core/firebase {:env #ig/ref :picture-gallery.infrastructure.env/env}
 :picture-gallery.infrastructure.router.core/router {:env #ig/ref :picture-gallery.infrastructure.env/env
                                                     :firebase #ig/ref :picture-gallery.infrastructure.firebase.core/firebase}
 :picture-gallery.infrastructure.server/server {:env #ig/ref :picture-gallery.infrastructure.env/env
                                                :router #ig/ref :picture-gallery.infrastructure.router.core/router
                                                :port 3000}}
```

`firebase_secrets.json` のファイル位置は環境変数から教える必要があるので、簡単のために script ファイルを作ります。

```shell
# env.sh
set -euo pipefail

echo "please run as \"source env.sh\""

export GOOGLE_APPLICATION_CREDENTIALS="resources/secrets/firebase_secrets.json"
```

REPL を再起動し、 `(start)` してみましょう。ログに `picture-gallery.infrastructure.firebase.core` の INFO が流れていることが確認できます。

    dev=> (start)
    loading environment via environ
    running in  dev
    database-url  jdbc:postgresql://dev_db:5432/picture_gallery_db?user=meguru&password=emacs
    log-level  :info
    orchestra instrument is active
    2021-03-16T15:52:05.347Z f04004b3a5e3 INFO [picture-gallery.infrastructure.firebase.core:16] - connectiong to firebase with  ServiceAccountCredentials{clientId=107926774701607421850, clientEmail=firebase-adminsdk-l42c5@sample-picture-gallery-c12rb.iam.gserviceaccount.com, privateKeyId=80f9a8cceb5036d0a96f73a108fa485aeed314a4, transportFactoryClassName=com.google.auth.oauth2.OAuth2Utils$DefaultHttpTransportFactory, tokenServerUri=https://oauth2.googleapis.com/token, scopes=[], serviceAccountUser=null, quotaProjectId=null}
    # ...

<a id="orgd77ed35"></a>

## DB の接続

次に DB の接続を行います。 今回は PostgreSQL を用います。 使うライブラリは hirari-cp (<https://github.com/tomekw/hikari-cp>) です。 hikari-cp は 高速に db のコネクションプールを作ることができるライブラリです。

`docker-compose` より、 `port=5432` から PostgreSQL がコンニチハしていることがわかるので、環境変数のセットアップから先に行います。

`profiles.clj` を次のように編集します。 `database-<option-name>` がちょうど環境変数のセットアップに必要な設定です。

```clojure
{:profiles/dev
 {:env
  {:env "dev"
   :database-adapter "postgresql"
   :database-name "pic_gallery"
   :database-username "meguru"
   :database-password "emacs"
   :database-server-name "dev_db"
   :database-port-number "5432"
   :log-level "info"}}}
```

これに従って、 `env.clj` も更新します。

```clojure
(defn get-database-options []
  {:adapter (env :database-adapter)
   :database-name "pic_gallery"
   :username (env :database-username)
   :password (env :database-password)
   :server-name (env :database-server-name)
   :port-number (Integer/parseInt (env :database-port-number))})

(defmethod ig/init-key ::env [_ _]
  (println "loading environment via environ")
  (let [database-options (get-database-options)
        running (env :env)
        log-level (decode-log-level (env :log-level))]
    (println "running in " running)
    (println "log-level " log-level)
    (println "database options" database-options)
    (when (.contains ["test" "dev"] running)
      (println "orchestra instrument is active")
      (st/instrument))
    {:database-options database-options
     :running running
     :log-level log-level
     :firebase-credentials (GoogleCredentials/getApplicationDefault)}))
```

実行サンプルはこんな感じ。

    loading environment via environ
    running in  dev
    log-level  :info
    database options {:adapter postgresql, :username meguru, :password emacs, :server-name dev_db, :port-number 5432}
    orchestra instrument is active
    # ...

次に、infrastructure のコードを書きます。 残念ながら、SQL クエリ周りのログは分離することが困難だったので、本コードの中に含めています。

```clojure
(ns picture-gallery.infrastructure.sql.sql
  (:require [integrant.core :as ig]
            [hikari-cp.core :as hikari-cp]
            [taoensso.timbre :as timbre])
  (:import
   [javax.sql DataSource]
   [net.ttddyy.dsproxy QueryInfo]
   [net.ttddyy.dsproxy.support ProxyDataSource]
   [net.ttddyy.dsproxy.listener QueryExecutionListener]))

(defrecord Boundary [spec])

;; define logging
(defn- query-parameters [params]
  (->> params (map (memfn getArgs)) (sort-by #(aget % 0)) (mapv #(aget % 1))))

(defn- query-parameter-lists [^QueryInfo query-info]
  (mapv query-parameters (.getParametersList query-info)))

(defn- logged-query [^QueryInfo query-info]
  (let [query  (.getQuery query-info)
        params (query-parameter-lists query-info)]
    (into [query] (if (= (count params) 1) (first params) params))))

(defn- logging-listener []
  (reify QueryExecutionListener
    (beforeQuery [_ _ _])
    (afterQuery [_ exec-info query-infos]
      (let [elapsed (.getElapsedTime exec-info)
            queries (mapv logged-query query-infos)]
        (if (= (count queries) 1)
          (timbre/info "sql/query" {:query (first queries) :elapsed elapsed})
          (timbre/info "sql/batch-query" {:queries queries :elapsed elapsed}))))))

(defn wrap-logger [datasource]
  (doto (ProxyDataSource. datasource)
    (.addListener (logging-listener))))

(defn unwrap-logger [^DataSource datasource]
  (.unwrap datasource DataSource))


;; integrant keys
(defmethod ig/init-key ::sql
  [_ {:keys [env logger]}]
  (let [datasource
        (-> (:database-options env)
          (hikari-cp/make-datasource)
          wrap-logger)]
    (timbre/info "setup connection pool ...")
    (->Boundary {:datasource
                 datasource})))

(defmethod ig/halt-key! ::sql
  [_ boundary]
  (timbre/info "close connection pool ...")
  (-> boundary
      .spec
      :datasource
      unwrap-logger
      hikari-cp/close-datasource))
```

実行例は次のようになります。

    (dev)> (start)
    ;; ...
    2021-03-16T20:59:29.564Z f04004b3a5e3 INFO [com.zaxxer.hikari.HikariDataSource:80] - HikariPool-19 - Starting...
    2021-03-16T20:59:29.568Z f04004b3a5e3 INFO [com.zaxxer.hikari.HikariDataSource:82] - HikariPool-19 - Start completed.
    2021-03-16T20:59:29.569Z f04004b3a5e3 INFO [picture-gallery.infrastructure.sql.sql:56] - setup connection pool ...
    ;; => :resumed
    dev>

参考:

- <https://github.com/tomekw/hikari-cp/blob/master/src/hikari_cp/core.clj> の `core-options`
- <https://github.com/brettwooldridge/HikariCP> Java の HikariCP (hikari-cp の参照元)
- <https://github.com/duct-framework/database.sql.hikaricp> hikari-cp への logging 実装

<a id="orgac32f93"></a>

## マイグレーション

<a id="org006244c"></a>

### 実装方針

DB との接続ができたところで、次に DB マイグレーションの設定を行います。 これは ragtime (<https://github.com/weavejester/ragtime>) を利用します。

マイグレーションで行いたいことは次の 2 つです。

1.  マイグレート マイグレーションのファイルに基づいて DB を掘ります。
2.  ロールバック マイグレーションしたものを i (> 1) 個だけ元に戻します。

<a id="orgf2a2b2f"></a>

### マイグレーションファイルを書く

まずマイグレーションファイルを書きます。ragtime のマイグレーションはマイグレート用の up.sql と、ロールバック用の down.sql を書く必要があります。

- 001<sub>users.up.sql</sub>

```sql
-- 001_users.up.sql
CREATE TABLE users (
       id varchar(12) PRIMARY KEY,
       firebase_token varchar(64)
);
```

- 001<sub>users.down.sql</sub>

```sql
-- 001_users.down.sql
DROP TABLE users;
```

<a id="org27b4481"></a>

### integrant のコードを書く

マイグレーションのコードそのものは ragtime のドキュメントを参考に実装します。

さらに、 `operation` キーでマイグレートするかロールバックするかを分岐させます。

```clojure
(ns picture-gallery.infrastructure.sql.migrate
  (:require [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [integrant.core :as ig]
            [taoensso.timbre :as timbre]))

(defn build-config [database-options migration-folder]
  (let [{:keys [adapter database-name username password server-name port-number]} database-options]
    {:datastore (jdbc/sql-database {:dbtype adapter
                                    :dbname database-name
                                    :user username
                                    :password password
                                    :port port-number
                                    :host server-name})
     :migrations (jdbc/load-resources migration-folder)}))

(defmethod ig/init-key ::migration [_ {:keys [env operation rollback-amount]}]
  (let [{:keys [database-options migrations-folder]} env
        migration-config (build-config database-options migrations-folder)]
    (timbre/info "run migration with operation" operation "(rollback-amount is " rollback-amount ")")
    (condp = operation
      :migrate  (repl/migrate migration-config)
      :rollback (repl/rollback migration-config (or rollback-amount 1))
      (let [message  (str "invalid migration operation " operation " is not in #{:migrate :rollback}")]
        (timbre/error message)
        (throw (ex-info message {}))))
    {}))
```

環境変数を渡す必要があるので、 `env.clj` も更新します。

<details><summary>更新したコード</summary>

```clojure
(ns picture-gallery.infrastructure.env
  (:require [environ.core :refer [env]]
            [integrant.core :as ig]
            [orchestra.spec.test :as st]
            [clojure.spec.alpha :as s])
  (:import (com.google.auth.oauth2 GoogleCredentials)))

(s/fdef decode-log-level
  :args (s/cat :str-log-level string?)
  :ret #{:trace :debug :info :warn :error :fatal :report})

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

(defn get-database-options []
  {:adapter (env :database-adapter)
   :database-name (env :database-name)
   :username (env :database-username)
   :password (env :database-password)
   :server-name (env :database-server-name)
   :port-number (Integer/parseInt (env :database-port-number))})

(defmethod ig/init-key ::env [_ _]
  (println "loading environment via environ")
  (let [database-options (get-database-options)
        running (env :env)
        migrations-folder (env :migrations-folder)
        log-level (decode-log-level (env :log-level))]
    (println "running in " running)
    (println "log-level " log-level)
    (println "migrations-folder" migrations-folder)
    (println "database options" database-options)
    (when (.contains ["test" "dev"] running)
      (println "orchestra instrument is active")
      (st/instrument))
    {:database-options database-options
     :running running
     :migrations-folder migrations-folder
     :log-level log-level
     :firebase-credentials (GoogleCredentials/getApplicationDefault)}))
```

</summary>

<a id="org76c2680"></a>

### CLI スクリプトを書く

動作確認のため、先に CLI スクリプトから仕上げます。 clojure.tools.cli (<https://github.com/clojure/tools.cli>) を利用して CLI オプション処理を実装します。

```clojure
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
```

config を書きます。

```clojure
;; resources/cmd/migration/config.cfg
{:picture-gallery.infrastructure.env/env {}
 :picture-gallery.infrastructure.sql.migrate/migration {:env #ig/ref :picture-gallery.infrastructure.env/env}}

```

実行用シェルスクリプトを書きます。

```shell
#!/usr/bin/env bash
# scripts/migration.sh
set -euo pipefail

# $* でシェルスクリプトに与えられた引数を受け渡す
lein run -m picture-gallery.cmd.migration.core $*
```

実行してみます。 Applying 001<sub>users</sub>、Rolling back 001<sub>users</sub> と、マイグレートとロールバックが行われていることが確認できます。

    # ./sample.sh -h
    This is the migration program

    Options:

      -o, --operation OPERATION     operation key in #{:migrate :rollback}
      -d, --rollback-amount N    1  rollback amount when it uses in :rollback opts
    # ./sample.sh -o migrate
    loading environment via environ
    running in  dev
    log-level  :info
    migrations-folder migrations
    database options {:adapter postgresql, :database-name pic_gallery, :username meguru, :password emacs, :server-name dev_db, :port-number 5432}
    orchestra instrument is active
    2021-03-18T14:37:38.388Z f04004b3a5e3 INFO [picture-gallery.infrastructure.sql.migrate:20] - run migration with operation :migrate (rollback-amount is  1 )
    Applying 001_users
    migration operation is succeed
    # ./sample.sh -o rollback
    loading environment via environ
    running in  dev
    log-level  :info
    migrations-folder migrations
    database options {:adapter postgresql, :database-name pic_gallery, :username meguru, :password emacs, :server-name dev_db, :port-number 5432}
    orchestra instrument is active
    2021-03-18T14:38:09.085Z f04004b3a5e3 INFO [picture-gallery.infrastructure.sql.migrate:20] - run migration with operation :rollback (rollback-amount is  1 )
    Rolling back 001_users
    migration operation is succeed

<a id="orga5e777a"></a>

### サーバ用コードに埋め込む

サーバ用コードに埋め込みます。

本ガイドでは、マイグレーションファイルにしたがってマイグレートされた状態を元にサーバコードが書かれている状態を想定します。

```clojure
;; resources/config.edn
{:picture-gallery.infrastructure.env/env {}
 :picture-gallery.infrastructure.logger/logger {:env #ig/ref :picture-gallery.infrastructure.env/env}
 :picture-gallery.infrastructure.firebase.core/firebase {:env #ig/ref :picture-gallery.infrastructure.env/env}
 :picture-gallery.infrastructure.sql.sql/sql {:env #ig/ref :picture-gallery.infrastructure.env/env
                                              :logger #ig/ref :picture-gallery.infrastructure.logger/logger}
 :picture-gallery.infrastructure.sql.migrate/migration  {:env #ig/ref :picture-gallery.infrastructure.env/env
                                                         :operation :migrate
                                                         :logger #ig/ref :picture-gallery.infrastructure.logger/logger}
 :picture-gallery.infrastructure.router.core/router {:env #ig/ref :picture-gallery.infrastructure.env/env
                                                     :firebase #ig/ref :picture-gallery.infrastructure.firebase.core/firebase}
 :picture-gallery.infrastructure.server/server {:env #ig/ref :picture-gallery.infrastructure.env/env
                                                :router #ig/ref :picture-gallery.infrastructure.router.core/router
                                                :port 3000}}
```

実行してみます。

<details><summary>実行例</summary>

    dev> (restart)
    2021-03-18T14:44:32.012Z f04004b3a5e3 INFO [picture-gallery.infrastructure.sql.sql:62] - close connection pool ...
    2021-03-18T14:44:32.015Z f04004b3a5e3 INFO [com.zaxxer.hikari.HikariDataSource:350] - HikariPool-1 - Shutdown initiated...
    2021-03-18T14:44:32.025Z f04004b3a5e3 INFO [com.zaxxer.hikari.HikariDataSource:352] - HikariPool-1 - Shutdown completed.
    2021-03-18T14:44:32.026Z f04004b3a5e3 INFO [picture-gallery.infrastructure.server:12] - stop server
    2021-03-18T14:44:32.034Z f04004b3a5e3 INFO [org.eclipse.jetty.server.AbstractConnector:381] - Stopped ServerConnector@5d49c08a{HTTP/1.1, (http/1.1)}{0.0.0.0:3000}
    :reloading ()
    loading environment via environ
    running in  dev
    log-level  :info
    migrations-folder migrations
    database options {:adapter postgresql, :database-name pic_gallery, :username meguru, :password emacs, :server-name dev_db, :port-number 5432}
    orchestra instrument is active
    2021-03-18T14:44:32.056Z f04004b3a5e3 INFO [picture-gallery.infrastructure.firebase.core:18] - connectiong to firebase with  ServiceAccountCredentials{clientId=107926774701607421850, clientEmail=firebase-adminsdk-l42c5@sample-picture-gallery-c12rb.iam.gserviceaccount.com, privateKeyId=80f9a8cceb5036d0a96f73a108fa485aeed314a4, transportFactoryClassName=com.google.auth.oauth2.OAuth2Utils$DefaultHttpTransportFactory, tokenServerUri=https://oauth2.googleapis.com/token, scopes=[], serviceAccountUser=null, quotaProjectId=null}
    set logger with log-level :info
    2021-03-18T14:44:32.056Z f04004b3a5e3 INFO [picture-gallery.infrastructure.router.core:77] - router got: env {:database-options {:adapter "postgresql", :database-name "pic_gallery", :username "meguru", :password "emacs", :server-name "dev_db", :port-number 5432}, :running "dev", :migrations-folder "migrations", :log-level :info, :firebase-credentials #object[com.google.auth.oauth2.ServiceAccountCredentials 0xb74d590 "ServiceAccountCredentials{clientId=107926774701607421850, clientEmail=firebase-adminsdk-l42c5@sample-picture-gallery-c12rb.iam.gserviceaccount.com, privateKeyId=80f9a8cceb5036d0a96f73a108fa485aeed314a4, transportFactoryClassName=com.google.auth.oauth2.OAuth2Utils$DefaultHttpTransportFactory, tokenServerUri=https://oauth2.googleapis.com/token, scopes=[], serviceAccountUser=null, quotaProjectId=null}"]}
    2021-03-18T14:44:32.060Z f04004b3a5e3 INFO [picture-gallery.infrastructure.server:7] - server is running in port 3000
    2021-03-18T14:44:32.060Z f04004b3a5e3 INFO [picture-gallery.infrastructure.server:8] - router is  clojure.lang.AFunction$1@a8104b8
    2021-03-18T14:44:32.061Z f04004b3a5e3 INFO [org.eclipse.jetty.server.Server:375] - jetty-9.4.36.v20210114; built: 2021-01-14T16:44:28.689Z; git: 238ec6997c7806b055319a6d11f8ae7564adc0de; jvm 11.0.9+11
    2021-03-18T14:44:32.063Z f04004b3a5e3 INFO [org.eclipse.jetty.server.AbstractConnector:331] - Started ServerConnector@337d116a{HTTP/1.1, (http/1.1)}{0.0.0.0:3000}
    2021-03-18T14:44:32.063Z f04004b3a5e3 INFO [org.eclipse.jetty.server.Server:415] - Started @47795489ms
    2021-03-18T14:44:32.064Z f04004b3a5e3 INFO [picture-gallery.infrastructure.sql.migrate:20] - run migration with operation :migrate (rollback-amount is  nil )
    Applying 001_users
    2021-03-18T14:44:32.148Z f04004b3a5e3 INFO [com.zaxxer.hikari.HikariDataSource:80] - HikariPool-2 - Starting...
    2021-03-18T14:44:32.152Z f04004b3a5e3 INFO [com.zaxxer.hikari.HikariDataSource:82] - HikariPool-2 - Start completed.
    2021-03-18T14:44:32.153Z f04004b3a5e3 INFO [picture-gallery.infrastructure.sql.sql:56] - setup connection pool ...
    ;; => :resumed

</details>

<a id="orgf5bb353"></a>

# interface の実装

Firebase Auth の token のデコード、SQL の実行部分は interface にあたるので、実装していきます。 この部分は、usecase との依存関係の方向上、インターフェースを介して (名前の通りですね) やり取りをする必要があるので、 Clojure におけるインターフェースの記述方法一つ、 `defprotocol` を利用して実装します。

<a id="org66d8fac"></a>

## Firebase Auth の token デコード機構

作る前にどんな機能があれば考えます (一つだけですが)。

- firebase auth の id-token をデコードする

  デコードに際して出てくるエラーは次のように分類します。簡単のため、try-catch 文を使って、引っかかった 例外のメッセージからエラーを分類します (エラーコードは全部 INVALID<sub>ARGUMENT</sub> です)。

  - 不正なトークン (トークンとして成立していない) &ldquo;Failed to parse &#x2026;&rdquo; というエラーが発生したとき
  - 期限切れのトークン &ldquo;Firebase xxx has expired &#x2026; &rdquo; というエラーが発生したとき
  - 不明なエラー (それ以外のエラー) それ以外

仕様が見えてきたところで実装してみます ([9.1](#org860b3cb))。

```clojure
(ns picture-gallery.interface.gateway.auth.core
  (:require [picture-gallery.interface.gateway.auth.firebase :as firebase-impl]
            [integrant.core :as ig]))

(defprotocol Auth
  (decode-id-token [this id-token]))

(extend-protocol Auth
  picture_gallery.infrastructure.firebase.core.FirebaseBoundary
  (decode-id-token [{:keys [firebase]} id-token]
    (firebase-impl/safe-decode-token (:firebase-auth firebase) id-token)))
```

```clojure
(ns picture-gallery.interface.gateway.auth.firebase
  (:require [clojure.string]
            [picture-gallery.domain.error :as error-domain]
            [picture-gallery.utils.error :refer [err->>]]))

(defn decode-token [firebase-auth id-token]
  (-> firebase-auth
      (.verifyIdToken id-token)
      .getUid))

(defn expired-id-token? [cause]
  (if (clojure.string/includes? cause "expired")
    [nil error-domain/expired-id-token]
    [cause nil]))

(defn invalid-id-token? [cause]
  (if  (clojure.string/includes? cause "Failed to parse")
    [nil error-domain/invalid-id-token]
    [cause nil]))

(defn unknown-id-token? [_]
  [nil error-domain/unknown-id-token])

(defn safe-decode-token [firebase-auth id-token]
  (try
    {:status :success
     :body {:decoded-id-token (decode-token firebase-auth id-token)}}
    (catch Exception e
      {:status :failure
       :body (second
              (err->>
               (or (.getMessage e) "unknown")
               expired-id-token?
               invalid-id-token?
               unknown-id-token?))})))
```

試してみます (※実際はこうなるまで無限回試行錯誤してます)。

```clojure
(def system
  (ig/init {:picture-gallery.infrastructure.env/env {}
            :picture-gallery.infrastructure.firebase.core/firebase {:env (ig/ref :picture-gallery.infrastructure.env/env)}}))

(decode-id-token
 (:picture-gallery.infrastructure.firebase.core/firebase system) "Hello")
;; => {:status :failure, :body {:status 400, :body {:code 1702, :message the firebase token is invalid}}}
(decode-id-token
 (:picture-gallery.infrastructure.firebase.core/firebase system) "<expired token>")
;; => {:status :failure, :body {:status 400, :body {:code 1701, :message the firebase token is expired}}}
 (decode-id-token
  (:picture-gallery.infrastructure.firebase.core/firebase system) "<valid token>")
;; => {:status :success, :body {:decoded-id-token <decoded-token>}}
(ig/halt! system)
```

参考:

- <https://github.com/firebase/firebase-admin-java/blob/d8b1583002d60568106bf4a7ba2d5bcbbb6c0463/src/main/java/com/google/firebase/auth/FirebaseTokenVerifierImpl.java>

<a id="orgea8d8f9"></a>

## SQL の実行機構

使うライブラリは、 next.jdbc (<https://github.com/seancorfield/next-jdbc>) です。 next.jdbc は非常に低レベルから JDBC (Java の DB 操作を行うためのライブラリ) を使うことができるライブラリで、チュートリアルがしっかりしているライブラリです。

<a id="orge86d440"></a>

### PostgreSQL との接続

<a id="orga53a509"></a>

# interface の組み込み

<a id="org526fd55"></a>

# 動作確認

<a id="orgce21e80"></a>

# 捕捉

<a id="org860b3cb"></a>

## 実装してみます

期待する機能が実装可能かどうかを REPL を動かしながら試す。 実装可能であれば仕様を固めてテストを書いたり実装を進めたりして、実装できなそうであれば、仕様を見直す。

特に実装が不透明なライブラリを使うときには、先にきっとこんなはずなテストを書いてから実装するよりも、こちらのほうが失敗が少ないので (n=1 orz)、Clojure や Python など使う際には、ぜひ REPL やインタプリタを活用してみて下さい。
