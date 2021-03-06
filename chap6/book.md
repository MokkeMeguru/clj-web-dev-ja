- [re-frame と DDD、関数型プログラミング](#org01850c2)
  - [re-frame と DDD](#org8fe3487)
  - [re-frame と関数型プログラミング](#orgdd56d49)
- [プロジェクトのセットアップ](#org99c3a10)
- [ClojureScript ライブラリの追加、npm ライブラリの追加](#orgdc5863e)
- [index.html の更新](#org176902e)
- [ルーティングとホームの実装](#orge8e65c8)
  - [ホーム画面をブラウザに表示する](#orgc3ffb60)
  - [Clojure Garden で CSS を書いてみる](#org6bbeaf7)
  - [SPA とルーティング](#org0af39d5)
- [付録](#org1f7e4f7)
  - [ルーティングの検証](#org48685be)

長いサーバサイドの実装は一旦終了とし、ここからはクライアントサイドの実装を行います。

クライアントサイドの実装では、re-frame (<https://github.com/day8/re-frame>) という ClojureScript x React x Redux (のようなもの) な SPA 開発を支援するフレームワークを利用します。 サーバサイドと同様に、Clean Architecture で仕上げることもできますが、クライアントサイドに Clean Architecture が必要とされるようなデータ操作を行わせるケースを想定しないために、フレームワークを用いることを選択しました。

<a id="org01850c2"></a>

# re-frame と DDD、関数型プログラミング

re-frame で重要となるアイテムは、 **db** **subs(subscribers)** **events** **views** の 4 つです。

簡単のために図示すると、それぞれのアイテムは下記のような役割を担っています。

![img](./img/re-frame-cycle.png)

重要なのは、中央のサイクル部です。 データモデル (DB) と HTML (Views) とのやり取りを、 **events** 、 **subs** のそれぞれが **単方向ずつ** 担っているのが確認できます。 この単方向のベクトルが re-frame 特徴である、データの流れを追いやすく、検証が行いやすい理由の一つになっています。

参考

- <https://qiita.com/lagenorhynque/items/3770e520bee0007e417c>

<a id="org8fe3487"></a>

## re-frame と DDD

上記のサイクルを達成するためには、いくつかあったほうが良い性質があります。

一つは、DB と Views で扱うコンテンツが共通である、という性質です。 言い換えれば、ドメインごとに **db** **events** **subs** **views** が整備されていたほうが良い、ということになります。 実際に、 re-frame のプロジェクトのディレクトリ構造は次のようになることが推奨されています。

    project
         |- content_A
         |        |- db.cljs
         |        |- events.cljs
         |        |- subs.cljs
         |        `- views.cljs
         |- content_B
         |        |- db.cljs
         |        |- events.cljs
         |        |- subs.cljs
         |        `- views.cljs
         |- ...

これはまさに ドメイン駆動のディレクトリ構造に近いものです。 また、HTML を考えれば、1 画面 1 コンテンツを半ば強制的に実現させることになります (勿論組み合わせることもできます)。

<a id="orgdd56d49"></a>

## re-frame と関数型プログラミング

当然のことながら、re-frame は ClojureScript で記述されるフレームワークです。 そして、ClojureScript は、Clojure の JavaScript サポート版のような立ち位置で、関数型言語の一つと言えます。

関数型言語の多くは、副作用という言葉に敏感です。 副作用というのは、例えば db を書き換えたり、HTML にデータを入力したり、API を叩いて出力を受け取ったり、といった、データの入出力やデータを不可逆的に操作することを指します。

re-frame では、 **views** と **db** 、 **subs** では (基本的に) 副作用のある処理を書きません。 この性質のために、 re-frame は他の JavaScript (や TypeScript) のフレームワークに比べてもテストの実装が容易です (副作用がない部分は入力と出力の関係が明らか)。

ただし、 re-frame はこの性質のために、ゴリゴリのアニメーション処理が必要がコードなどはあまり得意では (そもそも React 自体が brabra &#x2026;) ないです。

<a id="org99c3a10"></a>

# プロジェクトのセットアップ

re-frame はフレームワークなので、アプリのセットアップにテンプレートを使うことができます。

次のオプションをつけてプロジェクトを初期化します。

```shell
lein new re-frame pic-gallery-web +garden +10x +cider +test
```

`+xxx` はオプションを表しています。今回追加したオプションは、次のとおりです。

- garden: clojure で css を書くライブラリの追加
- 10x: デバッグツールの追加
- cider: emacs での開発支援ツールの追加
- test: テストのテンプレートの追加

現在のディレクトリ構造は次の通り

    pic-gallery-web
    ├── README.md
    ├── dev
    │   └── cljs
    │       └── user.cljs
    ├── karma.conf.js
    ├── package.json
    ├── project.clj
    ├── resources
    │   └── public
    │       └── index.html
    ├── src
    │   ├── clj
    │   │   └── pic_gallery_web
    │   │       └── css.clj
    │   └── cljs
    │       ├── deps.cljs
    │       └── pic_gallery_web
    │           ├── config.cljs
    │           ├── core.cljs
    │           ├── db.cljs
    │           ├── events.cljs
    │           ├── subs.cljs
    │           └── views.cljs
    └── test
        └── cljs
            └── pic_gallery_web
                └── core_test.cljs

ここで、 React をはじめとする re-frame の依存ライブラリをインポートするために、次のコマンドを実行します。

```shell
lein deps
```

さらに、初期化のために一度、 次のコマンドを実行する必要があります。

    $ lein watch
    OpenJDK 64-Bit Server VM warning: Options -Xverify:none and -noverify were deprecated in JDK 13 and will likely be removed in a future release.
    lein-shadow - running: npm --version
    lein-shadow - 'npm' version 7.7.5

    lein-shadow - found existing package.json file at /run/media/meguru/P/Github/clj-web-dev/chap6/pic-gallery-web/package.json
    lein-shadow - reading node dependencies from /run/media/meguru/P/Github/clj-web-dev/chap6/pic-gallery-web/src/cljs/deps.cljs
    lein-shadow - running: npm ci

    lein-shadow - node package manager successfully built node_modules directory:

    added 196 packages, and audited 197 packages in 1m

    found 0 vulnerabilities

    lein-shadow -  node package shadow-cljs@2.11.24 does not exist in /run/media/meguru/P/Github/clj-web-dev/chap6/pic-gallery-web/package.json!:devDependencies. Adding.
    lein-shadow -  node package karma@6.2.0 does not exist in /run/media/meguru/P/Github/clj-web-dev/chap6/pic-gallery-web/package.json!:devDependencies. Adding.
    lein-shadow -  node package karma-chrome-launcher@3.1.0 does not exist in /run/media/meguru/P/Github/clj-web-dev/chap6/pic-gallery-web/package.json!:devDependencies. Adding.
    lein-shadow -  node package karma-cljs-test@0.1.0 does not exist in /run/media/meguru/P/Github/clj-web-dev/chap6/pic-gallery-web/package.json!:devDependencies. Adding.
    lein-shadow -  node package karma-junit-reporter@2.0.1 does not exist in /run/media/meguru/P/Github/clj-web-dev/chap6/pic-gallery-web/package.json!:devDependencies. Adding.
    lein-shadow - running: npm install --save-dev --save-exact shadow-cljs@2.11.24 karma@6.2.0 karma-chrome-launcher@3.1.0 karma-cljs-test@0.1.0 karma-junit-reporter@2.0.1
    lein-shadow - node dev packages added successfully:

    added 197 packages, and audited 394 packages in 23s

    9 packages are looking for funding
      run `npm fund` for details

    found 0 vulnerabilities

    lein-shadow - running shadow-cljs...
    running: npm install --save --save-exact react@17.0.1 react-dom@17.0.1 highlight.js@10.7.1

    added 6 packages, and audited 400 packages in 4s

    9 packages are looking for funding
      run `npm fund` for details

    found 0 vulnerabilities
    shadow-cljs - HTTP server available at http://localhost:8280
    shadow-cljs - HTTP server available at http://localhost:8290
    shadow-cljs - server version: 2.11.24 running at http://localhost:9630
    shadow-cljs - nREPL server started on port 8777
    shadow-cljs - watching build :app
    shadow-cljs - watching build :browser-test
    [:app] Configuring build.
    shadow-cljs - watching build :karma-test
    [:browser-test] Configuring build.
    [:karma-test] Configuring build.
    [:app] Compiling ...
    [:karma-test] Compiling ...
    [:browser-test] Compiling ...
    ------ WARNING #1 -  -----------------------------------------------------------
     Resource: node_modules/highlight_DOT_js/lib/core.js:1475:23
     Missing "..." in type annotation for rest parameter.
    --------------------------------------------------------------------------------
    [:browser-test] Build completed. (209 files, 208 compiled, 0 warnings, 24.66s)
    [:karma-test] Build completed. (146 files, 146 compiled, 0 warnings, 15.39s)
    [:app] Build completed. (533 files, 532 compiled, 0 warnings, 25.15s)
    C-c C-c
    $

プロジェクトの REPL 実行方法は、各エディタの利用方法に従って下さい。

例えば、Emacs であれば、 `lein watch` した状態で、 cider-connect-clojurescript (コマンド) -> 8777 (入力) -> shadow (選択) -> app (入力) とすることで REPL に接続することができます。

なお、ClojureScript は Hot Loading が有効なので、コードを保存したものが現在画面に表示されるものになります。

参考:

- <https://github.com/day8/re-frame-template>

<a id="orgdc5863e"></a>

# ClojureScript ライブラリの追加、npm ライブラリの追加

ClojureScript も Clojure と同様に元言語のライブラリを利用することができます。 本章では、ClojureScript、npm のライブラリを両方追加します。

まずは ClojureScript のライブラリを project.clj へ追加します。

```clojure
(defproject pic-gallery-web "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.773"
                  :exclusions [com.google.javascript/closure-compiler-unshaded
                               org.clojure/google-closure-library
                               org.clojure/google-closure-library-third-party]]
                 [thheller/shadow-cljs "2.11.24"]
                 [reagent "1.0.0"]
                 [re-frame "1.2.0"]
                 [day8.re-frame/tracing "0.6.2"]
                 [garden "1.3.10"]
                 [ns-tracker "0.4.0"]

                 ;; add these libraries
                 ;; routing
                 [metosin/reitit "0.5.10"]
                 [metosin/reitit-malli "0.5.10"]

                 ;; http request
                 [day8.re-frame/http-fx "0.2.1"]

                 ;; testing
                 [day8.re-frame/test "0.1.5"]]

  :plugins [[cider/cider-nrepl "0.25.6"]
            [lein-shadow "0.3.1"]
            [lein-garden "0.3.0"]
            [lein-shell "0.5.0"]
            [lein-pprint "1.3.2"]]

  :min-lein-version "2.9.0"

  :jvm-opts ["-Xmx1G"]

  :source-paths ["src/clj" "src/cljs"]

  :test-paths   ["test/cljs"]
  ;; ...
  :prep-tasks [["garden" "once"]])
```

次に JavaScript ライブラリとして、bulma と node-sass をインストールします。 bulma は CSS フレームワークで、node-sass を経由することで、フレームワーク内の CSS の一部分を変更することができます。

CSS を 1 から手書きするのは大変 + Sass を使うほうが CSS を使うよりもよりも変数管理が楽 + bulma フレームワークを使いたい、という背景から、上述した 2 つのライブラリを追加します。

```shell
npm install node-sass --save-dev
npm install bulma --save-dev
```

ディレクトリ内の、 `package.json` が次のように更新されます。

```json
{
  "name": "pic-gallery-web",
  "devDependencies": {
    "bulma": "^0.9.2",
    "node-sass": "^5.0.0"
  }
}
```

さらに、 `package.json` を編集して、 node-sass を使うための npm CLI コマンドを追加します。

```json
{
  "name": "pic-gallery-web",
  "devDependencies": {
    "bulma": "^0.9.2",
    "node-sass": "^5.0.0"
  },
  "scripts": {
    "css-build": "node-sass --omit-source-map-url sass/mystyles.scss resources/public/css/mystyles.css",
    "css-watch": "npm run css-build -- --watch"
  }
}
```

- npm run css-build

  scss -> css へコンパイルするためのコマンド

- npm run css-watch

  scss -> css への変換を Hot Loading するためのコマンド

試しに、 次のような `sass/mystyles.scss` を追加します。

```scss
@charset "utf-8";
$navbar-breakpoint: 760px;
@import "../node_modules/bulma/bulma";
```

    $ npm run css-build

    > css-build
    > node-sass --omit-source-map-url sass/mystyles.scss resources/css/mystyles.css

    Rendering Complete, saving .css file...
    Wrote CSS to /run/media/meguru/P/Github/clj-web-dev/chap6/pic-gallery-web/resources/css/mystyles.css

これで、 bulma のカスタムされた css コードが `resources/css/mystyles.css` へ追加されました。

<a id="org176902e"></a>

# index.html の更新

以前、firebase auth を使うために、仮のクライアントサイドコードを実装しましたが、その実装をこちらの re-frame のコードにも移植します。

移植には前回と同様に `index.html` を編集する必要があります。

初期状態は次のとおりです。

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width,initial-scale=1" />
    <link href="css/screen.css" rel="stylesheet" type="text/css" />
    <title>pic-gallery-web</title>
  </head>
  <body>
    <noscript>
      pic-gallery-web is a JavaScript app. Please enable JavaScript to continue.
    </noscript>
    <div id="app"></div>
    <script src="js/compiled/app.js"></script>
  </body>
</html>
```

ここに、firebase auth を利用するためのコードの追加、そして、前章で追加した bulma の css コードの追加を行うと、次のようになります。

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width,initial-scale=1" />

    <!-- js libaries -->
    <script
      defer
      src="https://use.fontawesome.com/releases/v5.6.0/js/all.js"
    ></script>
    <script
      src="https://code.jquery.com/jquery-3.5.1.slim.js"
      integrity="sha256-DrT5NfxfbHvMHux31Lkhxg42LY6of8TaYyK50jnxRnM="
      crossorigin="anonymous"
    ></script>
    <script src="https://www.gstatic.com/firebasejs/ui/4.7.1/firebase-ui-auth__ja.js"></script>

    <!-- css -->
    <link
      type="text/css"
      rel="stylesheet"
      href="https://www.gstatic.com/firebasejs/ui/4.7.1/firebase-ui-auth.css"
    />
    <link href="/css/mystyles.css" rel="stylesheet" type="text/css" />
    <link href="css/screen.css" rel="stylesheet" type="text/css" />

    <title>pic-gallery-web</title>
  </head>
  <body>
    <noscript>
      pic-gallery-web is a JavaScript app. Please enable JavaScript to continue.
    </noscript>
    <div id="app"></div>

    <!-- firebase -->
    <script src="https://www.gstatic.com/firebasejs/7.23.0/firebase-app.js"></script>
    <script src="https://www.gstatic.com/firebasejs/7.23.0/firebase-analytics.js"></script>
    <script src="https://www.gstatic.com/firebasejs/7.23.0/firebase-auth.js"></script>

    <script type="text/javascript">
      // set your values from firebase project
      // --------------------------------------------
      var apiKey = "AIzaSyA-AfxCZtmMBfbA6xJsDqA5wSNmod8VrIk";
      var projectId = "sample-picture-gallery-c12rb";
      // --------------------------------------------

      var authDomain = projectId + ".firebaseapp.com";
      var firebaseConfig = {
        apiKey: apiKey,
        authDomain: authDomain,
        projectId: projectId,
      };

      // Initialize Firebase
      firebase.initializeApp(firebaseConfig);
    </script>

    <script src="js/compiled/app.js"></script>
  </body>
</html>
```

また、近年ほぼ必須となっている ServiceWorker の追加は次のようになります。

```html
<html>
  <body>
    <!-- ... -->
    <script src="/js/compiled/app.js"></script>
    <script>
      if ("serviceWorker" in navigator) {
        navigator.serviceWorker.register("/sw.js").then(function () {
          console.log("service worker registered");
        });
      }
    </script>
  </body>
</html>
```

`sw.js` は次の通り

```javascript
self.addEventListener("install", function (e) {
  console.log("[ServiceWorker] Install");
});

self.addEventListener("activate", function (e) {
  console.log("[ServiceWorker] Activate");
});
```

PWA のための マニュフェストファイルも追加しましょう (ワガママ)。

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width,initial-scale=1" />

    <!-- js libaries -->
    <script
      defer
      src="https://use.fontawesome.com/releases/v5.6.0/js/all.js"
    ></script>
    <script
      src="https://code.jquery.com/jquery-3.5.1.slim.js"
      integrity="sha256-DrT5NfxfbHvMHux31Lkhxg42LY6of8TaYyK50jnxRnM="
      crossorigin="anonymous"
    ></script>
    <script src="https://www.gstatic.com/firebasejs/ui/4.7.1/firebase-ui-auth__ja.js"></script>

    <!-- css -->
    <link
      type="text/css"
      rel="stylesheet"
      href="https://www.gstatic.com/firebasejs/ui/4.7.1/firebase-ui-auth.css"
    />
    <link href="/css/mystyles.css" rel="stylesheet" type="text/css" />
    <link href="css/screen.css" rel="stylesheet" type="text/css" />

    <!-- for PWA -->
    <link
      rel="apple-touch-icon"
      sizes="180x180"
      href="/icons/apple-touch-icon.png"
    />
    <link
      rel="icon"
      type="image/png"
      sizes="32x32"
      href="/icons/favicon-32x32.png"
    />
    <link
      rel="icon"
      type="image/png"
      sizes="16x16"
      href="/icons/favicon-16x16.png"
    />
    <link rel="manifest" href="pic-gallery.webmanifest" />

    <title>pic-gallery-web</title>
  </head>
  <body>
    ...
  </body>
</html>
```

`pic-gallery.webmanifest` は次の通り (icon 部は各自用意して下さい)。

```json
{
  "name": "Pic Gallery",
  "short_name": "PicGallery",
  "description": "Pic Gallery: Show your Picture and its Memory as your Gallery",
  "icons": [
    {
      "src": "/icons/android-chrome-192x192.png",
      "sizes": "192x192",
      "type": "image/png"
    },
    {
      "src": "/icons/android-chrome-512x512.png",
      "sizes": "512x512",
      "type": "image/png"
    }
  ],
  "display": "standalone"
}
```

参考:

- <https://developer.mozilla.org/ja/docs/Web/Progressive_web_apps/Offline_Service_workers>
- <https://developer.mozilla.org/ja/docs/Web/Progressive_web_apps/Installable_PWAs>
- <https://realfavicongenerator.net/>

<a id="orge8e65c8"></a>

# ルーティングとホームの実装

re-frame の機構を用いた実装の前に、ルーティングとシンプルな View を用いたホームの実装を行います。

まずは Home 画面を切り出すためにディレクトリ構造を見直します。

    src/cljs/pic_gallery_web
    ├── config.cljs
    ├── core.cljs
    ├── db.cljs
    ├── events.cljs
    ├── subs.cljs
    └── views.cljs

上の形ですと、別のコンテンツを追加したときに困るので、 `services` というディレクトリを作ります。 その中に `home/views.cljs` を作り、ホーム画面の View とします。

```clojure
(ns pic-gallery-web.services.home.views)

(defn about []
  [:<>
   [:p.subtitle "Pic Gallery とは"]])

(defn how-to-use []
  [:div.content
   [:p.title "使い方"]])

(def home-body
  [:<>
   [about]
   [:hr]
   [how-to-use]])

(def home-content
  {:title "Welcome to Pic Gallery"
   :body home-body})

(def home-page
  [:div.container.pt-5 {:style {:max-width "640px"}}
   [:div.titles
    [:p.title (:title home-content)]
    (:body home-content)]])
```

ここで、Clojure (の Hiccup という記法) での HTML の書き方を紹介します。

Hiccup は Clojure で HTML を表現するためのライブラリです。

Clojure のベクトル `[]` の第一引数が html tag を表し、オプショナルな第二引数が html element の属性を表します。そして、第三引数以降が中身のテキスト、子要素表します。

```clojure
;; [<html tag> {<attribute-key> <attribute-value>} <value or children of html element>]
;; e.g.
[:div {:style {:max-width "640px"}} [:p "Hello"] [:ul [:li "item_1"] [:li "item_2"]]]
```

また、第一引数について、 `.class_name` でクラス名を、 `#id_name` で ID を追加できます。

参考:

- <https://github.com/weavejester/hiccup>

<a id="orgc3ffb60"></a>

## ホーム画面をブラウザに表示する

home の View は今の所静的なものであるので、 db や subs、events は不要です。 これを実際の画面に反映してみます。

まず、 `src/cljs/pic_gallery_web` 下の、 db, events, subs, views を `services` の中に移動します。

```clojure
;; services/main/db.cljs
(ns pic-gallery-web.services.main.db)

(def default-db
  {:name "re-frame"})

;; services/main/events.cljs
(ns pic-gallery-web.services.main.events
  (:require
   [re-frame.core :as re-frame]
   [pic-gallery-web.services.main.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

;; services/main/subs.cljs
(ns pic-gallery-web.services.main.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))


;; services/main/views.cljs
(ns pic-gallery-web.services.main.views
  (:require [re-frame.core :as re-frame]
            [pic-gallery-web.main.subs :as subs]
            [pic-gallery-web.services.home.views :as home-views]))

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:h1 "Hello from " @name]
     home-views/home-page]))
```

`pic_gallery_web/core.cljs` を移動した namespace を読みに行くことができるよう編集します。

これをブラウザで確認すると、次のようになります。

![img](./img/init_home.png)

だいぶ質素ですが、CSS を何も適用していないせいです。妖怪のせいではないです。

<a id="org6bbeaf7"></a>

## Clojure Garden で CSS を書いてみる

Clojure は Java、 JavaScript、HTML も書けますが、 CSS も書けます。

Clojure で CSS を書くためのライブラリとして、 Garden (<https://github.com/noprompt/garden>) があります。

詳細な使い方はドキュメントに委託するとして、簡単な使い方を紹介します。

`src/clj/pic_gallery_web/css.clj` に Garden の初期コードが含まれています。

```clojure
(ns pic-gallery-web.css
  (:require [garden.def :refer [defstyles]]))

(defstyles screen
  [:body {:color "red"}]
)
```

コンパイルするには次の選択肢が用意されています。

- lein garden once 一度だけコンパイルする
- lein garden auto Hot loading を有効にする

  \$ lein garden auto
  OpenJDK 64-Bit Server VM warning: Options -Xverify:none and -noverify were deprecated in JDK 13 and will likely be removed in a future release.
  Compiling Garden...
  Compiling "resources/public/css/screen.css"...
  Wrote: resources/public/css/screen.css
  Successful

コンパイル先の CSS ファイルは `resources/public/css/screen.css` に示されるものになります。

```css
body {
  color: red;
}
```

Hiccup と同様に、ベクトルとマップを使って表現されていることが観察できると思います。

試しに、 body-color を black にするとブラウザでの表示にある、 **Hello from re-frame** の文字が黒くなっていることを確認することができます。

応用として、複雑な CSS を Garden を用いて書いてみます。

home.views.cljs を見てみると、 `#how-to-use` 内に `content>title` クラスが確認できます。 今の画面ですと、ちょっと文字が大きいかもしれないので、調節してみます。

```clojure
(defn how-to-use []
  [:div.content
   [:p.title "使い方"]])
```

```clojure
(defstyles screen
  [:body {:color "black"}]
  [:.content
   [:.title {:font-size "1.5rem"}]])

;; =>
;; body {
;;   color: black;
;; }

;; .content .title {
;;   font-size: 1.5rem;
;; }
```

ブラウザの画面を確認すると、調節されていることが確認できます。 ![img](./img/cssed_home.png)

<a id="org0af39d5"></a>

## SPA とルーティング

re-frame は SPA の開発フレームワークです。 SPA ということは URL に基づいてクライアント側でルーティングを行う必要があります。

今回はこのルーティングを、サーバサイド開発でも用いた、 reitit (<https://github.com/metosin/reitit>) で実装します。

以前も紹介しましたが、 Clojure と ClojureScript は同一構文を用いた言語であり、低レイヤーの部分を入れ替えれば全く同じコードを用いることができます。 reitit はこの特徴を生かし、サーバサイド、クライアントサイド両方で同様の構文を利用できるライブラリとして成立している非常に有用なライブラリです。(Java と JavaScript が同一言語かどうか、というプログラム初心者を判別する話がありますが、reitit では本当に同一であるかのように扱うことができます)

今回はまず、Home 画面が &ldquo;/&rdquo; で表示されるようにルーティングを行っていきます。

ルーティング先を表すキーを domain として記述します。 (この部分は Clean Architecture を意識しています)

```clojure
(ns pic-gallery-web.domain.routes)

(def ::home :_home)
```

```clojure
(ns pic-gallery-web.routers
  (:require
   ;; re-frame
   [re-frame.core :as re-frame]

   ;; reitit
   [reitit.coercion :as coercion]
   [reitit.coercion.spec]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend :as rf]

   [pic-gallery-web.services.home.views :as home-views]
   [pic-gallery-web.services.main.events :as main-events]
   [pic-gallery-web.domain.routes :as routes-domain]))

(def home-controllers
  [{:start (fn [_]
             (println "entering home"))
    :stop (fn [_]
            (println "exit home"))}])

(def routes
  ["/"
   [""
    {:name ::routes-domain/home
     :view home-views/home-page
     :link-text "app-home"
     :controllers home-controllers}]])

(def router (rf/router routes))

(defn on-navigate [new-match]
  (when new-match
    (re-frame/dispatch [::main-events/navigated new-match])))

(defn init-routes! []
  (rfe/start!
   router
   on-navigate
   {:use-fragment false
    ;; if true, use # routing
    ;; if false, use http-histroy API
    }))
```

re-frame と組み合わせるための **events** を追加します。 (ルーティングは外部から与えられる URL に基づいて起きるので events に書きます)

```clojure
(ns pic-gallery-web.services.main.events
  (:require
   [re-frame.core :as re-frame]
   [pic-gallery-web.services.main.db :as db]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [day8.re-frame.tracing :refer-macros [fn-traced]]))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
            db/default-db))

;; navigation
(re-frame/reg-fx
 ::navigate!
 (fn [route]
   (apply rfe/push-state route)))

(re-frame/reg-event-fx
 ::navigate
 (fn [_cofx [_ & route]]
   {::navigate! route}))

(re-frame/reg-event-db
 ::navigated
 (fn [db [_ new-match]]
   (let [old-match (:current-route db)
         controllers (rfc/apply-controllers (:controllers old-match) new-match)]
     (when-not (= new-match old-match) (.scrollTo js/window 0 0))
     (assoc db :current-route (assoc new-match :controllers controllers)))))
```

最後に、re-frame の起動と同時に reitit のルーティングを有効化する設定をします。

```clojure
(ns pic-gallery-web.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [pic-gallery-web.services.main.events :as main-events]
   [pic-gallery-web.services.main.views :as main-views]
   [pic-gallery-web.config :as config]
   [pic-gallery-web.routers :as routers]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (routers/init-routes!) ;; add here!
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [main-views/main-panel] root-el)))

(defn init []
  (re-frame/dispatch-sync [::main-events/initialize-db])
  (dev-setup)
  (mount-root))
```

`localhost:8280/` へアクセスし、コンソールログを見ると次のようなログが見えます。

    dev mode
    entering home
    (index):59 service worker registered
    browser.cljs:20 shadow-cljs: #52 ready!
    browser.cljs:20 shadow-cljs: load JS pic_gallery_web/core.cljs

&ldquo;entering home&rdquo; は、pic-gallery-web.routers/home-controller にある、 `:start` に書いた関数の実行結果になります。 controller の `:start` はルーティング先のページに入った際に呼び出される関数で、 `:stop` はルーティング先のページから出た際に呼び出される関数です。

例えば自動ログインなどを実装する際には、この機能を利用することが想定できます。

参考:

- <https://github.com/metosin/reitit/tree/master/examples/frontend-re-frame>

<a id="org1f7e4f7"></a>

# 付録

<a id="org48685be"></a>

## ルーティングの検証

ルーティングが正しく行われているかを検証するには、次のような関数をテストに実装することで達成できます。 特に URL の path にユーザ ID などを用いる際には、以下のような方法でテストを書くことが推奨できます。

```clojure
(= ::routes-domain/home (-> (rf/match-by-path router "/") :data :name))
```

例えばこんな感じに書きます。(test/cljs/pic_gallery_web/routers_test.cljs)

```clojure
(ns pic-gallery-web.routers-test
  (:require [pic-gallery-web.routers :as sut]
            [pic-gallery-web.domain.routes :as routes-domain]
            [reitit.frontend :as rf]
            [cljs.test :as t :include-macros true]))

(t/deftest route-match
  (t/testing "home"
    (t/is ::routes-domain/home (-> (rf/match-by-path sut/router "/") :data :name))))

```

core_test を以下のように修正します。(わざと fail するテストが書かれています。)

```clojure
(ns pic-gallery-web.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [pic-gallery-web.core :as core]))

;; (deftest fake-test
;;   (testing "fake description"
;;     (is (= 1 2))))
```

修正できたら、次のコマンドを実行して下さい。

    $ lein ci
    [:karma-test] Compiling ...
    [:karma-test] Build completed. (175 files, 2 compiled, 0 warnings, 3.89s)
    29 03 2021 09:34:40.099:INFO [karma-server]: Karma v6.2.0 server started at http://localhost:9876/
    29 03 2021 09:34:40.100:INFO [launcher]: Launching browsers ChromeHeadless with concurrency unlimited
    29 03 2021 09:34:40.103:INFO [launcher]: Starting browser ChromeHeadless
    29 03 2021 09:34:40.341:INFO [Chrome Headless 88.0.4324.182 (Linux x86_64)]: Connected on socket HccaKZMZQn0nRFsMAAAB with id 51594150
    LOG: 'Testing pic-gallery-web.routers-test'
    .
    Chrome Headless 88.0.4324.182 (Linux x86_64): Executed 1 of 1 SUCCESS (0.005 secs / 0.001 secs)

Executed 1 of 1 SUCCESS とのことで、テストが pass していることがわかります。

なお、Linux 普段遣い (chrome ではなく chromium がブラウザの場合) のお兄さんは、 `CHROME_BIN=/usr/bin/chromium lein ci` として動かしてください。
