(ns tiesql.plugin.param-impl
  (:use [tiesql.proto])
  (:require [tiesql.common :refer :all]
            [tiesql.common :as cc]
            [tiesql.core-util :as ccu]
            [tiesql.plugin.util :as cu]
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


(defn assoc-param-ref-gen [root-node generator]
  (let [p [param-key param-ref-gen-key]
        p-index (node-path root-node p)]
    (if p-index
      (assoc-in root-node (conj p-index :generator) generator)
      root-node)))



(extend-protocol INodeCompiler
  ParamKey
  (-schema [this]
    (let [params-pred? (s/pred (partial cu/validate-schema-batch (:ccoll this))
                               'k-schema-compiler-validate)]
      {(s/optional-key (-node-name this)) params-pred?}))
  (-compiler-validate [this v]
    (s/validate (-schema this) v))
  (-compiler-emit [this w]
    (let [child-g (group-by #(-node-name %) (:ccoll this))]
      (mapv #(-compiler-emit (get-in child-g [(second %) 0]) %) w)))
  ParamRefGenKey
  (-schema [_] [(s/one s/Keyword "Source Data Model")
                (s/one s/Keyword "Type of params ")
                (s/one s/Keyword "Refer type ")])
  (-compiler-validate [this v]
    (s/validate (-schema this) v))
  (-compiler-emit [_ w]
    (update-in w [0] cc/as-lower-case-keyword))
  ParamRefFunKey
  (-schema [_] [(s/one s/Keyword "Source Data Model")
                (s/one s/Keyword "Type of params ")
                (s/one (s/pred resolve 'resolve-clj) "Any value")
                (s/one s/Keyword "Refer Keyword ")])
  (-compiler-validate [this v]
    (s/validate (-schema this) v))
  (-compiler-emit [_ w]
    (-> w
        (assoc 2 (resolve (nth w 2)))
        (update-in [0] cc/as-lower-case-keyword)
        (update-in [3] cc/as-lower-case-keyword)))
  ParamRefKey
  (-schema [_] [(s/one s/Keyword "Source Data Model")
                (s/one s/Keyword "Type of Params ")
                (s/one s/Keyword "Refer keyword")])
  (-compiler-validate [this v]
    (s/validate (-schema this) v))
  (-compiler-emit [_ w]
    (-> w
        (update-in [0] cc/as-lower-case-keyword)
        (update-in [2] cc/as-lower-case-keyword)))
  ParamRefConKey
  (-schema [_] [(s/one s/Keyword "Source Data Model")
                (s/one s/Keyword "Type of Param ")
                (s/one s/Any "Any value")])
  (-compiler-validate [this v]
    (s/validate (-schema this) v))
  (-compiler-emit [_ w]
    (update-in w [0] cc/as-lower-case-keyword)))



(defn process-batch
  [child-coll input ks-coll]
  (let [pm (group-by-node-name child-coll)]
    (->> ks-coll
         (sort-by (fn [[_ n]]
                    (-porder (n pm))))
         (reduce (fn [acc-input ks]
                   (let [[src n] ks
                         p (partial -pprocess (n pm))
                         rv (cc/try! p ks acc-input)]
                     (if (cc/failed? rv)
                       (reduced rv)
                       (assoc-in acc-input src rv)))
                   ) input))))



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
