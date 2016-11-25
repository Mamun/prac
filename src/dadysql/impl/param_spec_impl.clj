(ns dadysql.impl.param-spec-impl
  (:require [dadysql.clj.fail :as f]
            [clojure.spec :as s]
            [spec-model.core :as sg]
            [spec-model.util :as sgi]))


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


(defn get-query-spec [coll]
  (->> (filter :dadysql.core/param-spec coll)
       (filter (fn [m] (not= (:dadysql.core/dml m)
                             :dadysql.core/dml-insert)))
       (into {} (map (juxt :dadysql.core/name :dadysql.core/param-spec)))))


(defn get-model-spec [coll]
  (->> (filter :dadysql.core/param-spec coll)
       (filter (fn [m] (= (:dadysql.core/dml m)
                          :dadysql.core/dml-insert)))
       (group-by :dadysql.core/model)
       (map (fn [[k v]] {k (apply merge (mapv :dadysql.core/param-spec v))}))
       (into {})))


(defn gen-spec [file-name coll]
  (let [f-k (filename-as-keyword file-name)
        q-spec (get-query-spec coll)
        m-spec (get-model-spec coll)]
    (into
      (sg/gen-spec f-k m-spec {:spec-model.core/gen-type    #{:spec-model.core/un-qualified}
                               :spec-model.core/gen-list?   false
                               :spec-model.core/gen-entity? false})
      (reverse
        (sg/gen-spec f-k q-spec {:spec-model.core/gen-type    #{:spec-model.core/ex
                                                               :spec-model.core/un-qualified}
                                 :spec-model.core/gen-list?   false
                                 :spec-model.core/gen-entity? false})))))



(defn- assosc-spec-to-m [f-k m]
  (if (contains? m :dadysql.core/param-spec)
    (if (= (:dadysql.core/dml m)
           :dadysql.core/dml-insert)
      (let [w (keyword (str "unq." (name f-k) "/" (name (:dadysql.core/model m))))]
        (assoc m :dadysql.core/spec w))
      (let [w (keyword (str "ex." (name f-k) "/" (name (:dadysql.core/name m))))]
        (assoc m :dadysql.core/spec w)))
    m))



(defn eval-and-assoc-spec [file-name cata-coll]
  (do
    (doseq [s (gen-spec file-name cata-coll)]
      (eval s))
    (let [;m (get-spec-map file-name cata-coll)
          f-k (filename-as-keyword file-name)]
      (mapv (fn [w] (assosc-spec-to-m f-k w)) cata-coll))))



(defn validate-param-spec [tm-coll req-m]
  (let [param-spec (condp = (:dadysql.core/op req-m)
                     :dadysql.core/op-push
                     (-> (map :dadysql.core/spec tm-coll)
                         (remove nil?)
                         (as-relational-spec))
                     (-> (map :dadysql.core/spec tm-coll)
                         (doall)
                         (as-merge-spec)))]
    (if (and (nil? param-spec)
             (empty? param-spec))
      tm-coll
      (if (s/valid? (eval param-spec) (:dadysql.core/param req-m))
        tm-coll
        (f/fail (s/explain-str (eval param-spec) (:dadysql.core/param req-m)))))))
