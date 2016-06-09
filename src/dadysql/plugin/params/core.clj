(ns dadysql.plugin.params.core
  (:use [dady.proto])
  (:require [dadysql.constant :refer :all]
            [dady.common :as cc]
            [dady.fail :as f]
            [dadysql.core-util :as ccu]
            [clojure.spec :as sp]
            [schema.core :as s]))


(defbranch ParamKey [cname ccoll corder])
(defleaf ParamRefConKey [cname corder])
(defleaf ParamRefKey [cname corder])
(defleaf ParamRefFunKey [cname corder])
(defleaf ParamRefGenKey [cname corder generator])

;(extend-as-branch ParamKey :ccoll)


(defn new-param-key
  [order childs]
  (ParamKey. param-key childs order))


(defn temp-generator [_ _]
  -1)


(defn new-child-keys
  []
  (vector (ParamRefConKey. param-ref-con-key 0)
          (ParamRefKey. param-ref-key 1)
          (ParamRefFunKey. param-ref-fn-key 2)
          (ParamRefGenKey. param-ref-gen-key 3 temp-generator)))

(defn get-child-spec [coll-node]
  (reduce (fn [acc node]
            (->> acc
                 (cons (spec node))
                 (cons (node-name node)))
            ) (list) coll-node))


(defn do-valid? [spec v]
  (if (clojure.spec/valid? (eval spec) v)
    true
    (do
      (clojure.pprint/pprint spec)
      (clojure.pprint/pprint v)
      (throw (Exception. (clojure.spec/explain-str (eval spec) v))))))


(defn validate-spec-batch
  [node-coll v-coll]
  (let [sp (->> (get-child-spec node-coll)
                (cons 'clojure.spec/alt)
                (list)
                (cons 'clojure.spec/*))]
    (do-valid? sp v-coll)))


(defn assoc-param-ref-gen [root-node generator]
  (let [p [param-key param-ref-gen-key]
        p-index (node-path root-node p)]
    (if p-index
      (assoc-in root-node (conj p-index :generator) generator)
      root-node)))



(extend-protocol INodeCompiler
  ParamKey
  (-spec [this]
    (let [params-pred? (s/pred (partial validate-spec-batch (:ccoll this))
                               'k-spec-spec-valid?)]
      {(s/optional-key (-node-name this)) params-pred?}))
  (-spec-valid? [this v]
    (s/validate (-spec this) v))
  (-compiler-emit [this w]
    (let [child-g (group-by #(-node-name %) (:ccoll this))]
      (mapv #(-compiler-emit (get-in child-g [(second %) 0]) %) w)))
  ParamRefGenKey
  (-spec [_]
    '(clojure.spec/tuple keyword? keyword? keyword?))
  (-spec-valid? [this v]
    (do-valid? (-spec this) v))
  (-compiler-emit [_ w]
    (update-in w [0] cc/as-lower-case-keyword))
  ParamRefFunKey
  (-spec [_]
    '(clojure.spec/tuple keyword? keyword? resolve keyword?))
  (-spec-valid? [this v]
    (do-valid? (-spec this) v))
  (-compiler-emit [_ w]
    (-> w
        (assoc 2 (resolve (nth w 2)))
        (update-in [0] cc/as-lower-case-keyword)
        (update-in [3] cc/as-lower-case-keyword)))
  ParamRefKey
  (-spec [_]
    '(clojure.spec/tuple keyword? keyword? keyword?))
  (-spec-valid? [this v]
    (do-valid? (-spec this) v))
  (-compiler-emit [_ w]
    (-> w
        (update-in [0] cc/as-lower-case-keyword)
        (update-in [2] cc/as-lower-case-keyword)))
  ParamRefConKey
  (-spec [_]
    '(clojure.spec/tuple keyword? keyword? number?))
  (-spec-valid? [this v]
    (do-valid? (-spec this) v))
  (-compiler-emit [_ w]
    (update-in w [0] cc/as-lower-case-keyword)))


#_[(s/one s/Keyword "Source Data Model")
   (s/one s/Keyword "Type of params ")
   (s/one s/Keyword "Refer type ")]

#_[(s/one s/Keyword "Source Data Model")
   (s/one s/Keyword "Type of params ")
   (s/one (s/pred resolve 'resolve-clj) "Any value")
   (s/one s/Keyword "Refer Keyword ")]

#_[(s/one s/Keyword "Source Data Model")
   (s/one s/Keyword "Type of Params ")
   (s/one s/Keyword "Refer keyword")]

#_[(s/one s/Keyword "Source Data Model")
   (s/one s/Keyword "Type of Param ")
   (s/one s/Any "Any value")]


(defn process-batch
  [child-coll input ks-coll]
  (let [pm (group-by-node-name child-coll)]
    (->> ks-coll
         (sort-by (fn [[_ n]]
                    (-porder (n pm))))
         (reduce (fn [acc-input ks]
                   (let [[src n] ks
                         p (partial -pprocess (n pm))
                         rv (f/try! p ks acc-input)]
                     (if (f/failed? rv)
                       (reduced rv)
                       (assoc-in acc-input src rv)))
                   ) input))))


;(instance? Object )
;(clojure.spec/valid?  instance? 0)



(extend-protocol IParamNodeProcessor
  ParamKey
  (-pprocess-type [_] :input)
  (-porder [this] (:lorder this))
  (-pprocess? [this m] true)
  (-pprocess [this p-value m]
    (process-batch (:ccoll this) m p-value))
  ParamRefConKey
  (-porder [this] (:lorder this))
  (-pprocess? [_ m]
    (->> (group-by second (param-key m))
         (param-ref-con-key)))
  (-pprocess [_ p-value m]
    (let [[_ _ v] p-value]
      v))
  ParamRefKey
  (-porder [this] (:lorder this))
  (-pprocess? [_ m]
    (->> (group-by second (param-key m))
         (param-ref-key)))
  (-pprocess [_ p-value m]
    (let [[s _ k] p-value]
      (->> (cc/replace-last-in-vector s k)
           (get-in m))))
  ParamRefFunKey
  (-porder [this] (:lorder this))
  (-pprocess? [_ m]
    (->> (group-by second (param-key m))
         (param-ref-fn-key)))
  (-pprocess [_ p-value m]
    (let [[s _ f k] p-value]
      (->> (cc/replace-last-in-vector s k)
           (get-in m)
           (f))))
  ParamRefGenKey
  (-porder [this] (:lorder this))
  (-pprocess? [_ m]
    (->> (group-by second (param-key m))
         (param-ref-gen-key)))
  (-pprocess [this p-value _]
    (let [[_ _ v] p-value]
      ((:generator this) :name v))))

;:name v :params {} :rformat :as-sequence

(defn assoc-param-path
  [data root-path param-coll]
  (for [p param-coll
        mp (ccu/get-path data root-path (first p))
        :when (not (get-in data mp))]
    (assoc p 0 mp)))


(defmulti param-paths (fn [input-format _ _] input-format))


(defmethod param-paths
  nested-map-format
  [_ [root-m & child-m] param-m]
  (let [model-name (get root-m model-key)
        rp (ccu/get-path param-m model-name)
        rpp (assoc-param-path param-m rp (param-key root-m))
        cpp (for [c child-m
                  :let [crp (ccu/get-path param-m rp (model-key c))]
                  p (assoc-param-path param-m crp (param-key c))]
              p)]
    (-> []
        (into rpp)
        (into cpp))))


(defmethod param-paths
  map-format
  [_ tm-coll param-m]
  (->> (map param-key tm-coll)
       (reduce concat)
       (cc/distinct-with-range 1)
       (assoc-param-path param-m (ccu/empty-path))))


(defn do-param
  [rinput input-format tm-coll param-imp]
  (let [r (param-paths input-format tm-coll rinput)]
    (-pprocess param-imp r rinput)))
