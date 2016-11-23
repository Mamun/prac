(ns dadysql.workflow-exec
  (:require
    [clojure.spec :as s]
    [dadysql.spec]
    [dadysql.clj.fail :as f]
    [dadymodel.core :as ji]
    [dadysql.impl.sql-bind-impl :as bi]
    [dadysql.selector :as dc]
    [dadysql.impl.param-impl :as pi]
    [dadysql.clj.common :as cc]
    [dadysql.impl.common-impl :as ci]))




(defn validate-input!
  [req-m]
  (if (s/valid? :dadysql.core/user-input req-m)
    req-m
    (f/fail (s/explain-data :dadysql.core/user-input req-m))))



(comment

  (s/valid? :dadysql.core/user-input {:dadysql.core/name [:get-employee-detail]
                                      :params            {:id 1}})


  (s/explain :dadysql.core/user-input {:dadysql.core/name         [:get-employee-detail]
                                       :group                     :load-dept
                                       :dadysql.core/param-format :map
                                       :params                    {}})

  )






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
        (not-empty (:dadymodel.core/join (first tm-coll)))
        (not (nil? (rest tm-coll))))
    true false))


(defn get-source-relational-key-value
  [j-coll data-m]
  (reduce (fn [acc j1]
            (let [[s st rel _ dt [_ sdt _]] j1
                  w (keys (cc/group-by-value st (s data-m)))]
              (if (= rel :dadymodel.core/rel-n-n)
                (merge acc {sdt w})
                (merge acc {dt w})))
            ) {} j-coll))


(defn- merge-relation-param
  [root-result root more-tm]
  (let [w (-> (:dadymodel.core/join root)
              (get-source-relational-key-value root-result))]
    (mapv (fn [r]
            (update-in r [:dadysql.core/param] merge w)
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
                   (ji/do-join (:dadymodel.core/join root))))))))


;;sql bind needs to call with in warp-do-join process
(defn warp-bind-sql [handler request-m]
  (fn [tm-coll]
    (->> tm-coll
         (transduce (map bi/sql-bind) conj)
         (handler))))



(defn input-format [req-m]
  (if (and
        (= :dadysql.core/op-push! (:dadysql.core/op req-m))
        (or (:dadysql.core/group req-m)
            (sequential? (:dadysql.core/name req-m))))
    :dadysql.core/format-nested
    :dadysql.core/format-map))



(defn process-input [req-m tm-coll & {:keys [disjoin]
                                      :or   {disjoin true}}]
  (let [pull-fn (:dadysql.core/pull req-m)
        in-format (input-format req-m)
        apply-disjoin (fn [input]
                        (if (and disjoin
                                 (= in-format :dadysql.core/format-nested))
                          (ji/do-disjoin input (get-in tm-coll [0 :dadymodel.core/join]))
                          input))]
    (f/try-> tm-coll
             (pi/validate-param-spec req-m)
             (pi/assoc-generator pull-fn)
             (pi/param-exec (:dadysql.core/param req-m) in-format)
             (ji/do-assoc-relation-key (get-in tm-coll [0 :dadymodel.core/join]))
             (apply-disjoin))))



(defn- bind-param [input tm-coll request-m]
  (if (= (input-format request-m)
         :dadysql.core/format-nested)
    (mapv (fn [m] (assoc m :dadysql.core/param ((:dadysql.core/model m) input))) tm-coll)
    (mapv (fn [m] (assoc m :dadysql.core/param input)) tm-coll)))



(defn do-execute [req-m tm-coll]
  (let [handler (-> (:dadysql.core/sql-exec req-m)
                    (warp-bind-sql req-m)
                    (warp-do-output req-m)
                    (warp-do-output-join req-m))]
    (f/try-> req-m
             (process-input tm-coll)
             (bind-param tm-coll req-m)
             (dc/init-db-seq-op req-m)
             (handler))))
