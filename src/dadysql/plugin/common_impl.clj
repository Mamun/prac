(ns dadysql.plugin.common-impl
  (:require
    [dady.fail :as f]))


(defn do-result
  [tm]
  (if-not (or (= (:dadysql.core/dml-key tm) :dadysql.core/dml-select)
              (= (:dadysql.core/dml-key tm) :dadysql.core/dml-call))
    tm
    (let [result (:dadysql.core/result tm)
          output (:dadysql.core/output tm)]
      (cond
        (nil? result)
        tm
        (f/failed? output)
        tm
        (and (empty? output)
             (contains? result :dadysql.core/result-single))
        (assoc tm :dadysql.core/output {})
        (and (contains? result :dadysql.core/result-array)
             (contains? result :dadysql.core/result-single))
        (assoc tm :dadysql.core/output [(first output) (second output)])
        (contains? result :dadysql.core/result-single)
        (assoc tm :dadysql.core/output (first output))
        :else tm))))


(defn do-column
  [tm]
  (if-not (or (= (:dadysql.core/dml-key tm) :dadysql.core/dml-call)
              (= (:dadysql.core/dml-key tm) :dadysql.core/dml-select))
    tm
    (let [column (:clojure.core/column tm)
          output (:dadysql.core/output tm)]
      (cond
        (or (nil? column)
            (f/failed? tm)
            (f/failed? output)
            (not (map? (first output))))
        tm
        :else
        (->> (repeat column)
             (map clojure.set/rename-keys output)
             (into [])
             (assoc tm :dadysql.core/output))))))


