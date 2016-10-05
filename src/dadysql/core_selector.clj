(ns dadysql.core-selector
  (:require
    [dady.common :as cc]
    [dady.fail :as f]
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


#_(defn validate-input!
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




(defn do-result1
  [m req-m ]
  (condp = (:dadysql.core/op req-m)
    :db-seq
    (-> m
        (assoc :dadysql.core/model (:dadysql.core/name m))
        (assoc :dadysql.core/result #{:dadysql.core/single :dadysql.core/array})
        (assoc :dadysql.core/dml-key :dadysql.core/dml-select))
    m))



(defn assoc-result-format
  [tm-coll req-m]
  (mapv (fn [m] (do-result1 m req-m )) tm-coll))






(comment


  (require '[dadysql.jdbc :as t])

  #_(-> {:name   [:create-dept]
       :params {:department [{:dept_name "Software dept "}
                             {:dept_name "Hardware dept"}]}}

      (select-name-for :push (t/read-file "tie.edn.sql"))
      )



  #_(-> {:name   [:get-dept-by-ids]
       :params {:id [1 2 112]}}

      (select-name-for :pull (t/read-file "tie.edn.sql"))
      )



  (select2 (t/read-file "tie.edn.sql")
           {:name   [:create-dept]
            :params {:department [{:dept_name "Software dept "}
                                  {:dept_name "Hardware dept"}]}}
           :push)

  )





