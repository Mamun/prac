(ns dadysql.plugin.common-impl
  (:require
    [dadysql.core :refer :all]
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
  (DocKey. :dadysql.core/doc))

(defn new-name-key []
  (NameKey. :dadysql.core/name))

(defn new-model-key []
  (ModelKey. :dadysql.core/model))

(defn new-group-key []
  (GroupKey. :dadysql.core/group))

(defn new-timeout-key []
  (TimeoutKey. :dadysql.core/timeout))

(defn new-commit-key []
  (CommitKey. :dadysql.core/commit))

(defn new-skip-key []
  (SkipKey. skip-key))

(defn new-column-key []
  (ColumnKey. :clojure.core/column 5 :output))

(defn new-result-key []
  (ResultKey. :dadysql.core/result 10 :output))




(defn do-result
  [tm]
  (if-not (or (= (:dadysql.core/dml-key tm) dml-select-key)
              (= (:dadysql.core/dml-key tm) dml-call-key))
    tm
    (let [result (:dadysql.core/result tm)
          output (output-key tm)]
      (cond
        (nil? result)
        tm
        (f/failed? output)
        tm
        (and (empty? output)
             (contains? result result-single-key))
        (assoc tm output-key {})
        (and (contains? result result-array-key)
             (contains? result result-single-key))
        (assoc tm output-key [(first output) (second output)])
        (contains? result result-single-key)
        (assoc tm output-key (first output))
        :else tm))))


(defn do-column
  [tm]
  (if-not (or (= (:dadysql.core/dml-key tm) dml-call-key)
              (= (:dadysql.core/dml-key tm) dml-select-key))
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






