(ns dadysql.plugin.validation-impl
  (:use [dady.proto])
  (:require [dadysql.constant :refer :all]
            [dady.common :as cc]
            [dady.fail :as f]
            [dadysql.plugin.base-impl :as cu]
            [clojure.spec :as sp]
            [schema.core :as s]))


;(defrecord ValidationKey [cname corder ccoll])
(defbranch ValidationKey [cname ccoll corder])
(defleaf ValidationTypeKey [cname corder])
(defleaf ValidationContaionKey [cname corder])
(defleaf ValidationRangeKey [cname corder])


(defn new-validation-key [order coll]
  (ValidationKey. validation-key coll order))

(defn new-child-coll []
  (vector (ValidationRangeKey. validation-range-key 2)
          (ValidationContaionKey. validation-contain-key 1)
          (ValidationTypeKey. validation-type-key 0)))



(defn resolve-type [w]
  (let [t (type w)]
    (if (= clojure.lang.Symbol t)
      (resolve w)
      t)))


(def resolve-type? (s/pred resolve-type 'resolve-type))


(def validation-type-key-schema [(s/one s/Keyword "Source Data Model")
                                 (s/one s/Keyword "Type of validation ")
                                 (s/one resolve-type? "Clojure or Java type")
                                 (s/one s/Str "fail message")])


(def validation-contain-key-schema [(s/one s/Keyword "Source Data Model")
                                    (s/one s/Keyword "Type of validation ")
                                    (s/one resolve-type? "Clojure or Java type")
                                    (s/one s/Str "fail message")])

(def validation-range-key-schema [(s/one s/Keyword "Source Data Model")
                                  (s/one s/Keyword "Type of validation ")
                                  (s/one s/Int "Min range value")
                                  (s/one s/Int "Max range value")
                                  (s/one s/Str "fail message")])


(defn valid? [spec v]
  (s/validate spec v))


(extend-protocol INodeCompiler
  ValidationKey
  (-spec [this]
    (let [params-pred? (s/pred (partial cu/validate-spec-batch (:ccoll this))
                               'k-spec-spec-valid?)]
      {(s/optional-key (-node-name this)) params-pred?}))
  (-spec-valid? [this v] (valid? (-spec this) v)
    #_(s/validate (-spec this) v))
  (-compiler-emit [this w]
    (let [child-g (group-by #(-node-name %) (:ccoll this))]
      (mapv #(-compiler-emit (get-in child-g [(second %) 0]) %) w)))
  ValidationTypeKey
  (-spec [_] nil)
  (-spec-valid? [this v] (valid? validation-type-key-schema v)
    #_(s/validate validation-type-key-schema v))
  (-compiler-emit [_ ks]
    (-> ks
        (assoc 2 (resolve-type (nth ks 2)))
        (update-in [0] cc/as-lower-case-keyword)))
  ValidationContaionKey
  (-spec [_] nil)
  (-spec-valid? [this v] (valid? validation-contain-key-schema v)
    #_(s/validate validation-contain-key-schema v))
  (-compiler-emit [_ ks]
    (-> ks
        (assoc 2 (resolve-type (nth ks 2)))
        (update-in [0] cc/as-lower-case-keyword)))
  ValidationRangeKey
  (-spec [_] nil)
  (-spec-valid? [this v] (valid? validation-range-key-schema v)
    #_(s/validate validation-range-key-schema v))
  (-compiler-emit [_ ks]
    (-> ks
        (update-in [0] cc/as-lower-case-keyword))))



(defn get-all-vali-key
  [m]
  (let [sql-p (into #{} (rest (sql-key m)))
        input-key (cc/as-sequential (input-key m))
        v-map (group-by first (validation-key m))]
    (for [p input-key
          p1 (keys (select-keys p sql-p))
          v1 (p1 v-map)]
      (assoc v1 0 (p1 p)))))



(defn validate-batch-process
  [child-coll m]
  (let [pm (group-by-node-name child-coll)]
    (->> (get-all-vali-key m)
         (sort-by (fn [[_ n]]
                    (node-order (n pm))))
         (reduce (fn [acc ks]
                   (let [[src n] ks
                         cp (get pm n)
                         w (node-process cp ks)]
                     (if (f/failed? w)
                       (reduced w)
                       acc))
                   ) m))))



(extend-protocol INodeProcessor
  ValidationKey
  (-lorder [this] (:corder this))
  (-process-type [_] :input)
  (-process? [_ _] true)
  (-process [this m]
    (validate-batch-process (:ccoll this) m))
  ValidationRangeKey
  (-lorder [this] (:corder this))
  (-process-type [_] :input)
  (-process? [_ m]
    (->> (group-by second (validation-key m))
         (validation-range-key)))
  (-process [_ result]
    (let [[p-value _ min max e-message] result]
      (if (and (>= p-value min)
               (<= p-value max))
        result
        (f/fail e-message))))
  ValidationTypeKey
  (-lorder [this] (:corder this))
  (-process-type [_] :input)
  (-process? [_ m]
    (->> (group-by second (validation-key m))
         (validation-type-key)))
  (-process [_ result]
    (let [[p-value _ v-type e-message] result]
      (if (= v-type (type p-value))
        result
        (f/fail {:msg   e-message
                 :value p-value
                 :type  (str (type p-value))}))))
  ValidationContaionKey
  (-lorder [this] (:corder this))
  (-process-type [_] :input)
  (-process? [_ m]
    (->> (group-by second (validation-key m))
         (validation-contain-key)))
  (-process [_ result]
    (let [[p-value _ v-type e-message] result
          r (mapv #(= v-type (type %)) p-value)
          r (every? true? r)]
      (if r
        result
        (f/fail e-message)))))



(comment

  (let [w {validation-key [[:id validation-type-key Long "error"]]
           input-key      [{:id 2}]
           sql-key        ["select * from tab " :id]}
        child (new-child-coll)]
    ;(println (get-all-vali-key w))
    (->
      (map->ValidationKey {:cname  validation-key
                           :corder 1
                           :ccoll  child})
      (-process w))
    )

  )