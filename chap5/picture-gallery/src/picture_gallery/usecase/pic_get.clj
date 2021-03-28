(ns picture-gallery.usecase.pic-get
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.interface.gateway.database.pics-repository :as pics-repository]
            [picture-gallery.domain.pics :as pics-domain]
            [picture-gallery.domain.error :as error-domain]
            [picture-gallery.utils.error :refer [err->> border-error]]))

(s/fdef pic-get
  :args (s/cat :db ::pics-repository/pics-repository
               :input-model ::pics-repository/pic-get-input)
  :ret (s/or :success (s/tuple ::pics-domain/pic-model nil?)
             :failure (s/tuple nil? ::error-domain/error)))

(defn get-pic [{:keys [db input-model] :as m}]
  (let [[pic-model err] (border-error {:function #(pics-repository/get-pic db (:pic-id input-model))
                                       :error-wrapper error-domain/database-error})]
    (cond
      err [nil err]
      :else [(assoc m :pic pic-model) nil])))

(defn ->output-model [{:keys [pic]}]
  [pic nil])

(defn pic-get [db input-model]
  (err->>
   {:input-model input-model
    :db db}
   get-pic
   ->output-model))
