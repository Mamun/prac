(ns dadysql.clj.spec-coerce
  (:require [clojure.spec :as s])
  (:import [BigInteger]))


(defn x-int? [x]
  (cond
    (integer? x) x
    (string? x) (try
                  (Integer/parseInt x)
                  (catch Exception e
                    :clojure.spec/invalid))
    :else :clojure.spec/invalid))


(defn x-integer? [x]
  (cond
    (integer? x) x
    (string? x) (try
                  (BigInteger.  x)
                  (catch Exception e
                    :clojure.spec/invalid))
    :else :clojure.spec/invalid))







#_(type


  (s/conform ::id "44234224623425324"))
