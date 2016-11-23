(ns dadysql.impl.param-spec-impl
  (:require [dadymodel.core :as sg]
            [dadymodel.util :as sgi]))


(defn filename-as-keyword [file-name-str]
  (-> (clojure.string/split file-name-str #"\.")
      (first)
      (keyword)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn as-merge-spec [spec-coll]
  (if (or (nil? spec-coll)
          (empty? spec-coll))
    spec-coll
    (->> spec-coll
         (remove nil?)
         (cons 'clojure.spec/merge))))


(defn as-relational-spec [[f-spec & rest-spec]]
  (list 'clojure.spec/merge f-spec
        (list 'clojure.spec/keys :req (into [] rest-spec))))


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
        nps (sgi/rename-key-to-namespace-key f-k s-m)]
    (->> (interleave (keys s-m)
                     (keys nps))
         (apply assoc {}))))



(defn assosc-spec-to-m [psk m]
  (if (and (contains? m :dadysql.core/param-spec)
           (get psk (:dadysql.core/name m)))
    (if (= (:dadysql.core/dml m)
           :dadysql.core/dml-insert)
      (let [w (get psk (:dadysql.core/model m))]
        (assoc m :dadysql.core/spec (keyword (str "unq." (namespace w) "/" (name w) ) )))
      (let [w (get psk (:dadysql.core/name m))]
        (assoc m :dadysql.core/spec (keyword (str "ex." (namespace w) "/" (name w) ) ) )))
    m))



(defn eval-and-assoc-spec [file-name coll]
  (do
    (doseq [s (gen-spec file-name coll ) ]
      (eval s))
    (let [m (get-spec-map file-name coll)]
      (mapv (fn [w] (assosc-spec-to-m m w) ) coll))))
