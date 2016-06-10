(ns dadysql.compiler.core
  (:require [dadysql.constant :refer :all]
            [dady.common :as cc]
            [dadysql.compiler.schema :as cs]
            [dady.proto :as p]))

;; Need to split it with name and model

(defn compiler-merge
  [old new]
  (cond (map? new) (merge old new)
        (vector? new) (into (empty old) (concat new old))
        :else (or new old)))


(defn default-config
  []
  {file-reload-key true
   timeout-key     1000
   name-key        global-key
   :tx-prop        [:isolation :serializable :read-only? true]})


(defn reserve-regex []
  #":_.*")


(defn assoc-join-with-recursive-meta-key
  "Assoc join key with model "
  [tm]
  (let [v (get-in tm [join-key])
        w (merge-with merge v (get-in tm [extend-meta-key]))]
    (-> tm
        (dissoc join-key)
        (assoc extend-meta-key w))))


(defn count-sql-and-name!
  [m]
  (let [sqls (sql-key m)
        name-coll (name-key m)
        t-sqls (count sqls)
        t-iden (count name-coll)]
    (when-not (= t-sqls t-iden)
      (if (> t-sqls t-iden)
        (throw (Exception. (format "Name not found for \" %s \" " (str sqls))))
        (throw (Exception. (format "Sql statement not found for \" %s \" " (str name-coll))))))
    m))


(defn distinct-name!
  [m-coll]
  (let [i-coll (->> m-coll
                    (map (juxt name-key))
                    (flatten))]
    (if-not (apply distinct? i-coll)
      (let [w (->> (frequencies i-coll)
                   (filter (fn [[_ v]]
                             (if (< 1 v) true false)))
                   (into {}))]
        (throw (Exception. (str "Found duplicate name " w)))))))


(defn group-by-config-key
  [coll]
  (->> coll
       (group-by #(if (= global-key (name-key %))
                   :config
                   :others))))


(defn group-by-reserve-key
  [r-name-coll coll]
  (->> coll
       (group-by (fn [m]
                   (let [name (name-key m)]
                     (if (or (re-matches (reserve-regex) (str name))
                             (contains? r-name-coll name)
                             (= global-key name))
                       :reserve
                       :others))))))


(def skip-key-for-call [join-key validation-key param-key])
(def skip-key-for-others [result-key column-key])


(defn do-filter-for-dml-type
  [m]
  (condp = (dml-key m)
    dml-select-key m
    dml-call-key (apply dissoc m skip-key-for-call)
    (apply dissoc m skip-key-for-others)))


(defn do-filter-for-skip
  [m]
  (->> (into [] (skip-key m))
       (apply dissoc m)))


(defn assoc-fnil-model
  [m]
  (if (model-key m)
    m
    (assoc m model-key (name-key m))))


(defn as-map
  [sql-name sql-model-name sql-m]
  (let [w (if-not sql-model-name sql-m (assoc sql-m model-key sql-model-name))]
    (assoc w name-key sql-name)))


;(conj [1 2 3] 4)
(defn combine-key
  [f-config m]
  (fn [[sql-name sql-model-name sql-m]]
    (let [w (as-map sql-name sql-model-name sql-m)
          p-coll [f-config m]

          w1 (mapv #(get-in % [extend-meta-key sql-name]) p-coll)
          w1 (apply merge-with compiler-merge (conj w1 w))

          model-k (get w1 model-key)
          w2 (mapv #(get-in % [extend-meta-key model-k]) p-coll)
          w2 (apply merge-with compiler-merge (conj w2 w1))

          m (dissoc m extend-meta-key)
          f-config (dissoc f-config extend-meta-key)]
      (merge-with compiler-merge f-config m w2))))



(defn map-name-model-sql-key
  [m]
  (let [model-v (model-key m)
        sql-model-seq (cond
                        (sequential? model-v)
                        model-v
                        (or
                          (keyword? model-v)
                          (string? model-v))
                        (repeat model-v)
                        :else
                        (repeat nil))
        sql-coll-m (sql-key m)
        sql-name-seq (name-key m)]
    (map vector sql-name-seq sql-model-seq sql-coll-m)))


(defn remove-duplicate [m]
  (->> (keys m)
       (reduce (fn [acc k]
                 (condp = k
                   param-key (update-in acc [k] (fn [w] (cc/distinct-with-range 2 w)))
                   validation-key (update-in acc [k] (fn [w] (cc/distinct-with-range 2 w)))
                   acc)
                 ) m)))




(defn compile-one
  [process-context f-config m]
  (cs/valid-spec (p/spec process-context ) m)
  ;(clojure.pprint/pprint (p/spec process-context ))
  ;(p/spec-valid? process-context m)
  (let [m (p/compiler-emit process-context m)
        f-config (dissoc f-config doc-key :tx-prop file-reload-key reserve-name-key name-key)
        m1 (-> m
               (assoc-join-with-recursive-meta-key)
               (dissoc doc-key sql-key name-key model-key group-key))
        assoc-group-key (fn [w] (assoc w group-key (group-key m)))]
    (->> (select-keys m [name-key model-key sql-key])
         (count-sql-and-name!)
         (map-name-model-sql-key)
         (mapv (combine-key f-config m1))
         (mapv (fn [w] (->> w
                            (do-filter-for-skip)
                            (do-filter-for-dml-type)
                            (assoc-fnil-model)
                            (assoc-group-key)
                            (remove-duplicate)))))))


(defn compile-one-config
  [config gpc]
  (if (nil? config)
    (default-config)
    (->> config
         (cs/valid-spec (p/spec gpc ) )
         (p/compiler-emit gpc)
         (merge (default-config))
         (assoc-join-with-recursive-meta-key))))

(defn into-name-map
  [v]
  (hash-map (name-key v) v))


(defn do-compile
  [coll cpc]
  (let [gpc (p/get-node-from-path cpc [global-key])
        mpc (p/get-node-from-path cpc [module-key])

        {:keys [config others]} (group-by-config-key coll)
        f-config (-> (first config)
                     (compile-one-config gpc))
        {:keys [reserve others]} (-> (get-in f-config [reserve-name-key])
                                     (group-by-reserve-key others))
        batch-steps (comp
                      (map #(compile-one mpc f-config %))
                      cat)
        batch-result (->> (into [] batch-steps others)
                          (concat config reserve))]
    (distinct-name! batch-result)
    (into {} (map into-name-map) batch-result)))


