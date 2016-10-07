(ns dadysql.jdbc-core
  (:require
    [clojure.spec :as s]
    [dady.fail :as f]
    [dadysql.core-selector :as dc]
    [dadysql.plugin.join-impl :as ji]
    [dadysql.plugin.sql-bind-impl :as bi]
    [dadysql.core-selector :as dc]
    [dadysql.plugin.param-impl :as pi]
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



(defmulti do-output (fn [_ req-m] (:dadysql.core/op req-m)))


(defmethod do-output
  :dadysql.core/op-db-seq
  [tm-coll _]
  (f/try-> tm-coll first :dadysql.core/output (get-in [1 0])))


(defmethod do-output
  :dadysql.core/op-push!
  [tm-coll req-m]
  (if (keyword? (:dadysql.core/name req-m))
    (f/try-> tm-coll first :dadysql.core/output)
    (into {} (comp (map into-model-map)) tm-coll)))


(defmethod do-output
  :dadysql.core/op-pull
  [tm-coll req-m]
  (if (keyword? (:dadysql.core/name req-m))
    (-> (comp (map ci/do-column)
              (map ci/do-result))
        (transduce conj tm-coll)
        (first)
        :dadysql.core/output)
    (into {} (comp (map ci/do-column)
                   (map ci/do-result)
                   (map into-model-map)) tm-coll)))


(defn warp-do-output [handler req-m]
  (fn [tm-coll]
    (f/try-> tm-coll
             (handler)
             (do-output req-m))))




(defn- is-join-pull?
  [tm-coll req-m]
  (if (and
        (= :dadysql.core/op-pull (:dadysql.core/op req-m))
        (or (:dadysql.core/group req-m)
            (sequential? (:dadysql.core/name req-m)))
        (not-empty (:dadysql.core/join (first tm-coll)))
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



(defn warp-do-output-join
  [handler req-m]
  (fn [tm-coll]
    (if (not (is-join-pull? tm-coll req-m))
      (handler tm-coll)
      (let [[root & more-tm] tm-coll
            root-output (handler [root])]
        (if (not-continue? root-output)
          root-output
          (f/try-> root-output
                   (merge-relation-param root more-tm)
                   (handler)
                   (merge root-output)
                   (ji/do-join (:dadysql.core/join root))))))))

;;sql bind needs to call with in warp-do-join process
(defn warp-bind-sql [handler request-m]
  (fn [tm-coll]
    (->> tm-coll
         (transduce (map bi/sql-bind) conj)
         (handler))))


(defn do-execute [req-m tms]
  (let [handler (-> (:dadysql.core/sql-exec req-m)
                    (warp-bind-sql req-m)
                    (warp-do-output req-m)
                    (warp-do-output-join req-m))
        gen (:dadysql.core/callback req-m)]
    (f/try-> tms
             (dc/select-name req-m)
             (dc/init-db-seq-op req-m)
             (pi/bind-input req-m gen)
             (validate-input-spec!)
             (handler))))
