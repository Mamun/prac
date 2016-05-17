(ns dadysql.plugin.sql-bind-impl
  (:use [dady.node-proto])
  (:require [dadysql.constant :refer :all]
            [dady.fail :as f]
            [dady.common :as cc]
            [schema.core :as s]))


(defn validate-input-not-empty!
  [m]
  (if (and (not-empty (rest (sql-key m)))
           (empty? (input-key m)))
    (f/fail (format "Input is missing for %s " (name-key m)))
    m))


(defn validate-input-type!
  [m]
  (let [dml-type (dml-key m)
        input (input-key m)
        sql (sql-key m)]
    (if (and (not= dml-type dml-insert-key)
             (not-empty (rest sql))
             (not (map? input)))
      (f/fail (format "Input Params for %s will be map format but %s is not map format " sql input))
      m)))


(defn- validate-required-params*!
  [p-set input]
  (let [p-set (into #{} p-set)
        i-set (into #{} (keys input))
        diff-keys (clojure.set/difference p-set i-set)]
    (if-not (empty? diff-keys)
      (f/fail (format "Missing required parameter %s" (pr-str diff-keys)))
      input)))


(defn validate-required-params!
  [m]
  (let [input (cc/as-sequential (input-key m))
        r (-> (f/comp-xf-until (map #(validate-required-params*! (rest (sql-key m)) %)))
              (transduce conj input))]
    (if (f/failed? r)
      r
      m)))


(defn get-vali-type
  [coll id]
  (->> coll
       (filter #(and
                 (= id (first %))
                 (= validation-type-key (second %))))
       (map #(nth % 2))
       (first)))



(defn get-place-holder
  [type v]
  (if (contains? (supers type)
                 clojure.lang.Sequential)
    (clojure.string/join ", " (repeat (count v) "?"))
    "?"))


(defn update-sql-str
  [v sql-str k]
  (clojure.string/replace-first sql-str (re-pattern (str k)) v))


(defn default-proc
  [tm]
  (let [[sql-str & sql-params] (sql-key tm)
        input (input-key tm)
        validation (validation-key tm)
        rf (fn [sql-coll p-key]
             (let [p-value (cc/as-sequential (p-key input))
                   w (-> (get-vali-type validation p-key)
                         (get-place-holder p-value)
                         (update-sql-str (first sql-coll) p-key))
                   q-str (assoc sql-coll 0 w)]
               (reduce conj q-str p-value)))]
    (->> (reduce rf [sql-str] sql-params)
         (assoc tm sql-key))))


(defn insert-proc
  [tm]
  (let [sql (sql-key tm)
        sql-str (reduce (partial update-sql-str "?") sql)]
    (->> (input-key tm)
         (cc/as-sequential)
         (mapv #(cc/select-values %1 (rest sql)))
         (reduce conj [sql-str])
         (assoc tm sql-key))))



(defn do-default-proc
  [tm]
  (f/try-> tm
            validate-input-not-empty!
            validate-input-type!
            validate-required-params!
            default-proc))


(defn do-insert-proc
  [tm]
  (f/try-> tm
            validate-input-not-empty!
            validate-input-type!
            validate-required-params!
            insert-proc))



(defn dml-type
  [v]
  (-> v
      (first)
      (clojure.string/trim)
      (clojure.string/lower-case)
      (clojure.string/split #"\s+")
      (first)
      (keyword)))


(def sql-param-regex #"\w*:[\w|\-|#]+")


(defn sql-str-compiler-emit
  [sql-str]
  (->> (re-seq sql-param-regex sql-str)
       (transduce (comp (map read-string)) conj)
       (reduce (fn [acc v]
                 (let [w (cc/as-lower-case-keyword v)
                       sql-str-w (-> (first acc)
                                     (clojure.string/replace-first (re-pattern (cc/as-string v)) (cc/as-string w)))]
                   (-> (assoc-in acc [0] sql-str-w)
                       (conj w)))
                 ) [sql-str])))


(defn sql-compiler-emit
  [sql-str]
  (let [p (comp (filter not-empty)
                (map sql-str-compiler-emit)
                (map (fn [v] {sql-key v
                              dml-key (dml-type v)})))
        sql (clojure.string/split (clojure.string/trim sql-str) #";")]
    (->> (transduce p conj [] sql)
         (mapv (fn [i m]
                 (assoc m index i)
                 ) (range)))))


(defbranch SqlKey [cname ccoll corder])
(defleaf InsertSqlKey [cname corder])
(defleaf UpdateSqlKey [cname corder])
(defleaf DeleteSqlKey [cname corder])
(defleaf SelectSqlKey [cname corder])
(defleaf CallSqlKey [cname corder])


(defn new-sql-key [order coll]
  (SqlKey. sql-key coll order))


(defn new-childs-key []
  (vector
    (InsertSqlKey. dml-insert-key 0)
    (UpdateSqlKey. dml-update-key 1)
    (DeleteSqlKey. dml-delete-key 2)
    (SelectSqlKey. dml-select-key 3)
    (CallSqlKey. dml-call-key 4)))



(defn- not-blank? [^String v]
  (not (clojure.string/blank? v)))


(extend-protocol INodeCompiler
  SqlKey
  (-schema [this]
    {(s/required-key (:cname this))
     (s/both s/Str (s/pred not-blank? 'not-blank?))})
  (-compiler-validate [this v] (s/validate (-schema this) v))
  (-compiler-emit [_ w]
    (sql-compiler-emit w)))


(defn batch-process [childs m]
  (let [p (-> (group-by-node-name childs)
              (get (dml-key m)))]
    (-process p m)))


(extend-protocol INodeProcessor
  SqlKey
  (-lorder [this] (:corder this))
  (-process-type [_] :input)
  (-process [this m]
    (batch-process (:ccoll this) m))
  InsertSqlKey
  (-lorder [this] (:corder this))
  (-process-type [_] :input)
  (-process? [_ m] (= dml-insert-key (dml-key m)))
  (-process [_ m]
    (do-insert-proc m))
  UpdateSqlKey
  (-lorder [this] (:corder this))
  (-process-type [_] :input)
  (-process? [_ m] (= dml-update-key (dml-key m)))
  (-process [_ m]
    (do-default-proc m))
  SelectSqlKey
  (-lorder [this] (:corder this))
  (-process-type [_] :input)
  (-process? [_ m] (do
                     (= dml-select-key (dml-key m))))
  (-process [_ m]
    (do-default-proc m))
  DeleteSqlKey
  (-lorder [this] (:corder this))
  (-process-type [_] :input)
  (-process? [_ m] (= dml-delete-key (dml-key m)))
  (-process [_ m]
    (do-default-proc m))
  CallSqlKey
  (-lorder [this] (:corder this))
  (-process-type [_] :input)
  (-process? [_ m] (= dml-call-key (dml-key m)))
  (-process [_ m]
    (do-default-proc m)))