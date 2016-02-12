(ns tiesql.plugin.base-impl
  (:require [schema.core :as s]
            [tiesql.common :refer :all]
            [tiesql.proto :refer :all]
            [tiesql.plugin.util :as cu]
            [tiesql.common :as cc]
            [tiesql.plugin.param-impl :as p]
            [tiesql.plugin.validation-impl :as v]
            [tiesql.plugin.sql-bind-impl :as sql]
            [tiesql.plugin.join-impl :as join]))



(defleaf DocKey [lanme])
(defleaf NameKey [lname])
(defleaf ModelKey [lname])
(defleaf GroupKey [lname])
(defleaf TimeoutKey [lname])
(defleaf CommitKey [lname])
(defleaf SkipKey [lname])
(defleaf ColumnKey [lname lorder ptype])
(defleaf ResultKey [lname lorder ptype])


(defbranch ExtendKey [lname coll lorder])
(defbranch Modulekey [lname coll lorder])
(defbranch GlobalKey [lname coll lorder])


(defn new-leaf-node-coll
  []
  (vector (NameKey. name-key)
          (DocKey. doc-key)
          (ModelKey. model-key)
          (GroupKey. group-key)
          (TimeoutKey. timeout-key)
          (CommitKey. commit-key)
          (SkipKey. skip-key)
          (ColumnKey. column-key 5 :output)
          (ResultKey. result-key 10 :output)
          (p/new-param-key 5 (p/new-child-keys))
          (v/new-validation-key 10 (v/new-child-coll))
          (sql/new-sql-key 75 (sql/new-childs-key))
          (join/new-join-key)))


(defn conj-extend-key-node
  ([node-coll ks]
   (->> (ExtendKey. extend-meta-key (select-node node-coll ks) 0)
        (conj node-coll)))
  ([node-coll]
   (conj-extend-key-node node-coll [param-key
                                    column-key
                                    timeout-key
                                    validation-key
                                    skip-key
                                    result-key])))


(defn new-module-key-node
  [node-coll]
  (Modulekey. module-key node-coll 0))


(defn select-module-node-processor
  [root-node]
  (-> (get-node-from-path root-node [module-key])
      (-childs)
      (filter-node-processor)))


(defn new-global-key-node
  ([node-coll ks]
   (GlobalKey. global-key (select-node node-coll ks) 0))
  ([node-coll]
   (new-global-key-node node-coll [doc-key
                                   param-key
                                   column-key
                                   validation-key
                                   result-key
                                   extend-meta-key
                                   timeout-key
                                   join-key])))


(defn new-root-node
  []
  (let [impl (new-leaf-node-coll)
        impl (conj-extend-key-node impl)
        gpc (new-global-key-node impl)
        mpc (new-module-key-node impl)]
    (vector gpc mpc)))


(defn branch?
  [node]
  (satisfies? IBranchNode node))

(defn childrent
  [node]
  (-childs node))





(defn resolve-model?
  [v]
  (if (keyword? v)
    true
    (every? keyword? v)))


(defn check-tx-proc?
  [v]
  (let [tt [{:isolation #{:none :read-committed :read-uncommitted :repeatable-read :serializable}}
            {:read-only? #{true false}}]
        v (partition 2 v)
        is-contains? (fn [k kv]
                       (->> (map #(contains? (k %) kv) tt)
                            (some true?)))]
    (reduce (fn [acc [k kv]]
              (if-not (is-contains? k kv)
                (reduced false)
                acc)
              ) true v)))


(defn default-global-schema
  []
  {(s/required-key name-key)         s/Keyword
   (s/optional-key tx-prop)          (s/pred check-tx-proc? 'check-tx-proc)
   (s/optional-key file-name-key)    s/Str
   (s/optional-key file-reload-key)  boolean
   (s/optional-key reserve-name-key) #{s/Keyword}
   (s/optional-key ds-key)           s/Any})



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
  ExtendKey
  (-schema [this]
    (let [r {(s/optional-key model-key) s/Keyword}]
      {(s/optional-key extend-meta-key)
       {s/Keyword (cu/merge-compiler-schema r (:coll this))}}))
  (-compiler-validate [this d-map]

    (s/validate (-schema this) d-map))
  (-compiler-emit [this v-map]
    (->> (keys v-map)
         (reduce (fn [acc k]
                   (assoc acc k (cu/compiler-emit-batch (:coll this) (k v-map)))
                   ) {})))
  ;;Global key
  GlobalKey
  (-schema [this]
    (cu/merge-compiler-schema (default-global-schema) (:coll this)))
  (-compiler-validate [this d-map]
    (s/validate (-schema this) d-map))
  (-compiler-emit [this v-map]
    (cu/compiler-emit-batch (:coll this) v-map))
  ;; Module key
  Modulekey
  (-schema [this]
    (cu/merge-compiler-schema {} (:coll this)))
  (-compiler-validate [this v]
    (s/validate (-schema this) v))
  (-compiler-emit [this v]
    (cu/compiler-emit-batch (:coll this) v)))




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
        (cc/failed? output)
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
            (cc/failed? tm)
            (cc/failed? output)
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






