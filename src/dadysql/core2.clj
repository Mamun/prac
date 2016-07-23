;; Expose as API
;;
(ns dadysql.core2
  (:require
    [clojure.spec :as s]
    [dady.common :as cc]
    [dady.fail :as f]
    [dadysql.core :refer :all]
    [dadysql.core-util :as cu]
    [dadysql.plugin.join.core :as j]
    [dadysql.plugin.params.core :as p]
    [dady.proto :as c]))



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
      (f/fail " Source is empty ")
      (f/failed? tms)
      tms
      (or (nil? tm-map)
          (empty? tm-map))
      (f/fail (format " %s Name not found" (str name-key-coll)))
      (cu/is-reserve? tms name-key-coll)
      tm-map
      :else
      (f/try-> tm-map
                (cu/validate-name! name-key-coll)
                (cc/select-values name-key-coll)
                (cu/validate-model!)
                (cu/filter-join-key)))))



(defn select-name-for-groups
  [tms gname name-coll]
  (let [name-set (into #{} (cc/as-sequential name-coll))
        p (if name-coll
            (comp (filter #(= (:dadysql.core/group %) gname))
                  (filter #(contains? name-set (:dadysql.core/name %))))
            (comp (filter #(= (:dadysql.core/group %) gname))))
        t (into [] p (vals tms))
        w (sort-by :dadysql.core/index t)]
    (into [] (map :dadysql.core/name) w)))


;;;;;;;;;;;;;;;;;;;;;;;;;;; Processing impl  ;;;;;;;;;;;;;;;;;;;;;;;;

(defn- coll-failed?
  [tm-coll]
  (reduce (fn [acc v]
            (if (f/failed? v)
              (reduced v)
              acc)
            ) tm-coll tm-coll))


(defn do-node-process
  [tm-coll n-processor type]
  (if (f/failed? tm-coll)
    tm-coll
    (if-let [r (f/failed? (coll-failed? tm-coll))]
      r
      (condp = type
        :output
        (-> (apply comp (c/as-xf-process :output n-processor))
            (transduce conj tm-coll))
        :input
        (let [p (->> (c/remove-child (c/remove-child n-processor :dadysql.core/param-spec ) :dadysql.core/param)
                     (c/as-xf-process :input)
                     (apply f/comp-xf-until))]
          (transduce p conj tm-coll))
        :sql-executor
        (-> (c/get-child n-processor :sql-executor)
            (c/node-process tm-coll))
        tm-coll))))


(defn apply-validation! [tm-coll]
  (reduce (fn [acc v]
            (if-let [vali (:dadysql.core/param-spec v)]
              (if (coll? (input-key v))

                (conj acc v)
                (if (s/valid? vali (input-key v))
                  (conj acc v)
                  (reduced (f/fail (s/explain-data vali (input-key v))))
                  ;(println "apply validation is called" vali (input-key v))
                  ))
              (conj acc v)
              )
            ) (empty tm-coll) tm-coll )
  )


(defmulti warp-input-node-process (fn [_ _ fmt] fmt))


(defmethod warp-input-node-process map-format
  [handler n-processor _]
  (fn [tm-coll params]
    (let [param-m (c/get-child n-processor :dadysql.core/param)
          input (p/do-param params map-format tm-coll param-m)]
      (if (f/failed? input)
        input
        (-> (mapv (fn [m] (assoc m input-key input)) tm-coll)
            (do-node-process n-processor :input)
            (apply-validation!)
            (handler input))))))


(defmethod warp-input-node-process nested-map-format
  [handler n-processor _]
  (fn [tm-coll params]
    (let [param-m (c/get-child n-processor :dadysql.core/param)
          input (f/try-> params
                          (p/do-param nested-map-format tm-coll param-m)
                          (j/do-disjoin (get-in tm-coll [0 :dadysql.core/join ])))]
      (if (f/failed? input)
        input
        (-> (mapv (fn [m] (assoc m input-key ((:dadysql.core/model m) input))) tm-coll)
            (do-node-process n-processor :input)
            (apply-validation!)
            (handler input))))))


(defn do-result1
  [format m]
  (condp = format
    map-format
    (dissoc m :dadysql.core/result)
    array-format
    (assoc m :dadysql.core/result #{result-array-key})
    value-format
    (-> m
        (assoc :dadysql.core/model (:dadysql.core/name m))
        (assoc :dadysql.core/result #{result-single-key result-array-key})
        (assoc :dadysql.core/dml-key dml-select-key))
    m))


(defn assoc-result-format
  [tm-coll format]
  (mapv (fn [m] (do-result1 format m)) tm-coll))


(defn into-model-map
  [v]
  (if (f/failed? v)
    (hash-map (:dadysql.core/model v) v)
    (hash-map (:dadysql.core/model v)
              (output-key v))))


#_(defn into-map
  [tm-coll]
  (into {} tm-coll))


(defn format-output
  [tm-coll format]
  (cond
    (= :one format)
    (f/try-> tm-coll first output-key)
    (= value-format format)
    (f/try-> tm-coll first output-key (get-in [1 0]))
    :else
    (let [xf (comp (map into-model-map))]
      (into {} xf tm-coll))))




(defmulti warp-output-node-process (fn [_ _ format] format))


(defmethod warp-output-node-process :default
  [handler n-processor format]
  (fn [tm-coll params]
    (f/try-> tm-coll
              (assoc-result-format format)
              (handler params)
              (do-node-process n-processor :output)
              (format-output format))))


(defn- is-join-pull
  [tm-coll]
  (if (and (not-empty (:dadysql.core/join (first tm-coll)))
           (not (nil? (rest tm-coll))))
    true false))


(defn- merge-relation-param
  [root-result root params]
  (-> (:dadysql.core/join root)
      (j/get-source-relational-key-value root-result)
      (merge params)))


(defn- not-continue?
  [root-result]
  (if (or (f/failed? root-result)
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
            (f/try-> root-output
                      (merge-relation-param root params)
                      (r-handler)
                      (merge root-output)
                      (j/do-join (:dadysql.core/join root)))))))))




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


    (if (f/failed? tm-coll)
      tm-coll
      (proc tm-coll params))))

