(ns picture-gallery.utils.error
  (:require [taoensso.timbre :as timbre]))

(defn bind-error [f [val err]]
  (if (nil? err)
    (f val)
    [nil err]))

(defmacro err->> [val & fns]
  (let [fns (for [f fns] `(bind-error ~f))]
    `(->> [~val nil]
          ~@fns)))

(defn border-error [{:keys [function error-wrapper]}]
  (try (let [result (function)]
         [result nil])
       (catch clojure.lang.ExceptionInfo e
         (timbre/warn (.getMessage e))
         [nil (error-wrapper (str "spec exception: " (.getMessage e)))])
       (catch java.lang.AssertionError e
         (timbre/warn (.getMessage e))
         [nil (error-wrapper (str "spec exception: " (.getMessage e)))])
       (catch Exception e
         (timbre/warn e)
         [nil (error-wrapper (str "unknown exception: " (.getMessage e)))])))
