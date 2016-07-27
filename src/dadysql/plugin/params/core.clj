(ns dadysql.plugin.params.core
  (:use [dady.proto])
  (:require [dadysql.spec :refer :all]
            [dady.common :as cc]
            [dady.fail :as f]
            [dadysql.plugin.util :as ccu]
    #_[clojure.spec :as sp]
    #_[schema.core :as s]))


(defbranch ParamKey [cname ccoll corder])
(defleaf ParamRefConKey [cname corder])
(defleaf ParamRefKey [cname corder])
(defleaf ParamRefFunKey [cname corder])
(defleaf ParamRefGenKey [cname corder generator])

;(extend-as-branch ParamKey :ccoll)


(defn new-param-key
  [order childs]
  (ParamKey. :dadysql.spec/param childs order))


(defn temp-generator [_ _]
  -1)


(defn new-child-keys
  []
  (vector (ParamRefConKey. :dadysql.spec/ref-con 0)
          (ParamRefKey. :dadysql.spec/ref-key 1)
          (ParamRefFunKey. :dadysql.spec/ref-fn-key 2)
          (ParamRefGenKey. :dadysql.spec/ref-gen 3 temp-generator)))


#_(defn get-child-spec [coll-node]
    (->> coll-node
         (reduce (fn [acc node]
                   (->> acc
                        (cons (spec node))
                        (cons (node-name node)))
                   ) (list))
         (cons 'clojure.spec/alt)
         (list)
         (cons 'clojure.spec/*)))



(defn assoc-param-ref-gen [root-node generator]


  (let [p [:dadysql.spec/param :dadysql.spec/ref-gen]
        p-index (node-path root-node p)]
    ; (println "p-index" p-index)
    ;    (clojure.pprint/pprint root-node)
    (if p-index
      (assoc-in root-node (conj p-index :generator) generator)
      root-node)))


#_(defn debug [m]
    (println "--deug ")
    (clojure.pprint/pprint m)
    (println "debgi finished")
    m
    )

(defn process-batch
  [child-coll input ks-coll]
  (let [pm (group-by-node-name child-coll)]
    ; (clojure.pprint/pprint pm)
    ; (clojure.pprint/pprint ks-coll)
    (->> ks-coll
         ;     (debug)
         (sort-by (fn [[_ n]]
                    ;(println n)
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
    (->> (group-by second (:dadysql.spec/param m))
         (:dadysql.spec/ref-con)))
  (-pprocess [_ p-value m]
    (let [[_ _ v] p-value]
      v))
  ParamRefKey
  (-porder [this] (:lorder this))
  (-pprocess? [_ m]
    (->> (group-by second (:dadysql.spec/param m))
         (:dadysql.spec/ref-key)))
  (-pprocess [_ p-value m]
    (let [[s _ k] p-value]
      (->> (cc/replace-last-in-vector s k)
           (get-in m))))
  ParamRefFunKey
  (-porder [this] (:lorder this))
  (-pprocess? [_ m]
    (->> (group-by second (:dadysql.spec/param m))
         (:dadysql.spec/ref-fn-key)))
  (-pprocess [_ p-value m]
    (let [[s _ f k] p-value]
      (->> (cc/replace-last-in-vector s k)
           (get-in m)
           (f))))
  ParamRefGenKey
  (-porder [this] (:lorder this))
  (-pprocess? [_ m]
    (->> (group-by second (:dadysql.spec/param m))
         (:dadysql.spec/ref-gen)))
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
  (let [model-name (get root-m :dadysql.spec/model)
        rp (ccu/get-path param-m model-name)
        rpp (assoc-param-path param-m rp (:dadysql.spec/param root-m))
        cpp (for [c child-m
                  :let [crp (ccu/get-path param-m rp (:dadysql.spec/model c))]
                  p (assoc-param-path param-m crp (:dadysql.spec/param c))]
              p)]
    (-> []
        (into rpp)
        (into cpp))))


(defmethod param-paths
  map-format
  [_ tm-coll param-m]
  (->> (map :dadysql.spec/param tm-coll)
       (reduce concat)
       (cc/distinct-with-range 1)
       (assoc-param-path param-m (ccu/empty-path))))


(defn do-param
  [rinput input-format tm-coll param-imp]
  (let [r (param-paths input-format tm-coll rinput)]
    (-pprocess param-imp r rinput)))
