(ns picture-gallery.interface.gateway.auth.auth-service
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.auth :as auth-domain]
            [orchestra.spec.test :as st]
            [integrant.core :as ig]))

(defprotocol Auth
  (decode-id-token [this encrypted-id-token]))

(defn auth-service? [inst]
  (satisfies? Auth inst))

(s/def ::auth-service auth-service?)

(s/fdef decode-id-token
  :args (s/cat :this ::auth-service
               :encrypted-id-token ::auth-domain/encrypted-id-token)
  :ret ::auth-domain/decode-id-token-result)

;; (st/instrument)
;; (def system
;;   (ig/init {:picture-gallery.infrastructure.env/env {}
;;             :picture-gallery.infrastructure.firebase.core/firebase {:env (ig/ref :picture-gallery.infrastructure.env/env)}}))

;; (println
;;  (decode-id-token
;;   (:picture-gallery.infrastructure.firebase.core/firebase system)
;;   "Hello"))

;; (println
;;  (decode-id-token
;;   (:picture-gallery.infrastructure.firebase.core/firebase system)
;;   "eyJhbGciOiJSUzI1NiIsImtpZCI6IjQ4OTQ5ZDdkNDA3ZmVjOWIyYWM4ZDYzNWVjYmEwYjdhOTE0ZWQ4ZmIiLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiTWVndXJ1IiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS9hLS9BT2gxNEdoeWhoSDZ6VmdHMnV1Szh0SWRxWXZVcWQ4UG1fM2hHQkpZVDFTMW13PXM5Ni1jIiwiaXNzIjoiaHR0cHM6Ly9zZWN1cmV0b2tlbi5nb29nbGUuY29tL3NhbXBsZS1waWN0dXJlLWdhbGxlcnktYzEycmIiLCJhdWQiOiJzYW1wbGUtcGljdHVyZS1nYWxsZXJ5LWMxMnJiIiwiYXV0aF90aW1lIjoxNjE1ODk4MTk1LCJ1c2VyX2lkIjoiTk94ME9BbGNROGFBNW5lREh3Z3dKMXByTWVrMiIsInN1YiI6Ik5PeDBPQWxjUThhQTVuZURId2d3SjFwck1lazIiLCJpYXQiOjE2MTU4OTgyMDMsImV4cCI6MTYxNTkwMTgwMywiZW1haWwiOiJtZWd1cnUubW9ra2VAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZ29vZ2xlLmNvbSI6WyIxMTAzODc4NjU0NTU0MjA5NDI4MTgiXSwiZW1haWwiOlsibWVndXJ1Lm1va2tlQGdtYWlsLmNvbSJdfSwic2lnbl9pbl9wcm92aWRlciI6Imdvb2dsZS5jb20ifX0.p071GDlS4Z5amNhwqRAhzO_WHcFlbBMLQBp-HcRUjJquP8GU3TSfVWopzVqzmcfHa1ThKZHtd4jAEm-fAix8z1OUjBuOprDH6CrRzb91Rp69fha4sCs7T_YtLJln60YWCzVKvrTBv2HwLEHhSe07NXryzmpLsHmyissM299AdXpiUy_6xtjxVJ01hIXRDw4n1qCWzCfcU_Pp7dGn35fKsBg2OjlPfb6y5pBhW_XNrZiOOW-Jsi54ZWyJWqpktU88-_P52dhd4h-H08vPsI638Sf6A_2PfH0zNtPUnWgy5wfQGdb_JhXpp5NFIC62FVu5AIliLduN5SXrijCHgVIayQ"))

;; (s/valid? ::auth-domain/decode-id-token-result
;;  (decode-id-token
;;   (:picture-gallery.infrastructure.firebase.core/firebase system)
;;   "eyJhbGciOiJSUzI1NiIsImtpZCI6ImY4NDY2MjEyMTQxMjQ4NzUxOWJiZjhlYWQ4ZGZiYjM3ODYwMjk5ZDciLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiTWVndXJ1IiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS9hLS9BT2gxNEdoeWhoSDZ6VmdHMnV1Szh0SWRxWXZVcWQ4UG1fM2hHQkpZVDFTMW13PXM5Ni1jIiwiaXNzIjoiaHR0cHM6Ly9zZWN1cmV0b2tlbi5nb29nbGUuY29tL3NhbXBsZS1waWN0dXJlLWdhbGxlcnktYzEycmIiLCJhdWQiOiJzYW1wbGUtcGljdHVyZS1nYWxsZXJ5LWMxMnJiIiwiYXV0aF90aW1lIjoxNjE2NzM2NzEwLCJ1c2VyX2lkIjoiTk94ME9BbGNROGFBNW5lREh3Z3dKMXByTWVrMiIsInN1YiI6Ik5PeDBPQWxjUThhQTVuZURId2d3SjFwck1lazIiLCJpYXQiOjE2MTY3NDYxOTAsImV4cCI6MTYxNjc0OTc5MCwiZW1haWwiOiJtZWd1cnUubW9ra2VAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZ29vZ2xlLmNvbSI6WyIxMTAzODc4NjU0NTU0MjA5NDI4MTgiXSwiZW1haWwiOlsibWVndXJ1Lm1va2tlQGdtYWlsLmNvbSJdfSwic2lnbl9pbl9wcm92aWRlciI6Imdvb2dsZS5jb20ifX0.ZT9XU_HxxxnhaQIMVyh6g4UvSkNYteuvxkNqtoEUjaKTrTgD14ZOOhwDpCn3tt9AU-dksMRjwVyCk3CcrdRbkP5CaxQpaI2z9y_DK_TKnnsNolszob5JnrfSDuCLh3Bx0zOXW9Fuu8s_zFJ2iPj_-XvylmO82_UQiO2R5d7Jnm4aEh5RMXbsGnQY_yY3yIApJZ40iCRirVSwMtOcH1Js2oOXoT-mgZt_jOPFNXVyVZqIwXjgKqVDZi4f0hwgLUchRjBSu26Sa3lRm7kX1VHhXkHFupntcM1v4SKMJnf69QXxdq79tlMwzEVeSIrYJKcB1gtUweqVe28ht7hnMAWzZg"))

;; (ig/halt! system)
