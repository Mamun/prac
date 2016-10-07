(ns dadysql.jdbc-core
  (:require
    [clojure.spec :as s]
    [dady.fail :as f]
    [dadysql.plugin.join-impl :as ji]
    [dadysql.plugin.sql-bind-impl :as bi]
    [dadysql.plugin.common-impl :as ci]))



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



(defn validate-input-spec! [tm-coll]
  (reduce (fn [acc v]
            (if-let [vali (:dadysql.core/param-spec v)]
              (let [w (do-spec-validate vali (:dadysql.core/input v))]
                (if (f/failed? w)
                  (reduced w)
                  (conj acc v)))
              (conj acc v))
            ) [] tm-coll))


;;;;;;;;;;;;;;;;;;;;;;;;;;; Processing impl  ;;;;;;;;;;;;;;;;;;;;;;;;

(defn- coll-failed?
  [tm-coll]
  (reduce (fn [acc v]
            (if (f/failed? v)
              (reduced v)
              acc)
            ) tm-coll tm-coll))



(defn into-model-map
  [v]
  (if (f/failed? v)
    (hash-map (:dadysql.core/model v) v)
    (hash-map (:dadysql.core/model v)
              (:dadysql.core/output v))))



(defn dispatch-output-format [req-m]
  (cond
    (= :dadysql.core/op-pull (:dadysql.core/op req-m))
    (if
      (or (:dadysql.core/group req-m)
          (sequential? (:dadysql.core/name req-m)))
      :dadysql.core/format-nested-join
      :one)

    (= :dadysql.core/op-db-seq (:dadysql.core/op req-m))
    :dadysql.core/format-value

    (= :dadysql.core/op-push! (:dadysql.core/op req-m))
    (if (or (:dadysql.core/group req-m)
            (sequential? (:dadysql.core/name req-m)))
      :dadysql.core/format-map
      :one)

    :else
    :dadysql.core/format-map))


(defn format-output
  [tm-coll req-m]
  (let [format (dispatch-output-format req-m)]
    (cond
      (= :one format)
      (f/try-> tm-coll first :dadysql.core/output)
      (= :dadysql.core/format-value format)
      (do
        (f/try-> tm-coll first :dadysql.core/output (get-in [1 0])))
      :else
      (let [xf (comp (map into-model-map))]
        (into {} xf tm-coll)))))


(defn- do-output-process
  [tm-coll req-m]
  (if (or (f/failed? tm-coll)
          (= (:dadysql.core/op req-m)
             :dadysql.core/op-push!)
          (= (:dadysql.core/op req-m)
             :dadysql.core/op-db-seq))
    tm-coll
    (if-let [r (f/failed? (coll-failed? tm-coll))]
      r
      (let [xf (comp (map ci/do-column)
                     (map ci/do-result))]
        (transduce xf conj tm-coll)))))




(defmulti warp-output-node-process (fn [_ req-m] (dispatch-output-format req-m)))


(defmethod warp-output-node-process :default
  [handler req-m]
  (fn [tm-coll]
    (f/try-> tm-coll
             (handler)
             (do-output-process req-m)
             (format-output req-m))))


(defn- is-join-pull
  [tm-coll]
  (if (and (not-empty (:dadysql.core/join (first tm-coll)))
           (not (nil? (rest tm-coll))))
    true false))


(defn- merge-relation-param
  [root-result root more-tm]
  (let [w (-> (:dadysql.core/join root)
              (ji/get-source-relational-key-value root-result))]
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
  [handler _]
  (let [rf (warp-output-node-process handler :default)]
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
                     (ji/do-join (:dadysql.core/join root)))))))))



(defn run-process [tm-coll request-m]
  (let [exec (:dadysql.core/sql-exec request-m)
        tm-coll (transduce (map bi/sql-bind) conj tm-coll)
        proc (-> exec
                 (warp-output-node-process request-m))]
    (proc tm-coll)))

