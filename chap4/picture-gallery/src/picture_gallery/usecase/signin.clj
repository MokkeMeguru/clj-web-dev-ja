(ns picture-gallery.usecase.signin
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.auth :as auth-domain]
            [picture-gallery.domain.error :as error-domain]))

(s/fdef signin
  :args (s/cat :input-model ::auth-domain/signin-input)
  :ret (s/or :success (s/cat :signin-output ::auth-domain/signin-output :error nil)
             :failure (s/cat :signin-output nil? :error ::error-domain/error)))

(defn signin [input-model]
  [{:user-id "123123123123"} nil])
