(ns dadysql.compiler.spec
  (:require [clojure.spec :as s]
            [dady.spec-util :as ds]
            [dady.spec-util :as ds]
            [dady.spec-util :as ds]
            [dady.spec-util :as ds]))


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




(defn assoc-spec [spec-name-m m]
  (if (and (contains? m :dadysql.core/param-spec)
           (get spec-name-m (:dadysql.core/name m)))
    (assoc m :dadysql.core/spec (get spec-name-m (:dadysql.core/name m)))
    m))



(defn get-param-spec [coll]
  (let [insert-coll (->> (filter :dadysql.core/param-spec coll)
                         (filter (fn [m] (= (:dadysql.core/dml m)
                                            :dadysql.core/dml-insert)))
                         (group-by :dadysql.core/model)
                         (map (fn [[k v]] {k (apply merge (mapv :dadysql.core/param-spec v))}))
                         (into {}))]
    (->> (filter :dadysql.core/param-spec coll)
         (into {} (map (juxt :dadysql.core/name :dadysql.core/param-spec)))
         (merge insert-coll))))




(defn eval-param-spec-batch [file-name coll]
  (let [f-k (ds/filename-as-keyword file-name)
        s-m (get-param-spec coll)
        psk (ds/map->spec-key f-k s-m)]
    ;;Also poosibel to write to file
    ;(ds/write-to-file (name f-k) (ds/map->spec f-k s-m) )
    (ds/eval-spec (ds/map->spec f-k s-m))
    (mapv #(assoc-spec psk %) coll)))



