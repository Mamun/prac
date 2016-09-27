(ns dadysql.core-processor
  (:require
    [clojure.spec :as sp]
    [dady.common :as cc]
    [dady.fail :as f]
    [clojure.set]
    [dadysql.spec :as tc]))


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
  (let [model-coll (mapv :dadysql.spec/model tm-coll)
        m (distinct model-coll)]
    (if (not= (count model-coll)
              (count m))
      (f/fail (str "Selecting duplicate model " model-coll))
      tm-coll)))


(defn filter-join-key-coll
  [join model-coll]
  (->> join
       (filter (fn [[_ _ rel d-table _ nr]]
                 (if (= rel :dadysql.spec/many-many)
                   (some #{(first nr)} model-coll)
                   (some #{d-table} model-coll))))
       (into [])))


(defn filter-join-key
  [coll]
  (let [model-key-coll (mapv :dadysql.spec/model coll)
        p (comp
            (cc/xf-skip-type #(= :dadysql.spec/dml-call (:dadysql.spec/dml-key %)))
            (map #(update-in % [:dadysql.spec/join] filter-join-key-coll model-key-coll)))]
    (transduce p conj [] coll)))


(defn is-reserve?
  [tms coll]
  (if (->> (clojure.set/intersection
             (into #{} coll)
             (get-in tms [tc/global-key :dadysql.spec/reserve-name]))
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
            (comp (filter #(= (:dadysql.spec/group %) gname))
                  (filter #(contains? name-set (:dadysql.spec/name %))))
            (comp (filter #(= (:dadysql.spec/group %) gname))))
        t (into [] p (vals tms))
        w (sort-by :dadysql.spec/index t)]
    (into [] (map :dadysql.spec/name) w)))


(defn validate-input!
  [req-m]
  (if (sp/valid? :dadysql.spec/input req-m)
    req-m
    (f/fail (sp/explain-data :dadysql.spec/input req-m))))


(defn select-name [tms req-m]
  (let [{:keys [group name]} req-m
        name (if group
               (select-name-for-groups tms group name)
               name)
        tm-coll (select-name-by-name-coll tms name)]
    tm-coll))



(defmulti default-request (fn [t _] t))


(defmethod default-request :pull
  [_ {:keys [name group] :as request-m}]
  (let [dfmat (if (or group
                      (sequential? name))
                {:pformat tc/map-format :rformat tc/nested-join-format}
                {:pformat tc/map-format :rformat :one})
        request-m (merge dfmat request-m)
        request-m (if group
                    (assoc request-m :rformat tc/nested-join-format)
                    request-m)]
    request-m))


(defmethod default-request :push
  [_ {:keys [group name] :as request-m}]
  (let [d (if (or group
                  (sequential? name))
            {:pformat tc/nested-map-format :rformat tc/nested-map-format}
            {:pformat tc/map-format :rformat :one})
        request-m (merge d request-m)
        request-m (if group
                    (-> request-m
                        (assoc :pformat tc/nested-map-format)
                        (assoc :rformat tc/nested-map-format))
                    request-m)]
    request-m))


(defmethod default-request :db-seq
  [_ request-m]
  (-> request-m
      (assoc :pformat tc/map-format)
      (assoc :rformat tc/value-format)))



(defn do-result1
  [format m]
  (condp = format
    tc/map-format
    (dissoc m :dadysql.spec/result)
    tc/array-format
    (assoc m :dadysql.spec/result #{:dadysql.spec/array})
    tc/value-format
    (-> m
        (assoc :dadysql.spec/model (:dadysql.spec/name m))
        (assoc :dadysql.spec/result #{:dadysql.spec/single :dadysql.spec/array})
        (assoc :dadysql.spec/dml-key :dadysql.spec/dml-select))
    m))



(defn assoc-result-format
  [tm-coll format]
  (mapv (fn [m] (do-result1 format m)) tm-coll))
