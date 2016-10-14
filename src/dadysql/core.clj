(ns dadysql.core
  (:require
    [clojure.spec :as s]
    [dady.fail :as f]
    [dadysql.selector :as dc]
    [dadysql.plugin.join-impl :as ji]
    [dadysql.plugin.sql-bind-impl :as bi]
    [dadysql.selector :as dc]
    [dadysql.plugin.param-impl :as pi]
    [dadysql.plugin.common-impl :as ci]))



(s/def :dadysql.core/exec-total-time int?)
(s/def :dadysql.core/exec-start-time int?)
(s/def :dadysql.core/query-exception string?)


(s/def :dadysql.core/output any?)

(s/def :dadysql.core/param map?)

(s/def :dadysql.core/format-nested any?)
(s/def :dadysql.core/format-nested-array any?)
(s/def :dadysql.core/format-nested-join any?)

(s/def :dadysql.core/format-map any?)
(s/def :dadysql.core/format-array any?)
(s/def :dadysql.core/format-value any?)

(s/def :dadysql.core/output-format #{:dadysql.core/format-nested :dadysql.core/format-nested-array :dadysql.core/format-nested-join
                                     :dadysql.core/format-map :dadysql.core/format-array :dadysql.core/format-value})
(s/def :dadysql.core/param-format #{:dadysql.core/format-nested :dadysql.core/format-map})


(s/def :dadysql.core/user-input (s/keys :req [(or :dadysql.core/name :dadysql.core/group)]
                                        :opt [:dadysql.core/param :dadysql.core/param-format :dadysql.core/output-format]))



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
              (let [w (do-spec-validate vali (:dadysql.core/param v))]
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
        pull-fn (:dadysql.core/pull req-m)]
    (f/try-> tms
             (dc/select-name req-m)
             (dc/init-db-seq-op req-m)
             (pi/bind-input req-m pull-fn)
             (validate-param-spec!)
             (handler))))
