(ns picture-gallery.interface.gateway.auth.auth-service
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.auth :as auth-domain]
            [orchestra.spec.test :as st]
            [integrant.core :as ig]))

(defprotocol Auth
  (decode-id-token [this encrypted-id-token]))

(defn auth-repository? [inst]
  (satisfies? Auth inst))

(s/def ::auth-repository auth-repository?)

(s/fdef decode-id-token
  :args (s/cat :this ::auth-repository
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

;; (println
;;  (decode-id-token
;;   (:picture-gallery.infrastructure.firebase.core/firebase system)
;;   "eyJhbGciOiJSUzI1NiIsImtpZCI6IjRlMDBlOGZlNWYyYzg4Y2YwYzcwNDRmMzA3ZjdlNzM5Nzg4ZTRmMWUiLCJ0eXAiOiJKV1QifQ.eyJuYW1lIjoiTWVndXJ1IiwicGljdHVyZSI6Imh0dHBzOi8vbGgzLmdvb2dsZXVzZXJjb250ZW50LmNvbS9hLS9BT2gxNEdoeWhoSDZ6VmdHMnV1Szh0SWRxWXZVcWQ4UG1fM2hHQkpZVDFTMW13PXM5Ni1jIiwiaXNzIjoiaHR0cHM6Ly9zZWN1cmV0b2tlbi5nb29nbGUuY29tL3NhbXBsZS1waWN0dXJlLWdhbGxlcnktYzEycmIiLCJhdWQiOiJzYW1wbGUtcGljdHVyZS1nYWxsZXJ5LWMxMnJiIiwiYXV0aF90aW1lIjoxNjE1OTM2Mzc4LCJ1c2VyX2lkIjoiTk94ME9BbGNROGFBNW5lREh3Z3dKMXByTWVrMiIsInN1YiI6Ik5PeDBPQWxjUThhQTVuZURId2d3SjFwck1lazIiLCJpYXQiOjE2MTYxMDUxMDcsImV4cCI6MTYxNjEwODcwNywiZW1haWwiOiJtZWd1cnUubW9ra2VAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImZpcmViYXNlIjp7ImlkZW50aXRpZXMiOnsiZ29vZ2xlLmNvbSI6WyIxMTAzODc4NjU0NTU0MjA5NDI4MTgiXSwiZW1haWwiOlsibWVndXJ1Lm1va2tlQGdtYWlsLmNvbSJdfSwic2lnbl9pbl9wcm92aWRlciI6Imdvb2dsZS5jb20ifX0.C5AR6kAc8a40uo0Lb4Te0a61ShSVWf3xoK0JkuXFTaKosl4i3UCxds4UKHb8Y-olxmYhHJ3zgvk3omPOeDeX3rQiFJijDiGkKDW-Aue0jIE13IRVDUjQDmue878WXmSYMQac7TGnaR8yT5GiNYj7AzOkRglFQI0rv2ussPe5EZ_0rDXdUzJuEfKTDHUAaYjb-dmjAMY90gfHU_1h13gu8Zkh1x1R0Yjg01vp5gj7vAaXx4cd8IPdhOvC00zkXWZ3NweRU1jg0ceYdgoVtljHaX5Fqu2kkjqxXQpYhlfEFK2F9bjv2rCp_J4iqUxX5qrEQ4U2ino4HT49_EmC1K0l0A"))

;; (ig/halt! system)
