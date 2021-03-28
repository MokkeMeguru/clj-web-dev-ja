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

(s/fdef pic-post-tcc
  :args (s/cat :m (s/keys :req-un [::input-model ::exist-user ::db ::image-db]))
  :ret (s/or :success (s/tuple (s/keys :req-un [::tcc-status]) nil?)
             :failure (s/tuple nil? ::error-domain/error)))
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
2
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

(defn pic-post [db auth image-db input-model]
  (err->>
   {:input-model input-model
    :auth auth
    :db db
    :image-db image-db}
   decode-id-token
   get-exist-user-has-id-token
   pic-post-tcc
   ->output-model))

;; (st/instrument)

;; (def image-files
;;   [(io/file (io/resource "sample.jpg"))
;;    (io/file (io/resource "sample.jpg"))])

;; (def system (ig/init {:picture-gallery.infrastructure.env/env {}
;;                       :picture-gallery.infrastructure.logger/logger {:env (ig/ref :picture-gallery.infrastructure.env/env)}
;;                       :picture-gallery.infrastructure.sql.sql/sql {:env (ig/ref :picture-gallery.infrastructure.env/env)
;;                                                                    :logger (ig/ref :picture-gallery.infrastructure.logger/logger)}
;;                       :picture-gallery.infrastructure.firebase.core/firebase {:env (ig/ref :picture-gallery.infrastructure.env/env)}
;;                       :picture-gallery.infrastructure.image-db.core/image-db {:env (ig/ref :picture-gallery.infrastructure.env/env)}}))

;; (def e-id-token
;;   "eyJhbGciOiJSUzI1NiIsImtpZCI6ImY4NDY2MjEyMTQxMjQ4NzUxOWJiZjhlYWQ4ZGZiYjM3ODYwMjk5ZDciLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiTWVndXJ1IiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS9hLS9BT2gxNEdoeWhoSDZ6VmdHMnV1Szh0SWRxWXZVcWQ4UG1fM2hHQkpZVDFTMW13PXM5Ni1jIiwiaXNzIjoiaHR0cHM6Ly9zZWN1cmV0b2tlbi5nb29nbGUuY29tL3NhbXBsZS1waWN0dXJlLWdhbGxlcnktYzEycmIiLCJhdWQiOiJzYW1wbGUtcGljdHVyZS1nYWxsZXJ5LWMxMnJiIiwiYXV0aF90aW1lIjoxNjE2NzM2NzEwLCJ1c2VyX2lkIjoiTk94ME9BbGNROGFBNW5lREh3Z3dKMXByTWVrMiIsInN1YiI6Ik5PeDBPQWxjUThhQTVuZURId2d3SjFwck1lazIiLCJpYXQiOjE2MTY3NTA1NTIsImV4cCI6MTYxNjc1NDE1MiwiZW1haWwiOiJtZWd1cnUubW9ra2VAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZ29vZ2xlLmNvbSI6WyIxMTAzODc4NjU0NTU0MjA5NDI4MTgiXSwiZW1haWwiOlsibWVndXJ1Lm1va2tlQGdtYWlsLmNvbSJdfSwic2lnbl9pbl9wcm92aWRlciI6Imdvb2dsZS5jb20ifX0.QYVGNHXuoYj9I75VgSUVTne7h943GKKRmVIPRa7CzI06p-gW5vTM2XRDOOudcgotmj3fQrV2XK0FWjZbGHERu-pJDg3knouCZJtvxWtwn4znmf-a6ohMUPZK0eRVEZm2R5IcI-K6JDWEzAqR3-ZuviRSUYh_IU4LhalIym9sX3FXyyEDREKroTKlq8DSxacgr6jHNh-zBD7QARHPTK_I6dGgpxOLNKCpkDAbRg8UWCVq_5wHLNfz_hsVxBI2suQD4wluWeZenMp9nSek54TYnaEHumehRJbqef6wio3oP0-fSu9TBXDoyj2az_X6wDDDEHDyEHHGUpguyWdwKRNdMg")

;; (pic-post
;;  (:picture-gallery.infrastructure.sql.sql/sql system)
;;  (:picture-gallery.infrastructure.firebase.core/firebase system)
;;  (:picture-gallery.infrastructure.image-db.core/image-db system)
;;  {:encrypted-id-token e-id-token :image-files image-files :title "hello2" :description "nice to meet you"})

;; (ig/halt! system)
