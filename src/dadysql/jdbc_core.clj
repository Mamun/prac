;; Expose as API
;;
(ns dadysql.jdbc-core
  (:require
    [clojure.spec :as s]
    [dady.fail :as f]
    [dadysql.spec :refer :all]
    [dadysql.core :as dc]
    [dadysql.plugin.join.core :as j]
    [dadysql.plugin.params.core :as p]
    [dady.proto :as c]))




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
        (let [p (->> (c/remove-child (c/remove-child n-processor :dadysql.spec/param-spec) :dadysql.spec/param)
                     (c/as-xf-process :input)
                     (apply f/comp-xf-until))]
          (transduce p conj tm-coll))
        :sql-executor
        (-> (c/get-child n-processor :sql-executor)
            (c/node-process tm-coll))
        tm-coll))))


(defn do-validate [spec v]
  ;(println v)
  (if (sequential? v)
    (reduce (fn [acc w]
              (if (s/valid? spec w)
                (conj acc w)
                (reduced (f/fail (s/explain-data spec w))))
              ) (empty v) v)

    (if (s/valid? spec v)
      v
      (f/fail (s/explain-data spec v)))))

;(sequential? {:a 2})

(defn apply-validation! [tm-coll]
  ;(clojure.pprint/pprint tm-coll)

  (reduce (fn [acc v]
            (if-let [vali (:dadysql.spec/param-spec v)]
              (let [w (do-validate vali (input-key v))]

                (if (f/failed? w)
                  (reduced w)
                  (conj acc v)
                  )
                )


              (conj acc v)
              )
            ) [] tm-coll)
  )


(comment

  ;(s/valid? :get-dept-by-id/spec {:id 3} )

  )



(defmulti warp-input-node-process (fn [_ _ fmt] fmt))


(defmethod warp-input-node-process map-format
  [handler n-processor _]
  (fn [tm-coll params]
    (let [param-m (c/get-child n-processor :dadysql.spec/param)
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
    (let [param-m (c/get-child n-processor :dadysql.spec/param)
          input (f/try-> params
                         (p/do-param nested-map-format tm-coll param-m)
                         (j/do-disjoin (get-in tm-coll [0 :dadysql.spec/join])))]
      (if (f/failed? input)
        input
        (-> (mapv (fn [m] (assoc m input-key ((:dadysql.spec/model m) input))) tm-coll)
            (do-node-process n-processor :input)
            (apply-validation!)
            (handler input))))))


(defn do-result1
  [format m]
  (condp = format
    map-format
    (dissoc m :dadysql.spec/result)
    array-format
    (assoc m :dadysql.spec/result #{:dadysql.spec/array})
    value-format
    (-> m
        (assoc :dadysql.spec/model (:dadysql.spec/name m))
        (assoc :dadysql.spec/result #{:dadysql.spec/single :dadysql.spec/array})
        (assoc :dadysql.spec/dml-key :dadysql.spec/dml-select))
    m))



(defn assoc-result-format
  [tm-coll format]
  (mapv (fn [m] (do-result1 format m)) tm-coll))


(defn into-model-map
  [v]
  (if (f/failed? v)
    (hash-map (:dadysql.spec/model v) v)
    (hash-map (:dadysql.spec/model v)
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
  (if (and (not-empty (:dadysql.spec/join (first tm-coll)))
           (not (nil? (rest tm-coll))))
    true false))


(defn- merge-relation-param
  [root-result root params]
  (-> (:dadysql.spec/join root)
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
                     (j/do-join (:dadysql.spec/join root)))))))))




(defn do-run
  [n-processor tms {:keys [params pformat rformat] :as p}]
  (let [exec (fn [tm-coll _]
               (do-node-process tm-coll n-processor :sql-executor))
        proc (-> exec
                 (warp-input-node-process n-processor pformat)
                 (warp-output-node-process n-processor rformat))
        tm-coll (dc/select-name tms p)]
    (if (f/failed? tm-coll)
      tm-coll
      (proc tm-coll params))))

