(ns dadysql.plugin.common-impl
  (:require
    [schema.core :as s]
    [dadysql.constant :refer :all]
    [dady.fail :as f]
    [dady.proto :refer :all]
    [dady.common :as cc]))



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
  (DocKey. doc-key))

(defn new-name-key []
  (NameKey. name-key))

(defn new-model-key []
  (ModelKey. model-key))

(defn new-group-key []
  (GroupKey. group-key))

(defn new-timeout-key []
  (TimeoutKey. timeout-key))

(defn new-commit-key []
  (CommitKey. commit-key))

(defn new-skip-key []
  (SkipKey. skip-key))

(defn new-column-key []
  (ColumnKey. column-key 5 :output))

(defn new-result-key []
  (ResultKey. result-key 10 :output))




(defn resolve-model?
  [v]
  (if (keyword? v)
    true
    (every? keyword? v)))


(extend-protocol INodeCompiler
  DocKey
  (-schema [this]
    {(s/optional-key (-node-name this)) s/Str})
  (-compiler-validate [this v] (s/validate (-schema this) v))
  (-compiler-emit [_ v] v)
  TimeoutKey
  (-schema [this]
    {(s/optional-key (-node-name this)) s/Int})
  (-compiler-validate [this v] (s/validate (-schema this) v))
  (-compiler-emit [_ v] v)
  NameKey
  (-schema [this]
    {(s/required-key (-node-name this)) (s/pred resolve-model? 'resolve-model?)})
  (-compiler-validate [this v] (s/validate (-schema this) v))
  (-compiler-emit [_ w]
    (cc/as-sequential w))
  ModelKey
  (-schema [this]
    {(s/optional-key (-node-name this)) (s/pred resolve-model? 'resolve-model?)})
  (-compiler-validate [this v] (s/validate (-schema this) v))
  (-compiler-emit [_ v] v)
  SkipKey
  (-schema [this]
    {(s/optional-key (-node-name this))
     #{(s/enum validation-key column-key join-key)}})
  (-compiler-validate [this v] (s/validate (-schema this) v))
  (-compiler-emit [_ v] v)
  GroupKey
  (-schema [this]
    {(s/optional-key (-node-name this)) s/Keyword})
  (-compiler-validate [this v] (s/validate (-schema this) v))
  (-compiler-emit [_ v] v)
  CommitKey
  (-schema [this]
    {(s/optional-key (-node-name this)) (s/enum commit-all-key commit-any-key commit-none-key)})
  (-compiler-validate [this v] (s/validate (-schema this) v))
  (-compiler-emit [_ v] v)
  ColumnKey
  (-schema [this]
    {(s/optional-key (-node-name this)) {s/Keyword s/Keyword}})
  (-compiler-validate [_ v] v)
  (-compiler-emit [_ v] v)
  ResultKey
  (-schema [this]
    (let [v #{(s/enum result-array-key result-single-key)}]
      {(s/optional-key (-node-name this)) v}))
  (-compiler-validate [this v] (s/validate (-schema this) v))
  (-compiler-emit [_ v] v)
  ;;Extend key
  )




(defn do-result
  [tm]
  (if-not (or (= (dml-key tm) dml-select-key)
              (= (dml-key tm) dml-call-key))
    tm
    (let [result (result-key tm)
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
  (if-not (or (= (dml-key tm) dml-call-key)
              (= (dml-key tm) dml-select-key))
    tm
    (let [column (column-key tm)
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






