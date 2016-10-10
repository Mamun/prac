(ns dadysql.plugin.param-impl
  (:require [dady.common :as cc]
            [dady.fail :as f]
            [dadysql.plugin.join-impl :as ji]
            [dadysql.plugin.util :as ccu]))


(defn temp-generator [_]
  -1)


(defn assoc-param-path
  [data root-path param-coll]
  (for [p param-coll
        mp (ccu/get-path data root-path (first p))
        :when (not (get-in data mp))]
    (assoc p 0 mp)))


(defmulti param-paths (fn [input-format _ _] input-format))


(defmethod param-paths
  :dadysql.core/format-nested
  [_ [root-m & child-m] param-m]
  (let [model-name (get root-m :dadysql.core/model)
        rp (ccu/get-path param-m model-name)
        rpp (assoc-param-path param-m rp (:dadysql.core/param-coll root-m))
        cpp (for [c child-m
                  :let [crp (ccu/get-path param-m rp (:dadysql.core/model c))]
                  p (assoc-param-path param-m crp (:dadysql.core/param-coll c))]
              p)]
    (-> []
        (into rpp)
        (into cpp))))


(defmethod param-paths
  :dadysql.core/format-map
  [_ tm-coll param-m]
  (->> (map :dadysql.core/param-coll tm-coll)
       (reduce concat)
       (cc/distinct-with-range 1)
       (assoc-param-path param-m (ccu/empty-path))))



(defn do-param1 [generator path m]
  (condp = (second path)
    :dadysql.core/param-ref-con
    (let [[_ _ v] path]
      v)
    :dadysql.core/param-ref-key
    (let [[s _ k] path]
      (->> (cc/replace-last-in-vector s k)
           (get-in m)))
    :dadysql.core/param-ref-fn-key
    (let [[s _ f k] path]

      (->> (cc/replace-last-in-vector s k)
           (get-in m)
           (f)))
    :dadysql.core/param-ref-gen
    (let [[_ _ v] path]
      (generator {:dadysql.core/name v}))
    m))




(defn param-exec [tm-coll rinput input-format generator]
  (let [param-paths (param-paths input-format tm-coll rinput)]
    (reduce (fn [acc-input path]
              (let [rv (do-param1 generator path acc-input)
                    [src] path]
                (if (f/failed? rv)
                  (reduced rv)
                  (assoc-in acc-input src rv)))
              ) rinput param-paths)))


(defn disptach-input-format [req-m]
  (if (and
        (= :dadysql.core/op-push! (:dadysql.core/op req-m))
        (or (:dadysql.core/group req-m)
            (sequential? (:dadysql.core/name req-m))))
    :dadysql.core/format-nested
    :dadysql.core/format-map))


(defmulti bind-input (fn [_ request-m _] (disptach-input-format request-m)))


(defmethod bind-input :dadysql.core/format-map
  [tm-coll request-m gen]
  (let [input (:dadysql.core/param request-m)
        input (param-exec tm-coll input :dadysql.core/format-map gen)]
    (if (f/failed? input)
      input
      (mapv (fn [m] (assoc m :dadysql.core/param input)) tm-coll))))


(defmethod bind-input :dadysql.core/format-nested
  [tm-coll request-m gen]
  (let [input (:dadysql.core/param request-m)
        input (param-exec tm-coll input :dadysql.core/format-nested gen)
        input (f/try-> input
                       (ji/do-disjoin (get-in tm-coll [0 :dadysql.core/join])))]
    (if (f/failed? input)
      input
      (mapv (fn [m] (assoc m :dadysql.core/param ((:dadysql.core/model m) input))) tm-coll))))


