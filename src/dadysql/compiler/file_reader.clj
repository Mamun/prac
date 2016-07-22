(ns dadysql.compiler.file-reader
  (:require [clojure.java.io :as io]
            [clojure.spec :as s]
            [clojure.tools.reader.edn :as edn]
            [dadysql.core :refer :all]))


(defn- tie-file-reader
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
                (string? v) (if (:dadysql.core/sql f)
                              acc
                              (->> (clojure.string/split (clojure.string/trim v) #";")
                                   (mapv clojure.string/trim)
                                   (assoc f :dadysql.core/sql)
                                   (conj r)))
                :else (conj acc v)))
            ) (list) w))



(defn read-file
  [file-name ]
  (-> file-name
      (tie-file-reader)
      (map-sql-tag)
      (reverse)))



(comment


  (require '[tie_edn])



  (load "./tie_edn.clj")

  )