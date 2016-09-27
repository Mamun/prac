(ns dadysql.core-processor
  (:require
    [clojure.spec :as sp]
    [dady.common :as cc]
    [dady.fail :as f]
    [clojure.set]
    ))


(defn validate-name!
  [tm name-coll]
  (let [skey (into #{} (keys tm))
        op-key (into #{} name-coll)]
    (if (clojure.set/superset? skey op-key)
      tm
      (->> (clojure.set/difference op-key skey)
           (first)
           (str "Name not found ")
           (f/fail)))))


(defn validate-model!
  [tm-coll]
  (let [model-coll (mapv :dadysql.core/model tm-coll)
        m (distinct model-coll)]
    (if (not= (count model-coll)
              (count m))
      (f/fail (str "Selecting duplicate model " model-coll))
      tm-coll)))


(defn filter-join-key-coll
  [join model-coll]
  (->> join
       (filter (fn [[_ _ rel d-table _ nr]]
                 (if (= rel :dadysql.core/many-many)
                   (some #{(first nr)} model-coll)
                   (some #{d-table} model-coll))))
       (into [])))


(defn filter-join-key
  [coll]
  (let [model-key-coll (mapv :dadysql.core/model coll)
        p (comp
            (cc/xf-skip-type #(= :dadysql.core/dml-call (:dadysql.core/dml-key %)))
            (map #(update-in % [:dadysql.core/join] filter-join-key-coll model-key-coll)))]
    (transduce p conj [] coll)))


(defn is-reserve?
  [tms coll]
  (if (->> (clojure.set/intersection
             (into #{} coll)
             (get-in tms [:_global_ :dadysql.core/reserve-name]))
           (not-empty))
    true
    false))

;;;;;;;;;;;;;;;;;;;;;;;;;;;; Selecting impl ;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn select-name-by-name-coll
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
      (is-reserve? tms name-key-coll)
      tm-map
      :else
      (f/try-> tm-map
               (validate-name! name-key-coll)
               (cc/select-values name-key-coll)
               (validate-model!)
               (filter-join-key)))))


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


(defn validate-input!
  [req-m]
  (if (sp/valid? :dadysql.core/input req-m)
    req-m
    (f/fail (sp/explain-data :dadysql.core/input req-m))))


(defn select-name [tms req-m]
  (let [{:keys [group name]} req-m
        name (if group
               (select-name-for-groups tms group name)
               name)
        tm-coll (select-name-by-name-coll tms name)]
    tm-coll))



(defmulti assoc-format (fn [t _] t))


(defmethod assoc-format :pull
  [_ {:keys [name group] :as request-m}]
  (let [dfmat (if (or group
                      (sequential? name))
                {:dadysql.core/pformat :dadysql.core/format-map
                 :dadysql.core/rformat :dadysql.core/format-nested-join}
                {:dadysql.core/pformat :dadysql.core/format-map
                 :dadysql.core/rformat :one})
        request-m (merge dfmat request-m)
        request-m (if group
                    (assoc request-m :dadysql.core/rformat :dadysql.core/format-nested-join)
                    request-m)]
    request-m))


(defmethod assoc-format :push
  [_ {:keys [group name] :as request-m}]
  (let [d (if (or group
                  (sequential? name))
            {:dadysql.core/pformat :dadysql.core/format-nested
             :dadysql.core/rformat :dadysql.core/format-nested}
            {:dadysql.core/pformat :dadysql.core/format-map
             :dadysql.core/rformat :one})
        request-m (merge d request-m)
        request-m (if group
                    (-> request-m
                        (assoc :dadysql.core/pformat :dadysql.core/format-nested)
                        (assoc :dadysql.core/rformat :dadysql.core/format-nested))
                    request-m)]
    request-m))


(defmethod assoc-format :db-seq
  [_ request-m]
  (-> request-m
      (assoc :dadysql.core/pformat :dadysql.core/format-map)
      (assoc :dadysql.core/rformat :dadysql.core/format-value)))



(defn do-result1
  [format m]
  (condp = format
    :dadysql.core/format-map
    (dissoc m :dadysql.core/result)
    :dadysql.core/format-array
    (assoc m :dadysql.core/result #{:dadysql.core/array})
    :dadysql.core/format-value
    (-> m
        (assoc :dadysql.core/model (:dadysql.core/name m))
        (assoc :dadysql.core/result #{:dadysql.core/single :dadysql.core/array})
        (assoc :dadysql.core/dml-key :dadysql.core/dml-select))
    m))



(defn assoc-result-format
  [tm-coll format]
  (mapv (fn [m] (do-result1 format m)) tm-coll))
