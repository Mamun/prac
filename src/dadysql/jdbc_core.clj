(ns dadysql.jdbc-core
  (:require
    [clojure.spec :as s]
    [dady.fail :as f]
    [dadysql.plugin.join.core :as j]
    [dadysql.plugin.params.core :as p]
    [dady.proto :as c]))



(defn do-spec-validate [spec v]
  (if (sequential? v)
    (reduce (fn [acc w]
              (if (s/valid? spec w)
                (conj acc w)
                (reduced (f/fail (s/explain-data spec w))))
              ) (empty v) v)
    (if (s/valid? spec v)
      v
      (f/fail (s/explain-data spec v)))))



(defn validate-param-spec! [tm-coll]
  (reduce (fn [acc v]
            (if-let [vali (:dadysql.core/param-spec v)]
              (let [w (do-spec-validate vali (:dadysql.core/input v))]
                (if (f/failed? w)
                  (reduced w)
                  (conj acc v))
                )
              (conj acc v))
            ) [] tm-coll))



(defmulti do-param (fn [_ _ fmt] (:dadysql.core/input-format fmt)))


(defmethod do-param :dadysql.core/format-map
  [tm-coll node {:keys [params]}]
  (let [param-m (c/get-child node :dadysql.core/param)
        input (p/apply-param-proc params :dadysql.core/format-map tm-coll param-m)]
    (if (f/failed? input)
      input
      (mapv (fn [m] (assoc m :dadysql.core/input input)) tm-coll))))


(defmethod do-param :dadysql.core/format-nested
  [tm-coll node {:keys [params]}]
  (let [param-m (c/get-child node :dadysql.core/param)
        input (f/try-> params
                       (p/apply-param-proc :dadysql.core/format-nested tm-coll param-m)
                       (j/do-disjoin (get-in tm-coll [0 :dadysql.core/join])))]
    (if (f/failed? input)
      input
      (mapv (fn [m] (assoc m :dadysql.core/input ((:dadysql.core/model m) input))) tm-coll))))


;;;;;;;;;;;;;;;;;;;;;;;;;;; Processing impl  ;;;;;;;;;;;;;;;;;;;;;;;;


(defn node->xf [type n-processor]
  (condp = type
    :input
    (->> (c/remove-child n-processor :dadysql.core/param)
         (c/as-xf-process :input)
         (apply f/comp-xf-until))
    :output
    (apply comp (c/as-xf-process :output n-processor))
    :sql-executor
    (c/get-child n-processor :sql-executor)
    (throw (ex-info "Node process not found for " {:type type}))))



(defn- coll-failed?
  [tm-coll]
  (reduce (fn [acc v]
            (if (f/failed? v)
              (reduced v)
              acc)
            ) tm-coll tm-coll))



(defn- do-node-process
  [tm-coll n-processor type]
  (if (f/failed? tm-coll)
    tm-coll
    (if-let [r (f/failed? (coll-failed? tm-coll))]
      r
      (condp = type
        :output
        (transduce (node->xf :output n-processor ) conj tm-coll)
        :sql-executor
        (c/node-process (node->xf :sql-executor n-processor ) tm-coll)

        tm-coll))))





(defn into-model-map
  [v]
  (if (f/failed? v)
    (hash-map (:dadysql.core/model v) v)
    (hash-map (:dadysql.core/model v)
              (:dadysql.core/output v))))


(defn format-output
  [tm-coll format]

  (cond
    (= :one format)
    (f/try-> tm-coll first :dadysql.core/output)
    (= :dadysql.core/format-value format)
    (do
      (f/try-> tm-coll first :dadysql.core/output (get-in [1 0])))
    :else
    (let [xf (comp (map into-model-map))]
      (into {} xf tm-coll))))




(defmulti warp-output-node-process (fn [_ _ format] format))


(defmethod warp-output-node-process :default
  [handler n-processor format]
  (fn [tm-coll]
    (f/try-> tm-coll
             (handler)
             (do-node-process n-processor :output)
             (format-output format))))


(defn- is-join-pull
  [tm-coll]
  (if (and (not-empty (:dadysql.core/join (first tm-coll)))
           (not (nil? (rest tm-coll))))
    true false))


(defn- merge-relation-param
  [root-result root more-tm]
  (let [w (-> (:dadysql.core/join root)
              (j/get-source-relational-key-value root-result))]
    (mapv (fn [r]
            (update-in r [:dadysql.core/input] merge w)
            ) more-tm)))



(defn- not-continue?
  [root-result]
  (if (or (f/failed? root-result)
          (nil? (first (vals root-result)))
          (empty? (first (vals root-result))))
    true false))



(defmethod warp-output-node-process :dadysql.core/format-nested-join
  [handler n-processor _]
  (let [rf (warp-output-node-process handler n-processor :default)]
    (fn [tm-coll]
      (if (not (is-join-pull tm-coll))
        (rf tm-coll)
        (let [[root & more-tm] tm-coll
              root-output (rf [root])]
          (if (not-continue? root-output)
            root-output
            (f/try-> root-output
                     (merge-relation-param root more-tm)
                     (rf)
                     (merge root-output)
                     (j/do-join (:dadysql.core/join root)))))))))



(defn warp-node-process
  [handler steps]
  (fn [tm-coll]
    (-> (transduce steps conj tm-coll)
        (handler))))



(defn get-process [n-processor request-m]
  ;(println "Request format" request-m)
  (let [rformat (:dadysql.core/output-format request-m)
        input-steps (node->xf :input n-processor)
        exec (fn [tm-coll]
               (do-node-process tm-coll n-processor :sql-executor))]
    (-> exec
        (warp-node-process input-steps)
        (warp-output-node-process n-processor rformat))))

