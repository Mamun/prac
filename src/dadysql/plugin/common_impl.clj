(ns dadysql.plugin.common-impl
  (:require
    [dady.fail :as f]
    ))


;(defleaf ColumnKey [lname lorder ptype])
;(defleaf ResultKey [lname lorder ptype])


#_(defn new-column-key []
  (ColumnKey. :clojure.core/column 5 :output))

#_(defn new-result-key []
  (ResultKey. :dadysql.core/result 10 :output))



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
             (contains? result :dadysql.core/single))
        (assoc tm :dadysql.core/output {})
        (and (contains? result :dadysql.core/array)
             (contains? result :dadysql.core/single))
        (assoc tm :dadysql.core/output [(first output) (second output)])
        (contains? result :dadysql.core/single)
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


#_(extend-protocol INodeProcessor
  ColumnKey
  (-lorder [this] (:lorder this))
  (-process-type [this] (:ptype this))
  (-process? [_ _] true)
  (-process [_ m] (do-column m))
  ResultKey
  (-lorder [this] (:lorder this))
  (-process-type [this] (:ptype this))
  (-process? [_ _] true)
  (-process [_ m] (do-result m)))






