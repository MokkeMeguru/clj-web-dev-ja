# Table of Contents

1.  [Clojure の特徴と利点](#org963e58b)
    1.  [Clojure](#org5ef97c4)
    2.  [ClojureScript](#org37ab6c9)
    3.  [関数型言語と REPL 開発](#org62c43a8)
2.  [Clojure Spec ~ データと型の仕様、契約プログラミング ~](#org0d34c49)
    1.  [Spec を元にした生成テスト](#orgbf51934)
3.  [Clojure と テスト、 あと Mock](#org648d8b0)
    1.  [Mock の話](#org50c296a)

本稿は、Clojure x ClojureScript 深める Web 開発 を読むにあたり、なんとなく知っておいてほしい事柄について紹介するものです。

<a id="org963e58b"></a>

# Clojure の特徴と利点

<a id="org5ef97c4"></a>

## Clojure

Clojure は Lisp と呼ばれるプログラミング言語の影響を強く受けた JVM 言語の一つです。JVM 言語の仲間としては、 Scala や Groovy なんかがあります。日本では Scala の方が有名かもしれません。

もっぱら Web 開発やその周辺に用いられていますが、Java でできることは大体できます (e.g. ゲーム開発、深層学習、スタンドアロンアプリ開発)。

構文の基本は、 `(関数 引数1 引数2 ...)` というもので、例えば 足し算 `1 + 2` であれば、 `(+ 1 2)` となります。
また関数の定義は、

    (defn 関数名 [引数1 引数2]
      ;; 実行する内容
      )

のようになります。基本的には括弧で括った処理単位の組み合わせ、といった感じになっていますが、詳しい構文は書いたり読んだりしながら慣れる方が早いです。

Clojure について、基礎的な部分を調べるに最も便利なサイトとしては、<https://clojuredocs.org/> を挙げることができます。
また不明点などは雑に Twitter で僕あたりに Reply / DM を投げるか、#Clojure で Tweet すると捕捉されます。

<a id="org37ab6c9"></a>

## ClojureScript

ClojureScript は Clojure の構文を用いることのできる JavaScript 方言です。 JavaScript 方言としては、TypeScript が有名ですね。

Clojure / ClojureScript の利点は、サーバ、クライアントで同じ構文を用いて開発ができるという点、また、一部のコードを共通化 (同じコードをサーバ、クライアント両方で使える) という点です。

    ;; clojure
    (defn add [x y] (+ x y))

    ;; clojurescript
    (defn add [x y] (+ x y))

<a id="org62c43a8"></a>

## 関数型言語と REPL 開発

関数型言語を簡単に説明すると、データを入れてデータを返す &ldquo;関数&rdquo; を基礎としたプログラミング言語です。
Clojure (Lisp) はその中でも緩めの関数型で、オブジェクト指向とうまく組み合わせたちょうどよい感じのプログラムの記述ができます。

例えば、足し算をする関数 Add を Java で書くと、次のようになります。

    public class AddClass {
      public static int add(int x, int y) { return x + y; }
      public static void main(String[] args) {
        int x = 1;
        int y = 2;

        int z = add(x, y);
        System.out.printf("result is: %d\n", z);
      }
    }

実行方法は、

1.  javac AddClass.java (compile)
2.  java AddClass (run) => result is 3

という形になります。add 関数は AddClass クラスのクラスメソッドとして実装されていますね。

Clojure で書くとこんな感じ。

    (defn add [x y] (+ x y))

    (format "result is: %d" (add 1 2))
    ;; => result is: 3

厳密ではないですが、 Python のように add 関数を記述して、実行する形になります。

比較までに Python で同じことを書くと、

    def add (x, y):
        return x + y

    print("result is: {}".format(add(1, 2)))
    # => result is: 3

Clojure では、コードを書いて評価しその結果を逐次確認する Read-Eval-Print Loop (REPL) という環境で開発を行うことが一般的であるのと同時に、製品時にはコードをコンパイルして実行できるという利点があります。
また、コードを書きながらデータを入力して期待する動作かどうかをチェックできるため、開発が比較的に容易であると言えます。

対して、昨今の テストコードを先に全部記述して、機能開発を行い、コンパイル/テスト実行をする <del>ウォーターフォールのような</del> 開発手法 とは相性があんまり良くないかもしれないです。

<a id="org0d34c49"></a>

# Clojure Spec ~ データと型の仕様、契約プログラミング ~

Clojure では、Java のように型 (Class) を宣言するすることができますが、Spec や malli といった、データについての型を定義する仕組みを用いることが好まれています。

Spec は、 **データや関数の仕様書** と言いかえることができます。

簡単のために、車を例に Spec を考えてみます。

車は次のような値を持っているとします。

- クラクションの音 (e.g. &ldquo;Beep&rdquo;)
- 重さ (e.g. 120)
- 速さ (e.g. 50)

また車は次のようなことができるとします。

- クラクションの音を鳴らす
- n 時間走る

これらは **仕様** であるといえ、 Clojure ではこれを Spec を用いて次のように記述することができます。

    (require '[clojure.spec.alpha :as s])

    ;; クラクションの音は string
    (s/def ::beep string?)

    ;; 重さは 正の integer
    (s/def ::weight pos-int?)

    ;; 速さは 正の integer
    (s/def ::speed pos-int?)

    ;; 車は、クラクションの音、重さ、速さを値として持つ
    (s/def ::car (s/keys :req-un [::beep ::weight ::speed]))

    ;; 車の音を鳴らす関数は、
    ;; car の spec を満たす値を引数にとって、
    ;; string を返す
    (s/fdef car-beep
      :args (s/cat :car ::car)
      :ret string?)

    ;; 車を走らせる関数は、
    ;; car の spec を満たす値と, integer 型の n を引数にとって、
    ;; 走った距離 (n x speed) を返す
    (s/fdef car-run
      :args (s/cat :car ::car :n (fn [n] (> n 0)))
      :ret int?
      :fn (fn [{:keys [args ret]}]
            (= ret (* (-> args :n) (-> args :car :speed)))))

Clojure ではこのようにデータ型を宣言することによって、値のバリデーションを行ったり、関数の実装の説明を行ったりします。
ここで、実際にこれらの仕様を満たすデータを宣言/関数を実装してみましょう。

    (def legal-car-example {:beep "beep!!!" :weight 120 :speed 50})

    ;; これは 仕様を満たしていない
    (def illegal-car-example {:beep 123 :weight 120 :speed 50})

    (defn car-beep [car]
      {:pre [(s/valid? ::car car)]
       :post [(s/valid? string? %)]}
      (format "the car says: %s" (:beep car)))

    (defn car-run [car n]
      {:pre [(s/valid? ::car car) (> n 0)]
       :post [(s/valid? int? %)]}
      (* n (:speed car)))

なお、pre / post とかあるのは、契約プログラミングにおける 事前条件 / 事後条件を示しています。これは関数を実行する際に、それぞれの条件を満たしているかを毎回チェックする、というものです。
特に安全にコードを実行したい際に利用することができます。

次に、実際に評価して / データを流して結果を見てみましょう。

    ;; テスト時に spec を利用する際の設定
    (require '[orchestra.spec.test :as st])
    (st/instrument)

    ;; OK な例
    (s/valid? ::car legal-car-example)
    (car-beep legal-car-example)
    (car-run legal-car-example 2)

    ;; ダメ な例
    (s/valid? ::car illegal-car-example)
    (car-beep illegal-car-example)
    (car-run legal-car-example -1)

    ;; 結果

    ;; OK な例
    ;; => true
    ;; => "the car says: beep!!!"
    ;; => 100

    ;; ダメな例
    ;; => false
    ;; => class clojure.lang.ExceptionInfo ...
    ;; => class clojure.lang.ExceptionInfo ...

とまあこんな形で仕様を満たすかどうかをチェックすることができます。

具体的に開発する際には、REPL で逐次様々なデータを流しながら仕様を満たすコードを書いたり、playground のコードから仕様の記述/見直しを行ったり、更にはテストコードを書いたり修正したりすることができます。

実際に <del>雑な</del> 開発を行っている際には、仕様の変更や仕様ミスがあることは当然のごとくありますし、手探りに開発をするケースもあると思います。その際には Clojure の REPL , Spec を用いた開発はかなり便利だという印象があります。

参考: <https://clojure.org/guides/spec>

<a id="orgbf51934"></a>

## Spec を元にした生成テスト

Clojure では Spec を用いて データや関数の仕様を記述することを紹介しました。
Spec は **自動的にデータを生成できるほどに** データ仕様を詳細に記述できることから、プロパティベーステストというテストを行うことができます。

下の例では、仕様からランダムなデータを生成してそれを元に 1000 回テストを行いました。

    (require '[clojure.spec.test.alpha :as stest])
    (stest/check `car-beep)

    ;; 結果
    ;; ({:spec #object[clojure.spec.alpha$fspec_impl$reify__24510x3628c964
    ;;                "clojure.spec.alpha$fspec_impl$reify__2451@3628c964"],
    ;;   :clojure.spec.test.check/ret
    ;;   {:result true,
    ;;    :pass? true,
    ;;    :num-tests 1000,
    ;;    :time-elapsed-ms 150, :seed 1615145728430},
    ;;   :sym user/car-beep})

場合によっては使うかもしれないので知っておくと得かもしれません。

<a id="org648d8b0"></a>

# Clojure と テスト、 あと Mock

先程まで Spec を用いてデータや関数の仕様を書く方法を紹介してきましたが、やはりテストは書いておくに越したことはないです。

Clojure を用いてテストを書く最もシンプルな方法は、 `deftest` を利用するものです。

試しに簡単な API ハンドラを書いてみましょう。

    (s/def ::first-name string?)
    (s/def ::last-name string?)
    (s/def ::full-name string?)

    (s/def ::params (s/keys :req-un [::first-name ::last-name]))
    (s/def ::status #{:success :failure})
    (s/def ::result (s/keys :req-un [::status] :opt-un [::full-name]))

    (s/fdef handler
      :args (s/cat :params ::params)
      :ret ::result)

    (defn handler [params]
      (let [{:keys [first-name last-name]} params]
        (if (= last-name "Meguru")
          {:status :success :full-name (format "%s %s" first-name last-name)}
          {:status :failure})))

    (handler {:first-name "Mokke" :last-name "Meguru"})
    ;; => {:status :success :full-name "Mokke Meguru"}
    (handler {:first-name "Sample" :last-name "User"})
    ;; => {:status :failure}

Spec を参考にテストを書くとすると、こんな感じになります。 (first-name, last-name が string であるのは **仕様として** 明らかです。)

    (require '[clojure.test :refer [deftest is testing run-tests]])

    (st/instrument)

    (deftest handler-test
      (testing "last name is Meguru"
        (let [params  {:first-name "Mokke" :last-name "Meguru"}
              result (handler params)]
          (is (= :success (:status result))) ;; status は success ?
          (is (= "Mokke Meguru" (:full-name result))))) ;; full-name は Mokke Meguru ?
      (testing "last name is not Meguru"
        (let [params {:first-name "Sample" :last-name "User"}
              result (handler params)]
          (is (= :failure (:status result)))))) ;; status は failure ?

    ;; テストの実行
    (run-tests)

    ;; 実行結果
    ;; {:test 1, :pass 3, :fail 0, :error 0, :type :summary}

<a id="org50c296a"></a>

## Mock の話

上の handler の例は非常に簡単な単体テストですね。
しかし実際に開発していると DB との連携やら Firebase との通信やらの部分が副作用として関数に含まれてしまうことがあります。
そのようなケースに対応するには、 `with-redefs` を用いると良いでしょう。

    ;; 仕様定義
    (s/def ::db any?)
    (s/def ::birthday pos-int?)
    (s/def ::raw-user-info (s/keys :req-un [::first-name ::last-name ::birthday]))
    (s/def ::user-info (s/keys :req-un [::full-name ::birthday]))
    (s/def ::result (s/keys :req-un [::status ::user-info]))

    (s/fdef get-user-info
     :args (s/cat :db ::db :first-name ::first-name :last-name ::last-name)
     :ret ::raw-user-info)

    (s/fdef complex-handler
     :args (s/cat :params ::params)
     :ret ::result)

    ;; 実装
    ;; DB へのコネクタ
    (def db nil)
    (defn get-user-info [db first-name last-name]
     ;; 何らかの SQL 処理
    )

    (defn complex-handler [params]
      (let [{:keys [first-name last-name]} params
            {:keys [firts-name last-name birthday]} (get-user-info db first-name last-name)
            full-name (format "%s %s" first-name last-name)]
       {:status :success
        :user-info {:full-name full-name :birthday birthday}}))

    ;; テスト
    (st/instrument)

    (deftest complex-handler-test
     ;; ここで Mock を定義
     (with-redefs [get-user-info (fn [db first-name last-name]
                                     {:first-name first-name
                                      :last-name last-name
                                      :birthday 20210301})]
      (testing "complex-handler test with mock"
        (let [params {:first-name "Mokke" :last-name "Meguru"}
              result (complex-handler params)]
          (is (= :success (-> result :status)))
          (is (= "Mokke Meguru" (-> result :user-info :full-name)))
          (is (= 20210301 (-> result :user-info :birthday)))))))

    (run-tests)
    ;; 結果
    ;; {:test 2, :pass 6, :fail 0, :error 0, :type :summary}

参考: <https://clojuredocs.org/clojure.core/with-redefs>
