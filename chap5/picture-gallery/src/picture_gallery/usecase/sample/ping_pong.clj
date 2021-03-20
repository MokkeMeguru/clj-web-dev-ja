(ns picture-gallery.usecase.sample.ping-pong
  (:require [clojure.spec.alpha :as s]
            [picture-gallery.domain.sample :as sample-domain]
            [picture-gallery.domain.error :as error-domain]
            [orchestra.spec.test :as st]))

(s/fdef ping-pong
  :args (s/cat :input-model ::sample-domain/ping-pong-input)
  :ret (s/or :success (s/cat :ping-pong-output ::sample-domain/ping-pong-output :error nil?)
             :failure (s/cat :ping-pong-output nil? :error ::error-domain/error)))

(defn ping-pong [input-model]
  (let [{:keys [ping comment]} input-model
        output-model (cond-> {:pong "pong"}
                       comment (assoc :comment comment))]
    [output-model nil]))

;; (st/instrument)
;; (ping-pong {:ping "ping" :comment "hello"})
;; (ping-pong {:ping "ping" :comment "nice to meet you"})
;; (ping-pong {:ping "ping"})
