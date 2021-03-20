(ns picture-gallery.utils.string
  (:require [clojure.string]))

(defn rand-str "
  generate random string with the length 'len
  "
  [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))
