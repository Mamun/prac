(ns dadysql.compiler.validation
  (:require [dadysql.constant :refer :all]
            [clojure.spec :as s]))


(defn validate-spec! [coll]
  (let [w (s/conform :dadysql.compiler.spec/compiler-spec coll)]
    (if (= w :clojure.spec/invalid)
      (do
        (println (s/explain :dadysql.compiler.spec/compiler-spec coll))
        (throw (ex-info "Compile failed " (s/explain-data :dadysql.compiler.spec/compiler-spec coll)))))))


(defn validate-distinct-name! [coll]
  (let [i-coll (flatten (mapv name-key coll))]
    (if-not (apply distinct? i-coll)
      (let [w (->> (frequencies i-coll)
                   (filter (fn [[_ v]]
                             (if (< 1 v) true false)))
                   (into {}))]
        (throw (ex-info (format "Found duplicate name %s" (str (keys w))) w))))))


(defn validate-name-model! [coll]
  (let [i-coll (->> (mapv (juxt name-key model-key) coll)
                    (filter (fn [v] (every? vector? v) ) ))]
    (doseq [[name-coll model-coll] i-coll]
      (let [t-iden (count name-coll)
            t-sqls (count model-coll)]
        (when-not (= t-sqls t-iden)
          (if (> t-sqls t-iden)
            (throw (Exception. (format "Name not found for \" %s \" " (str model-coll))))
            (throw (Exception. (format "Model not found for \" %s \" " (str name-coll))))))))))


(defn validate-name-sql! [coll]
  (let [i-coll (->> (mapv (juxt name-key sql-key) coll)
                    (filter (fn [v] (every? vector? v) ) ))]
    (doseq [[name-coll sql-coll] i-coll]
      (let [t-iden (count name-coll)
            t-sqls (count sql-coll)]
        (when-not (= t-sqls t-iden)
          (if (> t-sqls t-iden)
            (throw (Exception. (format "Name not found for \" %s \" " (str sql-coll))))
            (throw (Exception. (format "Sql statement not found \" %s \" " (str name-coll))))))))))


(defn validate-extend-name! [coll]
  (let [i-coll (->> (mapv (juxt name-key extend-meta-key) coll)
                    (mapv (fn [[n e]]
                            (let [n (if (vector? n)
                                      (into #{} n)
                                      #{n}
                                      ) ]
                              [n (keys e)])))
                    (filter (fn [[n e ]] (if (or
                                               (nil? e)
                                               (empty? e))
                                           false true  ) ) ))]
    (doall
      (for [[name-coll ex-name-list] i-coll
            ex-name ex-name-list]
        (if-not (contains? name-coll ex-name )
          (throw (Exception. (format "Extend name not found for \" %s \" " ex-name))))))))

