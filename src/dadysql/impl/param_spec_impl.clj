(ns dadysql.impl.param-spec-impl
  (:require [dadysql.clj.spec-generator :as sg]))


(defn filename-as-keyword [file-name-str]
  (-> (clojure.string/split file-name-str #"\.")
      (first)
      (keyword)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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


(defn gen-spec [file-name coll]
  (let [f-k (filename-as-keyword file-name)]
    (->> (get-param-spec coll)
         (sg/gen-spec f-k))))



(defn get-spec-map [file-name coll]
  (let [f-k (filename-as-keyword file-name)
        s-m (get-param-spec coll)
        nps (sg/assoc-ns-key f-k s-m)]
    (->> (interleave (keys s-m)
                     (keys nps))
         (apply assoc {}))))



(defn assosc-spec-to-m [psk m]
  (if (and (contains? m :dadysql.core/param-spec)
           (get psk (:dadysql.core/name m)))
    (if (= (:dadysql.core/dml m)
           :dadysql.core/dml-insert)
      (assoc m :dadysql.core/spec (get psk (:dadysql.core/model m)))
      (assoc m :dadysql.core/spec (get psk (:dadysql.core/name m))))
    m))



(defn eval-and-assoc-spec [file-name coll]
  (do
    (apply sg/eval-spec (gen-spec file-name coll ) )
    (let [m (get-spec-map file-name coll)]
      (mapv (fn [w] (assosc-spec-to-m m w) ) coll))))
