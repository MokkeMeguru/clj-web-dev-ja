- [画像をローカルに保存する](#org3931e49)
- [画像の REST のフロー確認](#orgb17ef8b)
  - [post](#org41e3185)
  - [get](#orgd11e09f)
  - [update](#org2a6a62f)
  - [delete](#org45255b1)
  - [user&rsquo;s image list](#org044e511)
    - [get](#org1274ae3)
- [ドメイン・ハンドラの作成](#orga8ca3a1)
  - [ドメイン](#org4a7b869)
  - [ハンドラ](#org762f6b1)
- [infrastructure の実装](#org5de0570)
- [interface の実装](#orgc9ddaf2)
  - [画像を保存するための interface](#orgf7f5174)
  - [Pic 情報を保存するための SQL interface](#org97ae223)
- [interface の組み込み](#org483db03)
- [動作確認](#orgdb40875)
- [付録](#orgabd7c30)
  - [Repository 内に Transaction を封じ込める](#orgdc4fcba)
    - [同一 サービス 内での Transaction](#org9fb6020)
    - [複数サービスをまたいだ Transaction](#orgcb0627d)

~~安心して下さい。長い戦いは終わったので、後は消化試合です~~

本稿では、画像を投稿し、閲覧することができる簡易サービスを想定し、以前までに作った Auth と組み合わせた API 開発を進めていきます。

また、本稿では実装に難儀するトランザクションの処理を Repository 内に封じ込める / TCC (try-confirm/catch) を用いる手法で解決し [8.1](#orgdc4fcba) 、実例として紹介します。

<a id="org3931e49"></a>

# 画像をローカルに保存する

本ガイドでは、画像をローカルに保存します。 一般には、GCS などの外部ストレージを利用することが多いのですが、連携に関する話をまとめるには紙面と時間が足りないので、 ローカルに保存、というシンプルな方法を利用します。

Clojure で画像ファイルを保存する方法は、 `javax.imageio.ImageIO/write` 関数を用いることです。 名前空間からして Java の機能を使っていますが、実際そのとおりです。 このように優秀な Java のドキュメント付きライブラリを使える点が Clojure の強みの一つです。

```clojure
;; docs https://docs.oracle.com/javase/jp/8/docs/api/javax/imageio/ImageIO.html
(import javax.imageio.ImageIO)

(def image (ImageIO/read (io/file "resources/sample.png")))

(ImageIO/write image "png" (io/file "resources/copy-sample.png"))
;; return true (success) / false (failure)
;; catch IllegalArgumentException ... any parameter is null
;;       IOException              ... write error
```

<a id="orgb17ef8b"></a>

# 画像の REST のフロー確認

base-URL は、 `/api/pics` とします。 なお、post / delete については header に認証情報が付与されているものとします。

<a id="org41e3185"></a>

## post

画像ファイルである images は、json で扱うことが難しいので、FormData を利用する。

- request

  ```json
  {
    "images": ["<image-file>"],
    "title": "<title>",
    "description(optional)": "<description>"
  }
  ```

- response

  ```json
  {
    "id": "<id>"
  }
  ```

<a id="orgd11e09f"></a>

## get

- request (query)

  ```json
  {
    "id": "<id>"
  }
  ```

- response

  ```json
  {
    "id": "<id>",
    "user-id": "<user-id>",
    "title": "<title>",
    "description(optional)": "<description>",
    "created_at": "<created_at as long number>",
    "image-urls": ["<image-url>"]
  }
  ```

<a id="org2a6a62f"></a>

## update

今回は実装しません。

<a id="org45255b1"></a>

## delete

- request

  ```json
  {
    "id": "<id>"
  }
  ```

- response なし、成功であれば `204`

<a id="org044e511"></a>

## user&rsquo;s image list

ユーザで絞り込んだ画像のリスト、と解釈し、 base-URL を `/api/users/<user-id>/pics` とします。

<a id="org1274ae3"></a>

### get

簡単のために、pagination を offset 法で実装することを前提に request query を組み立てています。

- request (query)

  ```json
  {
    "page-id": "<page-index>"
  }
  ```

- response `description` は長い文字列を想定しており、 list 表示では必要としていないので省略します。 `image-urls` についても、(現状では) 一枚の `image-url` のみを返すようにします。

  ```json
  [
    {
      "id": "<id>",
      "title": "<title>",
      "created_at": "<created_at as long number>",
      "image-urls": ["<image-url>"]
    }
  ]
  ```

<a id="orga8ca3a1"></a>

# ドメイン・ハンドラの作成

<a id="org4a7b869"></a>

## ドメイン

```clojure
(ns picture-gallery.domain.pics
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string]
            [picture-gallery.domain.users :as users-domain]
            [picture-gallery.domain.base :as base-domain]
            [picture-gallery.domain.auth :as auth-domain])
  (:import javax.imageio.ImageIO))

;; 不適切名のはじき出し
(def invalid-title-substrs
  ["fuck"])

(defn acceptable-title? [title]
  (apply
   = false
   (mapv (partial clojure.string/includes? title)
         invalid-title-substrs)))

;; 画像ファイルかどうかのチェック
(defn image-file? [image-file]
  (and (instance? java.io.File image-file)
       (some? (ImageIO/read image-file))))

(def min-title-length 0)
(def max-title-length 128)
(def min-description-length 0)
(def max-description-length 1024)
(def max-images-per-one-pic 3)

(s/def ::pic-id uuid?)
(s/def ::image-file image-file?)
(s/def ::title (s/and string?
                      #(< min-title-length (count %) max-title-length)
                      acceptable-title?))
(s/def ::description (s/and string?
                            #(< min-description-length (count %) max-description-length)))

(s/def ::image-url  string?)
(s/def ::image-files (s/coll-of ::image-file :min-count 1 :max-count max-images-per-one-pic))
(s/def ::image-urls (s/coll-of ::image-url :min-count 1 :max-count max-images-per-one-pic))

;; model
(s/def ::pic-images-create-model ::image-files)

(s/def ::pic-create-model
  (s/keys :req-un [::users-domain/user-id ::image-urls ::title]
          :opt-un [::description]))

(s/def ::pic-model
  (s/keys :req-un [::users-domain/user-id ::pic-id ::image-urls ::title ::base-domain/created-at]
          :opt-un [::description]))

(s/def ::pics-model
  (s/coll-of ::pic-model))

;; usecase 内の出入り部分の型
(s/def ::pic-post-input
  (s/keys :req-un [::auth-domain/encrypted-id-token ::image-files ::title]
          :opt-un [::description]))

(s/def ::pic-post-output
  (s/keys :req-un [::pic-id]))

(s/def ::pic-get-input
  (s/keys :req-un [::pic-id]))

(s/def ::pic-get-output ::pic-model)

(s/def ::pic-delete-input
  (s/keys :req-un [::auth-domain/encrypted-id-token ::pic-id]))

(s/def ::pic-delete-output true?)

(s/def ::pic-image-get-input
  (s/keys :req-un [::image-url]))

(s/def ::pic-image-get-output
  (s/keys :req-un [::image-file]))
```

<a id="org762f6b1"></a>

## ハンドラ

ベースのハンドラは `/api/pics` 以下に切り出します。

```clojure
(ns picture-gallery.infrastructure.router.pics
  (:require [picture-gallery.domain.openapi.pics :as pics-openapi]))

(defn pics-router [db auth image-db]
  ["/pics"
   {:swagger {:tags ["pics"]}}
   [""
    {:swagger {:tags ["pics"]}
     :post {:summary "post pic"
            :swagger {:security [{:Bearer []}]}
            :parameters {:multipart pics-openapi/pics-post-parameters-multipart}
            :responses {200 {:body ::pics-openapi/pics-post-response}}
            :handler (fn [input-data]
                       {:status 200
                        :body {:id "1"}})}}]
   ["/:pic-id"
    [""
     {:get {:summary "get a pic"
            :parameters {:path {:pic-id ::pics-openapi/id}}
            :responses {200 {:body ::pics-openapi/pic-get-response}}
            :handler (fn [input-data]
                       {:statsu 200
                        :body {}})}
      :delete {:summary "delete a pic"
               :parameters {:path {:pic-id ::pics-openapi/id}}
               :responses {204 {}}
               :handler (fn [input-data]
                          {:status 204
                           :body {}})}}]]])
```

ユーザの中にある pics という認識に立ち、 `users` namespace へ切り出しました。

```clojure
(ns picture-gallery.infrastructure.router.users
  (:require [picture-gallery.domain.openapi.pics :as pics-openapi]
            [picture-gallery.domain.openapi.users :as users-openapi]))

(defn users-router [db auth]
  ["/users"
   {:swagger {:tags ["users"]}}
   ["/:user-id"
    ["/pics"
     {:get {:summary "get pics per user"
            :parameters {:query {:page-id pos-int?}
                         :path {:user-id ::users-openapi/user-id}}
            :responses {200 {:body ::pics-openapi/user-pics-get-response}}
            :handler (fn [input-data]
                       {:status 200
                        :body {}})}}]]])
```

画像そのもののへの URL についても別の namespace に切り出しました。

```clojure
(ns picture-gallery.infrastructure.router.images
  (:require [picture-gallery.domain.openapi.pics :as pics-openapi]))

(defn images-router [db image-db]
  ["/img"
   {:swagger {:tags ["images"]}}
   ["/pics/:image-id"
    {:get {:summary "get a image of pic"
           :parameters {:path {:image-id ::pics-openapi/image-id}}
           :swagger {:produces ["image/png"]}
           :handler (fn [input-data]
                      {:status 200
                       :body {}})}}]])
```

ここまでで swagger は次の通りになります。

![img](./img/swagger-overview.png)

<a id="org5de0570"></a>

# infrastructure の実装

ローカルに画像を保存するために、image-db の infrastructure を作ります。 今回は、保存先の親ディレクトリ (`parent-dir`) を持つだけの infrastructure とします。

```clojure
(ns picture-gallery.infrastructure.image-db.core
  (:require [integrant.core :as ig]))

(defrecord LocalImageDBBoundary [image-db])

(defmethod ig/init-key ::image-db
  [_ {:keys [env]}]
  (let [parent-dir (:local-image-db-parent-dir env)]
    (->LocalImageDBBoundary {:parent-dir parent-dir})))
```

infrastructure を書いたので、config を編集します。

```clojure
{:picture-gallery.infrastructure.env/env {}
 :picture-gallery.infrastructure.logger/logger {:env #ig/ref :picture-gallery.infrastructure.env/env}
 :picture-gallery.infrastructure.firebase.core/firebase {:env #ig/ref :picture-gallery.infrastructure.env/env}
 :picture-gallery.infrastructure.sql.sql/sql {:env #ig/ref :picture-gallery.infrastructure.env/env
                                              :logger #ig/ref :picture-gallery.infrastructure.logger/logger}
 :picture-gallery.infrastructure.image-db.core/image-db {:env #ig/ref :picture-gallery.infrastructure.env/env}
 :picture-gallery.infrastructure.sql.migrate/migration  {:env #ig/ref :picture-gallery.infrastructure.env/env
                                                         :operation :migrate
                                                         :logger #ig/ref :picture-gallery.infrastructure.logger/logger}
 :picture-gallery.infrastructure.router.core/router {:env #ig/ref :picture-gallery.infrastructure.env/env
                                                     :auth #ig/ref :picture-gallery.infrastructure.firebase.core/firebase
                                                     :db #ig/ref :picture-gallery.infrastructure.sql.sql/sql}
 :picture-gallery.infrastructure.server/server {:env #ig/ref :picture-gallery.infrastructure.env/env
                                                :router #ig/ref :picture-gallery.infrastructure.router.core/router
                                                :port 3000}}
```

`env.clj` `profiles.clj` についても、以前と同様に編集します。

<a id="orgc9ddaf2"></a>

# interface の実装

interface も前回と同様に、 `defprotcol` を書いて、実装を書くだけです。

<a id="orgf7f5174"></a>

## 画像を保存するための interface

protocol は次の通り。保存、取得、削除のみの小さい interface です。

```clojure
(ns picture-gallery.interface.gateway.image-db.pics-service
  (:import (java.io File))
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.pics :as pics-domain]
            [integrant.core :as ig]
            [clojure.java.io :as io]))

(defprotocol Pics
  (get-pic-image [image-db blob])
  (save-pic-image [image-db ^File image])
  (delete-pic-image [image-db blob]))

(defn pics-service? [inst]
  (satisfies? Pics inst))

(s/def ::pics-service pics-service?)

(s/fdef get-pic-image
  :args (s/cat :image-db ::pics-service
               :blob ::pics-domain/image-url)
  :ret (s/or :exist ::pics-domain/image-file
             :not-exist empty?))

(s/fdef save-pic-image
  :args (s/cat :image-db ::pics-service
               :image ::pics-domain/image-file)
  :ret ::pics-domain/image-url)

(s/fdef delete-pic-image
  :args (s/cat :image-db ::pics-service
               :blob ::pics-domain/image-url)
  :ret (s/and int? (partial <= 0)))
```

<details><summary>impl</summary>

```clojure
(ns picture-gallery.interface.gateway.image-db.local.pics-service
  (:require [picture-gallery.interface.gateway.image-db.pics-service :refer [Pics]]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre])
  (:import (javax.imageio ImageIO)))

(extend-protocol Pics
  picture_gallery.infrastructure.image_db.core.LocalImageDBBoundary

  (get-pic-image [{{:keys [parent-dir]} :image-db} blob]
    (let [file (io/file parent-dir "pic" blob)]
      (if (.isFile file) file nil)))

  (save-pic-image [{{:keys [parent-dir]} :image-db} image]
    (try
      ;; check duplicate
      (loop [blob (java.util.UUID/randomUUID)
             retry 0]
        (let [file (io/file parent-dir "pic" (.toString blob))]
          (cond
            (> retry 10) (throw (ex-info "save pic's image failed: at apply unique random uuid"))
            (and file (.isFile file)) (recur (java.util.UUID/randomUUID) (inc retry))
            :else (do (ImageIO/write (ImageIO/read image) "png" file)
                      (.toString blob)))))
      (catch java.io.IOException e
        (timbre/error "Pics save image Error: " (.getMessage e))
        (throw (ex-info "failed to save image" {:parent-dir parent-dir :image image})))))

  (delete-pic-image [{{:keys [parent-dir]} :image-db} blob]
    (try
      (io/delete-file (io/file parent-dir "pic" blob)) 1
      (catch Exception e
        (timbre/warn "Pics delete image Error: " (.getMessage e)) 0))))
```

</details>

<a id="org97ae223"></a>

## Pic 情報を保存するための SQL interface

前回ユーザ用に作った interface と同様に作ります。

今回は、1 つの投稿について、複数枚の画像が投稿できることを（長期的に）想定しているので、 テーブルを分離し( one-many )ます。

そして、usecase に transaction を持ち込まないため、この複数テーブルの操作を一つの repository に押し込んでしまいます。

```clojure
(ns picture-gallery.interface.gateway.database.pics-repository
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.users :as users-domain]
            [picture-gallery.domain.base :as base-domain]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [orchestra.spec.test :as st]
            [next.jdbc :as jdbc]))

(defprotocol Pics
  (get-pics [db])
  (get-pics-by-user [db user-id page-id])
  (get-pic [db pic-id])
  (create-pic [db pic-create-model state])
  (update-pic-state [db pic-id state])
  (delete-pic [db pic-id logical?]))

(defn pics-repository? [inst]
  (satisfies? Pics inst))

(s/def ::pics-repository pics-repository?)

(s/fdef get-pics
  :args (s/cat :db ::pics-repository)
  :ret ::pics-domain/pics-model)

(s/fdef get-pics-by-user
  :args (s/cat :db ::pics-repository
               :user-id ::users-domain/user-id
               :page-id pos-int?)
  :ret ::pics-domain/pics-model)

(s/fdef get-pic
  :args (s/cat :db ::pics-repository
               :pic-id ::pics-domain/pic-id)
  :ret (s/or :exist ::pics-domain/pic-model
             :not-exist empty?))

(s/fdef create-pic
  :args (s/cat :db ::pics-repository
               :pic-create-model ::pics-domain/pic-create-model
               :state ::base-domain/tcc-state)
  :ret (s/tuple ::pics-domain/pic-model ::base-domain/tcc-state))

(s/fdef update-pic-state
  :args (s/cat :db ::pics-repository
               :pic-id ::pics-domain/pic-id
               :state ::base-domain/tcc-state)
  :ret (s/and int? (partial <= 0)))

(s/fdef delete-pic
  :args (s/cat :db ::pics-repository
               :pic-id ::pics-domain/pic-id
               :logical? boolean?)
  :ret (s/and int? (partial <= 0)))
```

<details><summary>impl(複数テーブルの操作のため、かなり長いです)</summary>

```clojure
(ns picture-gallery.interface.gateway.database.sql.pics-repository
  (:require [picture-gallery.interface.gateway.database.pics-repository :refer [Pics]]
            [picture-gallery.interface.gateway.database.sql.utils :as sql-utils]
            [next.jdbc :as jdbc]
            [clojure.string]
            [next.jdbc.sql :as njs]
            [next.jdbc.types :refer [as-other]]
            [clojure.spec.alpha :as s]))

(defn pic-create-model->sql [{:keys [user-id title description]} state]
  (cond->
   {:user_id user-id
    :title title
    :tcc_state (as-other (name state))}
    description (assoc :description description)))

(defn pic-image-urls->sql [image-urls]
  (vec (map-indexed  (fn [idx image-url]
                       {:blob image-url
                        :index idx}) image-urls)))

(defn sql->pic-model [sql-pic sql-pic-image]
  (let [{:keys [id user_id title description created_at updated_at is_deleted tcc_state]} sql-pic
        image-urls (mapv #(:blob %) (sort-by :index sql-pic-image))]
    (if-not id
      nil
      (cond->
       {:pic-id id
        :user-id user_id
        :title title
        :image-urls image-urls
        :created-at (sql-utils/sql-to-long created_at)
        :is-deleted is_deleted
        :tcc-state tcc_state}
        description (assoc :description description)
        updated_at (assoc :updated-at (sql-utils/sql-to-long updated_at))))))

(def sql-basic-selection
  "SELECT * FROM pics INNER JOIN pic_images ON (pics.id = pic_images.id)")

(extend-protocol Pics
  picture_gallery.infrastructure.sql.sql.Boundary

  (get-pics [{:keys [spec]}]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (let [pics (jdbc/execute! conn [(clojure.string/join " " [sql-basic-selection "limit 100"])] sql-utils/default-jdbc-option)
            pics-images (mapv #(jdbc/execute! conn ["SELECT * FROM pic_images WHERE id = ?" (:id %)] sql-utils/default-jdbc-option) pics)]
        (mapv sql->pic-model pics pics-images))))

  (get-pics-by-user [{:keys [spec]} user-id page-id]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (let [sql-offset (* 20 (dec page-id))
            pics (jdbc/execute! conn ["SELECT * FROM pics WHERE user_id = ? AND is_deleted = false AND tcc_state = ? limit 20 offset ?" user-id (as-other "confirm") sql-offset] sql-utils/default-jdbc-option)
            pics-head-images (mapv #(jdbc/execute! conn ["SELECT * FROM pic_images WHERE id = ? AND index = 0" (:id %)] sql-utils/default-jdbc-option) pics)]
        (mapv sql->pic-model pics pics-head-images))))

  (get-pic [{:keys [spec]} pic-id]
    (with-open [conn (jdbc/get-connection (:datasource spec))]
      (sql->pic-model
       (jdbc/execute-one! conn ["SELECT * FROM pics WHERE id = ? AND is_deleted = false AND tcc_state = ?" pic-id (as-other "confirm")] sql-utils/default-jdbc-option)
       (jdbc/execute! conn ["SELECT * FROM pic_images WHERE id = ?" pic-id] sql-utils/default-jdbc-option))))

  (create-pic [{:keys [spec]} pic-create-model state]
    (let [sql-pic-create-model (pic-create-model->sql pic-create-model state)
          sql-pic-image-urls (pic-image-urls->sql (:image-urls pic-create-model))]
      (jdbc/with-transaction [tx (:datasource spec)]
        (let [pic-id (loop [pic-id (java.util.UUID/randomUUID) retry 0]
                       (cond
                         (> retry 10) (throw (ex-info "pic's unique random uuid generation failed" {:pic-create-model pic-create-model}))
                         (nil? (jdbc/execute-one! tx ["SELECT * FROM pics WHERE id = ?" pic-id])) pic-id
                         :else (recur (java.util.UUID/randomUUID) (inc retry))))
              pic-result (njs/insert! tx :pics (assoc sql-pic-create-model :id pic-id) sql-utils/default-jdbc-option)
              pic-image-result (njs/insert-multi! tx :pic_images [:blob :id :index] (mapv (fn [{:keys [blob index]}] [blob pic-id index]) sql-pic-image-urls) sql-utils/default-jdbc-option)]
          [(sql->pic-model pic-result pic-image-result) (keyword (:tcc_state pic-result))]))))

  (update-pic-state [{:keys [spec]} pic-id state]
    (sql-utils/update! spec :pics {:tcc_state (as-other (name state))} {:id pic-id}))

  (delete-pic [{:keys [spec]} pic-id logical?]
    (if logical?
      (sql-utils/logical-delete! spec :pics {:id pic-id})
      (sql-utils/physical-delete! spec :pics {:id pic-id}))))
```

</details>

<a id="org483db03"></a>

# interface の組み込み

usecase 層に interface を組み込んでいきます。 一番問題となるのは、投稿処理の部分で、TCC パターンを組み合わせて実装することになります。

そこで、まずは実装フローを図にしてみます。

![img](./img/pic_tcc_flow.png)

Clojure において、 (エラー処理を省いて) 処理が一本筋であると、かなり綺麗な (ref: 前回の signin の usecase ) 実装ができます。 しかし、今回は枝分かれのある処理を行った後、集約する必要が見えています。

そのため、本ガイドでは、この TCC 処理の部分を取り出すことで、一本筋に見えるよう関数の実装を工夫しています。

結果として実装がかなり長くなったため、内部関数の TCC 処理部分について、 spec を書いて、仕様を明らかにしています。 このように複雑な関数を実装する必要性に迫られた際には、REPL で動作を確認しながら、 spec を用いて、 **どのような関数を実装するのか削り出せる点** が、Clojure の強みの一つです (と思っています)。

<details><summary> 実装 (300 line +) </summary>

```clojure
(ns picture-gallery.usecase.pic-post
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.utils.error :refer [err->> border-error]]
            [picture-gallery.interface.gateway.database.pics-repository :as pics-repository]
            [picture-gallery.interface.gateway.image-db.pics-service :as pics-service]
            [orchestra.spec.test :as st]
            [clojure.java.io :as io]
            [picture-gallery.domain.error :as error-domain]
            [taoensso.timbre :as timbre]
            [picture-gallery.interface.gateway.auth.auth-service :as auth-service]
            [picture-gallery.interface.gateway.database.users-repository :as users-repository]
            [integrant.core :as ig]
            [picture-gallery.domain.users :as users-domain]
            [picture-gallery.domain.base :as base-domain]))

(s/fdef pic-post
  :args (s/cat :db (s/and ::users-repository/users-repository
                          ::pics-repository/pics-repository)
               :auth ::auth-service/auth-service
               :image-db ::pics-service/pics-service
               :input-model ::pics-domain/pic-post-input)

  :ret (s/or :success (s/tuple ::pics-domain/pic-post-output nil?)
             :failure (s/tuple nil? ::error-domain/error)))

;; この部分は signin と同じ関数を使いまわしています。
;; 小さな機能で分割することでコードを再利用できるようになり、メンテナンスコストの低下が望めるかもしれません。：
(defn decode-id-token "
  decode encrypted id-token
  "
  [{:keys [input-model auth] :as m}]
  (let [[[status body] err] (border-error {:function #(auth-service/decode-id-token auth (:encrypted-id-token input-model))
                                           :error-wrapper error-domain/auth-error})]
    (cond
      err [nil err]
      (= :failure status) [nil body]
      :else [(assoc m :id-token (:id-token body)) nil])))

(defn get-exist-user-has-id-token "
  get active (not logical deleted) user
  which has id-token"
  [{:keys [id-token db] :as m}]
  (let [[active-user err] (border-error {:function #(users-repository/get-exist-user-by-auth-token db id-token)
                                         :error-wrapper error-domain/database-error})]
    (cond
      err [nil err]
      (empty? active-user) [nil error-domain/signin-failed-by-user-not-found]
      :else [(assoc m :exist-user active-user) nil])))


;; --- tcc-process -------
;; 以下が spec の定義です。REPL 経由で 実装と往復し、仕様を決めていきます。
;; 極力関数の概形を揃えることで、可読性を向上させると良いでしょう (そのためにも沢山仮実装してみて下さい)。
;; spec helper
(s/def ::input-model ::pics-domain/pic-post-input)
(s/def ::exist-user ::users-domain/user-model)
(s/def ::db (s/and ::pics-repository/pics-repository
                   ::users-repository/users-repository))
(s/def ::image-db ::pics-service/pics-service)

(s/def ::tcc-image-process (s/or :success ::pics-domain/image-urls :failure nil?))
(s/def ::tcc-db-process (s/or :success ::pics-domain/pic-model :failure nil?))
(s/def ::tcc-error (s/or ::no-error nil? ::error ::error-domain/error))
(s/def ::tcc-result ::base-domain/tcc-state)
(s/def ::tcc-status (s/keys :req-un [::tcc-image-process ::tcc-db-process ::tcc-result ::tcc-error]))

;; try
(s/fdef pic-post-try-phase-save-images
  :args (s/cat :image-files ::pics-domain/image-files
               :image-db ::image-db)
  :ret (s/or :success (s/tuple ::pics-domain/image-urls nil?)
             :failure (s/tuple nil? ::error-domain/error)))

(s/fdef pic-post-try-phase-save-pic
  :args (s/cat :m (s/keys :req-un [::input-model ::exist-user ::pics-domain/image-urls]) :db ::db)
  :ret (s/or :success (s/tuple ::pics-domain/pic-model nil?)
             :failure (s/tuple nil? ::error-domain/error)))

(s/fdef pic-post-try-phase
  :args (s/cat :m (s/keys :req-un [::input-model ::exist-user ::db ::image-db]))
  :ret (s/tuple boolean? ::tcc-status))

;; confirm
(s/fdef pic-post-confirm-phase-save-images
  :args (s/cat :tcc-image-process ::tcc-image-process :image-db ::image-db)
  :ret (s/or :success (s/tuple ::tcc-image-process nil?)
             :failure (s/tuple nil? ::error-domain/error)))

(s/fdef pic-post-confirm-phase-save-pic
  :args (s/cat :tcc-db-process ::tcc-db-process :db ::db)
  :ret (s/or :success (s/tuple ::tcc-db-process nil?)
             :failure (s/tuple nil? ::error-domain/error)))

(s/fdef pic-post-confirm-phase
  :args (s/cat :m (s/keys :req-un [::tcc-status ::db ::image-db]))
  :ret (s/or :success (s/tuple ::tcc-status nil?)
             :failure (s/tuple nil? ::error-domain/error)))

;; cancel
(s/fdef pic-post-cancel-phase-remove-images
  :args (s/cat :tcc-image-process ::tcc-image-process :image-db ::image-db)
  :ret (s/or :success (s/tuple ::tcc-image-process nil?)
             :failure (s/tuple nil? ::error-domain/error)))

(s/fdef pic-post-cancel-phase-remove-pic-model
  :args (s/cat :tcc-db-process ::tcc-db-process :db ::db)
  :ret (s/or :success (s/tuple ::tcc-db-process nil?)
             :failure (s/tuple nil? ::error-domain/error)))

(s/fdef pic-post-cancel-phase
  :args (s/cat :m (s/keys :req-un [::tcc-status ::db ::image-db]))
  :ret (s/tuple nil? ::error-domain/error))

;; root
(s/fdef pic-post-tcc
  :args (s/cat :m (s/keys :req-un [::input-model ::exist-user ::db ::image-db]))
  :ret (s/or :success (s/tuple (s/keys :req-un [::tcc-status]) nil?)
             :failure (s/tuple nil? ::error-domain/error)))

;; --------------------------
;; 以下が実装です。
;; try-phase
(defn pic-post-try-phase-save-images "
 tcc's try-process
  ! 1. save images ^ generate each image's url
  2. save pic model as tried-model into db
 "
  [image-files image-db]
  (loop [acc-image-files image-files
         image-urls []]
    (if (-> acc-image-files count zero?)
      [image-urls nil]
      (let [[image-url err]
            (border-error {:function #(pics-service/save-pic-image image-db (first acc-image-files))
                           :error-wrapper error-domain/image-db-error})]
        (cond
          err [image-urls err]
          :else (recur (rest acc-image-files) (conj image-urls image-url)))))))

(defn pic-post-try-phase-save-pic "
  tcc's try-process
  1. save images ^ generate each image's url
  ! 2. save pic model as tried-model into db
  "
  [{:keys [input-model exist-user image-urls]} db]
  (let [pic-create-model {:user-id (:user-id exist-user)
                          :image-urls image-urls
                          :title (:title input-model)
                          :description (:description input-model)}
        [[new-pic-tried _] err] (border-error {:function #(pics-repository/create-pic db pic-create-model :try)
                                               :error-wrapper error-domain/database-error})]
    (cond
      err [nil err]
      :else [new-pic-tried nil])))

(defn pic-post-try-phase "
  tcc's try-process
  1. save images ^ generate each image's url
  2. save pic model as tried-model into db
  "
  [{:keys [input-model exist-user db image-db]}]
  (let [[image-urls err] (pic-post-try-phase-save-images (:image-files input-model) image-db)
        [new-pic err] (if err
                        [nil err]
                        (pic-post-try-phase-save-pic {:input-model input-model
                                                      :exist-user exist-user
                                                      :image-urls image-urls}
                                                     db))]
    (if err
      [false {:tcc-image-process image-urls
              :tcc-db-process new-pic
              :tcc-result :try
              :tcc-error err}]
      [true {:tcc-image-process image-urls
             :tcc-db-process new-pic
             :tcc-result :try
             :tcc-error nil}])))

;; confirm-process
(defn pic-post-confirm-phase-save-images "
  tcc's confirm-process
  ! 1. confirm saved images
  2. save pic model as confirmed-model into db
  "
  [tcc-image-process image-db]
  [tcc-image-process nil])

(defn pic-post-confirm-phase-save-pic "
  tcc's confirm-process
  1. confirm saved images
  ! 2. save pic model as confirmed-model into db
  "
  [tcc-db-process db]
  (let [[_ err] (border-error {:function #(pics-repository/update-pic-state db (:pic-id tcc-db-process) :confirm)
                               :error-wrapper error-domain/database-error})]
    (when err
      (timbre/error "pic-post tcc confirm phase failed at save-pic" tcc-db-process))
    (cond
      err [nil err]
      :else [tcc-db-process nil])))

(defn pic-post-confirm-phase "
  tcc's confirm-process
  1. confirm saved images
  2. save pic model as confirmed-model into db
  "
  [{:keys [tcc-status db image-db] :as m}]
  (let [{:keys [tcc-db-process tcc-image-process]} tcc-status
        [tcc-image-process err] (pic-post-confirm-phase-save-images tcc-image-process image-db)
        [tcc-db-process err] (if err [nil err] (pic-post-confirm-phase-save-pic tcc-db-process db))]
    (cond
      err [nil err]
      :else [{:tcc-image-process tcc-image-process
              :tcc-db-process tcc-db-process
              :tcc-result :confirm
              :tcc-error nil} nil])))

;; cancel-process
(defn pic-post-cancel-phase-remove-images "
  tcc's cancel-process
  ! 1. remove images
  2. set pic model's tcc-state :cancel
  "
  [tcc-image-process image-db]
  (let [delete-image-results
        (map (fn [image-url]
               (try (pics-service/delete-pic-image image-db image-url)
                    (catch Exception e
                      (timbre/error "pic-post tcc cancel phase failed at remove-image" image-url "cause: " (.getMessage e))
                      -1))) tcc-image-process)]
    (if (every? (partial <= 0) delete-image-results)
      [tcc-image-process nil]
      [nil error-domain/image-delete-failed])))

(defn pic-post-cancel-phase-remove-pic-model "
  tcc's cancel-process
  1. remove images
  ! 2. set pic model's tcc-state :cancel
  "
  [tcc-db-process db]
  (let [[_ err]
        (border-error {:function #(pics-repository/update-pic-state db (:pic-id tcc-db-process) :cancel)
                       :error-wrapper error-domain/database-error})]
    (cond
      err [nil err]
      :else [tcc-db-process  nil])))

(defn pic-post-cancel-phase "
  tcc's cancel-process
  1. remove images
  2. set pic model's tcc-state :cancel
  "
  [{:keys [tcc-status db image-db]}]
  (let [{:keys [db-process image-process]} tcc-status
        [image-process image-err] (pic-post-cancel-phase-remove-images image-process image-db)
        [db-process db-err] (pic-post-cancel-phase-remove-pic-model db-process db)]
    (when image-err
      (timbre/error "pic-post tcc cancel phase failed at remove-images" image-process))
    (when db-err
      (timbre/error "pic-post tcc cancel phase failed at remove-pic-model" db-process))
    (cond
      image-err [nil image-err]
      db-err [nil db-err]
      :else [nil (-> tcc-status :tcc-error)])))

(defn pic-post-tcc "
  tcc-process
  1. try-phase
     returns [try-success? tcc-status]
  2-a. confirm-phase if try-success?
  2-b. cancel-phase if-not try-success?
  "
  [{:keys [input-model exist-user db image-db]}]
  (let [m {:input-model input-model :exist-user exist-user :db db :image-db image-db}
        [try-success? tcc-status]
        (pic-post-try-phase m)]
    (when (:error tcc-status)
      (timbre/warn "pic-post tcc process error: " (:error tcc-status)
                   "/db-process: "  (:tcc-db-process tcc-status)
                   "/image-process: " (:tcc-image-process tcc-status)))
    (let [[tcc-result err] (if try-success?
                             (pic-post-confirm-phase (assoc m :tcc-status tcc-status))
                             (pic-post-cancel-phase (assoc m :tcc-status tcc-status)))]
      (cond
        err [nil err]
        :else [(assoc m :tcc-status tcc-result) nil]))))

;; ----------------
(defn ->output-model [{:keys [tcc-status]}]
  [{:pic-id (-> tcc-status :tcc-db-process :pic-id)} nil])

;; 大元の関数は、 sigin / signup と同様に err->> マクロでくくるという規格を設けています。
(defn pic-post [db auth image-db input-model]
  (err->>
   {:input-model input-model
    :auth auth
    :db db
    :image-db image-db}
   decode-id-token               ;; encrypted-id-token を decode します。
   get-exist-user-has-id-token   ;; ユーザ情報を獲得します。
   pic-post-tcc                  ;; tcc パターンでデータを db、image-db へ保存します。
   ->output-model))              ;; output のモデルにフォーマットします。
```

</details>

その他の実装は、signin / signup と同様に作られるので、省略します。

<a id="orgdb40875"></a>

# 動作確認

Swagger を用いて動作確認をします。

- post

  ![img](./img/post_pic.png)

- get list

  ![img](./img/get_list_pics.png)

- get image

  ![img](./img/get_image.png)

<a id="orgabd7c30"></a>

# 付録

<a id="orgdc4fcba"></a>

## Repository 内に Transaction を封じ込める

Transaction は副作用を伴う関数を ACID に扱いたいときに用います。 ACID の A は原子性 (atomicity) であり、原子をやり取りするのに一つの Repository を使おう、という立場であれば、この主張は正しいと言えます。

<a id="org9fb6020"></a>

### 同一 サービス 内での Transaction

例えば、銀行の預金テーブル Y があったとき、Ron から Potter に 20 ポンド 送金があったとき、 Ron の預金を減らす処理と Potter の預金を増やす処理は ACID でなければなりません。

このとき、次の 2 つのパターンが考えられます。

1.  usecase 側に transaction を漏らして、SQL を実行
2.  repository 内で Ron と Potter の預金を操作する

参考: <https://github.com/duct-framework/duct/wiki/Boundaries>

しかし、実際の Transaction の利用場面では、 (**同一 DB 上の**) いくつかの異なるテーブルにまたがって ACID な操作を行いたいケースもあります。 例えばソーシャルゲームのポイントガチャがそれに当たります。 (ポイントテーブル Z のポイント z を減らして、アイテムテーブル I にアイテム i を追加する)

このときには、次の 2 つのパターンが考えられます (他にもありますが簡単のため 2 つとします)。

1.  usecase 側に 該当 DB の transaction を漏らして、 transaction 内で 複数の repository を操作する。
2.  複数テーブルで表されるモデルを 1 つのモデルとみなして、 1 つの repository とする。

2 については、モデルをどう効率よく設計できているかに依存した方針です。また、 ORM を意識したモデル設計とは異なります。 しかしモデルを原子とみなす考え方をすると、 2 の実装を考えることもできます。

ただし弱点として、新しい機能開発をする際に、モデル (= transaction の単位) を見直す必要があります。 言い換えると、 1 を用いることで、モデルが Atomic でなくとも usecase 内部で Atomic な単位を生成することができるので、お手軽に拡張することができます。

<a id="orgcb0627d"></a>

### 複数サービスをまたいだ Transaction

同一 DB だけで完結する transaction は上記解決策を考えればよいのですが、例えば決済サービスと連動して自社 DB を操作します、となると話が変わります。 2 つのサービスを連携させた transaction は通常生成することが困難です。 そのため、TCC (try-confirm/cancel) というアプローチが用いられるケースが有名所さんとなっています。

TCC とは、try-phase, confirm/cancel-phase の 2 つのフェーズを用いた transaction 方式です。

try-phase とは、各サービスに対してリソースの仮押さえを行う phase です。ここで、抑えたリソースは **必ず** 確定 (confirm) / 棄却 (cancel) ができることが保証されています。 confirm/cancel-phase とは、 すべてのサービスで try が成功すればリソース消費の確定 (confirm)を、一つでも失敗すれば棄却 (cancel) する phase です。

![img](./img/tcc.png)

TCC の場合は、 transaction を DB やサービスの repository から usecase へ持ち込む必要がないため、本手続きは usecase 内に書くことも容易です (実際に本ガイドでは、 **同一サービス内の操作は同一 repository** に、 **複数サービスにまたがる操作は TCC を用いて usecase 内** に落としています)。

勿論 TCC ではなく、ログを吐き出して記録することでサーバ外で容易に error-catch / retry できるようにする手法などもあるので、たくさん実験してみて下さい (少なくとも本ガイドは全くフレームワークやライブラリの制約が削ぎ落とされているので、実装は可能なはずです)。

参考:

- <https://qiita.com/nk2/items/d9e9a220190549107282#tcc%E3%83%91%E3%82%BF%E3%83%BC%E3%83%B3>
- <https://engineering.mercari.com/blog/entry/2019-06-07-155849/>
