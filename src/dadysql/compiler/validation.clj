(ns dadysql.compiler.validation
  (:require [dadysql.constant :refer :all]
            [clojure.spec :as s]))


(defn do-validate! [coll]
  (let [w (s/conform :dadysql.compiler.spec/compiler-spec coll)]
    (if (= w :clojure.spec/invalid)
      (do
        (println (s/explain :dadysql.compiler.spec/compiler-spec coll))
        (throw (ex-info "Compile failed " (s/explain-data :dadysql.compiler.spec/compiler-spec coll)))))))



(defn distinct-name!
  [m-coll]
  (let [i-coll (->> m-coll
                    (map (juxt name-key))
                    (flatten))]
    (if-not (apply distinct? i-coll)
      (let [w (->> (frequencies i-coll)
                   (filter (fn [[_ v]]
                             (if (< 1 v) true false)))
                   (into {}))]
        (throw (Exception. (str "Found duplicate name " w)))))))


(defn count-sql-and-name!
  [m]
  ;(clojure.pprint/pprint m)
  (let [sqls (sql-key m)
        name-coll (name-key m)
        t-sqls (count sqls)
        t-iden (count name-coll)]
    (when-not (= t-sqls t-iden)
      (if (> t-sqls t-iden)
        (throw (Exception. (format "Name not found for \" %s \" " (str sqls))))
        (throw (Exception. (format "Sql statement not found for \" %s \" " (str name-coll))))))
    m))
