(ns dadysql.selector
  (:require
    [dadysql.clj.common :as cc]
    [dadysql.clj.fail :as f]
    [clojure.set]))


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
                 (if (= rel :spec-model.core/rel-n-n)
                   (some #{(first nr)} model-coll)
                   (some #{d-table} model-coll))))
       (into [])))


(defn filter-join-key
  [coll]
  (let [model-key-coll (mapv :dadysql.core/model coll)
        p (comp
            (cc/xf-skip-type #(= :dadysql.core/dml-call (:dadysql.core/dml %)))
            (map #(update-in % [:spec-model.core/join] filter-join-key-coll model-key-coll)))]
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
      (vals tm-map)
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


(defn do-result1
  [m req-m]
  (condp = (:dadysql.core/op req-m)
    :dadysql.core/op-db-seq
    (-> m
        (assoc :dadysql.core/model (:dadysql.core/name m))
        (assoc :dadysql.core/result #{:dadysql.core/result-single :dadysql.core/result-array})
        (assoc :dadysql.core/dml :dadysql.core/dml-select))
    m))


(defn init-db-seq-op
  [tm-coll req-m]
  (mapv (fn [m] (do-result1 m req-m)) tm-coll))



