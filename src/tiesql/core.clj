;; Expose as API
;;
(ns tiesql.core
  (:require
    [tiesql.common :as cc]
    [tiesql.common :refer :all]
    [tiesql.core-util :as cu]
    [tiesql.plugin.join-core :as j]
    [tiesql.plugin.param-impl :as p]
    [tiesql.proto :as c]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;; Selecting impl ;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn select-config
  [tms]
  (global-key tms))


(defn select-name
  "Return list module "
  [tms name-coll]
  (let [name-key-coll (cc/as-sequential name-coll)
        tm-map (select-keys tms name-key-coll)]
    (cond
      (or (nil? tms)
          (empty? tms))
      (cc/fail " Source is empty ")
      (or (nil? tm-map)
          (empty? tm-map))
      (cc/fail (format " %s Name not found" (str name-key-coll)))
      (cu/is-reserve? tms name-key-coll)
      tm-map
      :else
      (cc/try-> tm-map
                (cu/validate-name! name-key-coll)
                (cc/select-values name-key-coll)
                (cu/validate-model!)
                (cu/filter-join-key)))))



(defn select-name-for-groups
  [tms gname name-coll]
  (let [name-set (into #{} (cc/as-sequential name-coll))
        p (if name-coll
            (comp (filter #(= (group-key %) gname))
                  (filter #(contains? name-set (name-key %))))
            (comp (filter #(= (group-key %) gname))))
        t (into [] p (vals tms))
        w (sort-by index t)]
    (into [] (map name-key) w)))


;;;;;;;;;;;;;;;;;;;;;;;;;;; Processing impl  ;;;;;;;;;;;;;;;;;;;;;;;;

(defn- coll-failed?
  [tm-coll]
  (reduce (fn [acc v]
            (if (cc/failed? v)
              (reduced v)
              acc)
            ) tm-coll tm-coll))


(defn do-node-process
  [tm-coll n-processor type]
  (if (cc/failed? tm-coll)
    tm-coll
    (if-let [r (cc/failed? (coll-failed? tm-coll))]
      r
      (condp = type
        :output
        (-> (apply comp (c/as-xf-process :output n-processor))
            (transduce conj tm-coll))
        :input
        (let [p (->> (c/remove-child n-processor param-key)
                     (c/as-xf-process :input)
                     (apply cc/comp-xf-until))]
          (transduce p conj tm-coll))
        :sql-executor
        (-> (c/get-child n-processor :sql-executor)
            (c/node-process tm-coll))
        tm-coll))))



(defmulti warp-input-node-process (fn [_ _ fmt] fmt))


(defmethod warp-input-node-process map-format
  [handler n-processor _]
  (fn [tm-coll params]
    (let [param-m (c/get-child n-processor param-key)
          input (p/do-param params map-format tm-coll param-m)]
      (if (cc/failed? input)
        input
        (-> (mapv (fn [m] (assoc m input-key input)) tm-coll)
            (do-node-process n-processor :input)
            (handler input))))))


(defmethod warp-input-node-process nested-map-format
  [handler n-processor _]
  (fn [tm-coll params]
    (let [param-m (c/get-child n-processor param-key)
          input (cc/try-> params
                          (p/do-param nested-map-format tm-coll param-m)
                          (j/do-disjoin (get-in tm-coll [0 join-key])))]
      (if (cc/failed? input)
        input
        (-> (mapv (fn [m] (assoc m input-key ((model-key m) input))) tm-coll)
            (do-node-process n-processor :input)
            (handler input))))))


(defn do-result1
  [t m]
  (condp = t
    map-format
    (dissoc m result-key)
    array-format
    (assoc m result-key #{result-array-key})
    value-format
    (-> m
        (assoc model-key (name-key m))
        (assoc result-key #{result-single-key result-array-key})
        (assoc dml-key dml-select-key))
    m))


(defn assoc-result-format
  [tm-coll t]
  (mapv (fn [m] (do-result1 t m)) tm-coll))


(defn into-model-map
  [v]
  (if (cc/failed? v)
    (hash-map (model-key v) v)
    (hash-map (model-key v)
              (output-key v))))


#_(defn into-map
  [tm-coll]
  (into {} tm-coll))


(defn format-output
  [tm-coll t]
  (cond
    (contains? #{map-format array-format} t)
    (cc/try-> tm-coll first output-key)
    (= value-format t)
    (cc/try-> tm-coll first output-key (get-in [1 0]))
    :else
    (let [xf (comp (map into-model-map))]
      (into {} xf tm-coll))))




(defmulti warp-output-node-process (fn [_ _ format] format))


(defmethod warp-output-node-process :default
  [handler n-processor format]
  (fn [tm-coll params]
    (cc/try-> tm-coll
              (assoc-result-format format)
              (handler params)
              (do-node-process n-processor :output)
              (format-output format))))


(defn- is-join-pull
  [tm-coll]
  (if (and (not-empty (join-key (first tm-coll)))
           (not (nil? (rest tm-coll))))
    true false))


(defn- merge-relation-param
  [root-result root params]
  (-> (join-key root)
      (j/get-source-relational-key-value root-result)
      (merge params)))


(defn- not-continue?
  [root-result]
  (if (or (cc/failed? root-result)
          (nil? (first (vals root-result)))
          (empty? (first (vals root-result))))
    true false))



(defmethod warp-output-node-process nested-join-format
  [handler n-processor _]
  (let [rf (warp-output-node-process handler n-processor :default)]
    (fn [tm-coll params]
      (if (not (is-join-pull tm-coll))
        (rf tm-coll params)
        (let [[root & more-tm] tm-coll
              root-output (rf [root] params)
              r-handler (fn [r-out]
                          (rf more-tm r-out))]
          (if (not-continue? root-output)
            root-output
            (cc/try-> root-output
                      (merge-relation-param root params)
                      (r-handler)
                      (merge root-output)
                      (j/do-join (join-key root)))))))))




(defn do-run
  [n-processor tms {:keys [gname name params pformat rformat]}]
  (let [exec (fn [tm-coll _]
              (do-node-process tm-coll n-processor :sql-executor))
        proc (-> exec
                 (warp-input-node-process n-processor pformat)
                 (warp-output-node-process n-processor rformat))
        name (if gname
               (select-name-for-groups tms gname name)
               name)
        tm-coll (select-name tms name)
        ]
    (if (cc/failed? tm-coll)
      tm-coll
      (proc tm-coll params))))

