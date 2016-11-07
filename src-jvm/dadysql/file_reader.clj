(ns dadysql.file-reader
  (:require [clojure.java.io :as io]
            [clojure.walk :as w]
            [clojure.spec :as s]
            [dadysql.impl.param-impl :as pl ]
            [clojure.tools.reader.edn :as edn]))


(def alais-map {:doc          :dadysql.core/doc
                :timeout      :dadysql.core/timeout
                :reserve-name :dadysql.core/reserve-name
                :file-reload  :dadysql.core/file-reload
                :tx-prop      :dadysql.core/tx-prop

                :join         :dadysql.core/join
                :1-1          :dadysql.core/join-one-one
                :1-n          :dadysql.core/join-one-many
                :n-1          :dadysql.core/join-many-one
                :n-n          :dadysql.core/join-many-many

                :name         :dadysql.core/name
                :model        :dadysql.core/model
                :group        :dadysql.core/group
                :column       :dadysql.core/column
                :sql          :dadysql.core/sql

                :result       :dadysql.core/result
                :array        :dadysql.core/result-array
                :single       :dadysql.core/result-single

                :commit       :dadysql.core/commit
                :all          :dadysql.core/commit-all
                :any          :dadysql.core/commit-any
                :none         :dadysql.core/commit-none

                :dml-type     :dadysql.core/dml
                :index        :dadysql.core/index

                :skip         :dadysql.core/skip
                :param        :dadysql.core/default-param
                :ref-con      :dadysql.core/param-ref-con
                :ref-key      :dadysql.core/param-ref-key
                :ref-fn-key   :dadysql.core/param-ref-fn-key
                :ref-gen      :dadysql.core/param-ref-gen
                :param-spec   :dadysql.core/param-spec

                :extend       :dadysql.core/extend
                :spec-file    :dadysql.core/spec-file})


(defn key->nskey
  [m mk]
  (w/postwalk (fn [x]
                (if-let [v (get mk x)]
                  v
                  x)) m))


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
                (string? v) (if (:sql f)
                              acc
                              (->> (clojure.string/split (clojure.string/trim v) #";")
                                   (mapv clojure.string/trim)
                                   (assoc f :sql)
                                   (conj r)))
                :else (conj acc v)))
            ) (list) w))


(defn compiler-resolve [coll]
  (w/postwalk (fn [v]
                (if (symbol? v)
                  (resolve v)
                  v)
                ) coll))




(defn compiler-param-resolve [coll]
  (w/postwalk (fn [m]
                (if (and (map? m)
                         (:param m))
                  (update-in m [:param] pl/convert-param-t)
                  m
                  )
                ) coll))




;;Do we need to unquote again here
(defn read-file
  [file-name]
  (-> file-name
      (tie-file-reader)
      (map-sql-tag)
      (reverse)
      (compiler-resolve)
      (compiler-param-resolve)
      (key->nskey alais-map)))



(comment


  (partition-by (fn [[k v]]
                  (keyword? v)
                  )
                {:a :b :t (fn [t] t)})

  ;(var? [int?])

  ;(var? :a)

  ;(var? #'clojure.spec/coll-of)

  ;(coll? {1 2})

  (->> (read-file "tie2.edn.sql")
       (s/conform :dadysql.core/compiler-spec)
       (clojure.pprint/pprint))



  (->
    (read-file "tie3.edn.sql")
    (clojure.pprint/pprint))

  ;(require '[tie_edn])



  (load "./tie_edn.clj")

  )