(ns dadysql.plugin.validation.core
  (:use [dady.proto])
  (:require [dadysql.constant :refer :all]
            [dady.common :as cc]
            [dady.fail :as f]
            [clojure.spec :as sp]
            [schema.core :as s]))


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


(defn get-child-spec [coll-node]
  (->> coll-node
       (reduce (fn [acc node]
                 (->> acc
                      (cons (spec node))
                      (cons (node-name node)))
                 ) (list))
       (cons 'clojure.spec/alt)
       (list)
       (cons 'clojure.spec/*)))



(defn do-valid? [spec v]
  (if (clojure.spec/valid? (eval spec) v)
    true
    (do
      (clojure.pprint/pprint spec)
      (clojure.pprint/pprint v)
      (throw (Exception. (clojure.spec/explain-str (eval spec) v))))))


(defn validate-spec-batch
  [node-coll v-coll]
  (do-valid? (get-child-spec node-coll) v-coll))



(extend-protocol INodeCompiler
  ValidationKey
  (-spec [this]
    (let [params-pred? (s/pred (partial validate-spec-batch (:ccoll this))
                               'k-spec-spec-valid?)]
      {(s/optional-key (-node-name this)) params-pred?}))
  (-emit [this w]
    (let [child-g (group-by #(-node-name %) (:ccoll this))]
      (mapv #(-emit (get-in child-g [(second %) 0]) %) w)))
  ValidationTypeKey
  (-spec [_] '(clojure.spec/tuple keyword? keyword? resolve string?))
  (-emit [_ ks]
    (-> ks
        (assoc 2 (resolve (nth ks 2)))
        (update-in [0] cc/as-lower-case-keyword)))
  ValidationContaionKey
  (-spec [_] '(clojure.spec/tuple keyword? keyword? resolve string?))
  (-emit [_ ks]
    (-> ks
        (assoc 2 (resolve (nth ks 2)))
        (update-in [0] cc/as-lower-case-keyword)))
  ValidationRangeKey
  (-spec [_] '(clojure.spec/tuple keyword? keyword? integer? integer? string?))
  (-emit [_ ks]
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
      (if (sp/valid? v-type p-value)
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
          r (mapv #(sp/valid? v-type %) p-value)
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
    (get-child-spec child)
    #_(->
        (map->ValidationKey {:cname  validation-key
                             :corder 1
                             :ccoll  child})
        (-process w))
    )

  )



(comment

  (spec (ValidationRangeKey. validation-range-key 2))

  (alt-map {:hello (spec (ValidationRangeKey. validation-range-key 2))
            :hell2 (spec (ValidationRangeKey. validation-range-key 2))})


  (get-child-spec (new-child-coll))
  (apply concat (seq))

  )


#_(def validation-type-key-schema [(s/one s/Keyword "Source Data Model")
                                   (s/one s/Keyword "Type of validation ")
                                   (s/one resolve-type? "Clojure or Java type")
                                   (s/one s/Str "fail message")])





#_(def validation-contain-key-schema [(s/one s/Keyword "Source Data Model")
                                      (s/one s/Keyword "Type of validation ")
                                      (s/one resolve-type? "Clojure or Java type")
                                      (s/one s/Str "fail message")])

#_(def validation-range-key-schema [(s/one s/Keyword "Source Data Model")
                                    (s/one s/Keyword "Type of validation ")
                                    (s/one s/Int "Min range value")
                                    (s/one s/Int "Max range value")
                                    (s/one s/Str "fail message")])


