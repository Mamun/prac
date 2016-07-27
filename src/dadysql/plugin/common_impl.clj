(ns dadysql.plugin.common-impl
  (:require
    [dadysql.spec :refer :all]
    [dady.fail :as f]
    [dady.proto :refer :all]))



(defleaf DocKey [lanme])
(defleaf NameKey [lname])
(defleaf ModelKey [lname])
(defleaf GroupKey [lname])
(defleaf TimeoutKey [lname])
(defleaf CommitKey [lname])
(defleaf SkipKey [lname])
(defleaf ColumnKey [lname lorder ptype])
(defleaf ResultKey [lname lorder ptype])


(defn new-doc-key []
  (DocKey. :dadysql.spec/doc))

(defn new-name-key []
  (NameKey. :dadysql.spec/name))

(defn new-model-key []
  (ModelKey. :dadysql.spec/model))

(defn new-group-key []
  (GroupKey. :dadysql.spec/group))

(defn new-timeout-key []
  (TimeoutKey. :dadysql.spec/timeout))

(defn new-commit-key []
  (CommitKey. :dadysql.spec/commit))

(defn new-skip-key []
  (SkipKey. :dadysql.spec/skip))

(defn new-column-key []
  (ColumnKey. :clojure.core/column 5 :output))

(defn new-result-key []
  (ResultKey. :dadysql.spec/result 10 :output))




(defn do-result
  [tm]
  (if-not (or (= (:dadysql.spec/dml-key tm) :dadysql.spec/dml-select)
              (= (:dadysql.spec/dml-key tm) :dadysql.spec/dml-call))
    tm
    (let [result (:dadysql.spec/result tm)
          output (output-key tm)]
      (cond
        (nil? result)
        tm
        (f/failed? output)
        tm
        (and (empty? output)
             (contains? result :dadysql.spec/single))
        (assoc tm output-key {})
        (and (contains? result :dadysql.spec/array)
             (contains? result :dadysql.spec/single))
        (assoc tm output-key [(first output) (second output)])
        (contains? result :dadysql.spec/single)
        (assoc tm output-key (first output))
        :else tm))))


(defn do-column
  [tm]
  (if-not (or (= (:dadysql.spec/dml-key tm) :dadysql.spec/dml-call)
              (= (:dadysql.spec/dml-key tm) :dadysql.spec/dml-select))
    tm
    (let [column (:clojure.core/column tm)
          output (output-key tm)]
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
             (assoc tm output-key))))))


(extend-protocol INodeProcessor
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






