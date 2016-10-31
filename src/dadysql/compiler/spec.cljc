(ns dadysql.compiler.spec
  (:require [clojure.spec :as s]
            [dady.spec :as ds]
            [dady.spec :as ds]))


(s/def :dadysql.core/common
  (s/merge
    (s/keys :opt [:dadysql.core/timeout
                  :dadysql.core/column
                  :dadysql.core/result
                  :dadysql.core/param-coll
                  :dadysql.core/param-spec])))



(s/def :dadysql.core/extend
  (s/every-kv keyword? (s/merge (s/keys :opt [:dadysql.core/model]) :dadysql.core/common)))



(comment

  (s/exercise :dadysql.core/common)

  (clojure.pprint/pprint
    (s/exercise :dadysql.core/extend 1))

  )



(s/def :dadysql.core/module (s/merge
                              :dadysql.core/common
                              (s/keys :req [:dadysql.core/name :dadysql.core/sql]
                                      :opt [:dadysql.core/model :dadysql.core/skip :dadysql.core/group :dadysql.core/commit :dadysql.core/extend])))


(s/def :dadysql.core/spec-file symbol?)

(s/def :dadysql.core/global (s/keys :req [:dadysql.core/name]
                                    :opt [:dadysql.core/timeout :dadysql.core/read-only? :dadysql.core/tx-prop :dadysql.core/file-reload :dadysql.core/reserve-name :dadysql.core/join :dadysql.core/spec-file]))


(s/def :dadysql.core/compiler-spec
  (clojure.spec/cat
    :global (s/? :dadysql.core/global)
    :module (s/* :dadysql.core/module)))




(defn validate-input-spec! [coll]
  (let [w (s/conform :dadysql.core/compiler-spec coll)]
    (if (= w :clojure.spec/invalid)
      (do
        (println (s/explain-str :dadysql.core/compiler-spec coll))
        (throw (ex-info "Compile failed " (s/explain-data :dadysql.core/compiler-spec coll)))))))




(defn assoc-param-spec [parent-ns m]
  (if-not (contains? m :dadysql.core/param-spec)
    m
    (->> (ds/create-ns-key parent-ns (:dadysql.core/name m))
         (assoc m :dadysql.core/spec))))



(defn gen-spec [parent-ns coll]
  (let [coll (filter :dadysql.core/param-spec coll)
        g-coll (group-by :dadysql.core/dml coll)

        ]
    (clojure.pprint/pprint g-coll)
    (for [[k v] g-coll
          m v]
      (->> (hash-map (:dadysql.core/name m)
                     (:dadysql.core/param-spec m))))))




(defn eval-param-spec-batch [file-name coll]
  (let [f-k (ds/filename-as-keyword file-name)
        s-m (gen-spec f-k coll)]
    (mapv #(ds/eval-spec (ds/map->spec f-k %)) s-m)
    (mapv #(assoc-param-spec f-k %) coll)))



