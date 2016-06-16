(ns dadysql.compiler.file-reader
  (:require [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            [dadysql.constant :refer :all]
            [dadysql.compiler.core :as cpl]))


(defn tie-file-reader
  [file-name]
  (let [fs (-> file-name
               (io/resource)
               (slurp)
               (clojure.string/replace #"\n" " "))]
    (for [ms (clojure.string/split fs #"/\*")
          :when (not (clojure.string/blank? ms))
          m (clojure.string/split ms #"\*/")
          :when (not (clojure.string/blank? m))]
      (if (.startsWith (clojure.string/triml m) "{")
        (do
          (edn/read-string
            (clojure.string/lower-case m)))
        m))))


(defn map-sql-tag
  [w]
  (reduce (fn [acc v]
            (let [[f & r] acc]
              (cond
                (nil? f) (conj acc v)
                (string? v) (if (sql-key f)
                              acc
                              (->> (clojure.string/trim v)
                                   (assoc f sql-key)
                                   (conj r)))
                :else (conj acc v)))
            ) (list) w))



(defn read-file
  [file-name pc]
  (-> file-name
      (tie-file-reader)
      (map-sql-tag)
      (cpl/do-compile)))



