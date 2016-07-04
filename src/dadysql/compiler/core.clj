(ns dadysql.compiler.core
  (:require [dadysql.constant :refer :all]
            [dadysql.compiler.spec]
            [dady.common :as cc]
            [clojure.spec :as s]
    ;[clojure.core.match :as m]
            [dadysql.compiler.core-emit :as e]
            [dady.common :as dc]
            [dadysql.constant :as c]
            [dadysql.compiler.file-reader :as fr]))


(defn default-config
  []
  {file-reload-key true
   timeout-key     1000
   name-key        global-key
   :tx-prop        [:isolation :serializable :read-only? true]})


(defn do-debug [m]
  (println m)
  m
  )


(defn reserve-regex []
  #":_.*")


(defn group-by-reserve-key
  [r-name-coll coll]
  (->> coll
       (group-by (fn [m]
                   (let [name (get-in m [name-key])]
                     (if (or (re-matches (reserve-regex) (str name))
                             (contains? r-name-coll name)
                             (= global-key name))
                       :reserve
                       :modules))))))


(defn group-by-config-key
  [coll]
  (->> coll
       (group-by #(if (= global-key (name-key %))
                   :global
                   :modules))))


(defn do-grouping [coll]
  (let [{:keys [global modules]} (group-by-config-key coll)
        f-global (or (first global) {})
        {:keys [reserve modules]} (-> (get-in f-global [reserve-name-key])
                                      (group-by-reserve-key modules))]
    (hash-map :global f-global :reserve reserve :modules modules)))



(defn do-validate! [coll]
  (let [w (s/conform :dadysql.compiler.spec/spec coll)]
    (if (= w :clojure.spec/invalid)
      (do
        (println (s/explain :dadysql.compiler.spec/spec coll))
        (throw (ex-info "Compile failed " (s/explain-data :dadysql.compiler.spec/spec coll)))))))


(defn join-to-extend-key
  "Assoc join key with model "
  [tm]
  (let [v (get-in tm [join-key])
        v (e/join-emit v)
        w (merge-with merge v (get-in tm [extend-meta-key]))]
    (-> tm
        (dissoc join-key)
        (assoc extend-meta-key w))))




(defn map-name-model-sql [m]
  ;(println m)
  (cond

    (and (keyword? (name-key m))
         (keyword? (model-key m)))

    (do
      [(-> m
           (assoc index 0)
           (update-in [sql-key] first))])

    (and (sequential? (name-key m))
         (sequential? (model-key m)))
    (do
      (mapv (fn [i s n m]
              {name-key  n
               index     i
               sql-key   s
               model-key m})
            (range)
            (get-in m [sql-key])
            (get-in m [name-key])
            (get-in m [model-key])))

    (and (sequential? (name-key m))
         (keyword? (model-key m)))

    (do
      (mapv (fn [i n s]
              {index     i
               name-key  n
               sql-key   s
               model-key (get-in m [model-key])})
            (range)
            (get-in m [name-key])
            (get-in m [sql-key])))

    (sequential? (name-key m))
    (mapv (fn [i s n]
            {name-key n
             index    i
             sql-key  s})
          (range)
          (get-in m [sql-key])
          (get-in m [name-key]))

    (keyword? (name-key m))
    [(-> m
         (assoc index 0)
         (update-in [sql-key] first))]

    :else
    (do
      (throw (ex-data (str "does not match " m))))))



(defn compiler-merge
  [old new]
  (cond (map? new) (merge old new)
        (vector? new) (into (empty old) (concat new old))
        :else (or new old)))



(defn do-merge
  [w module-m f-config]
  (let [name-v (get w name-key)
        module-extend (extend-meta-key module-m)
        config-extend (extend-meta-key f-config)

        w1 (merge-with compiler-merge
                       (get-in config-extend [name-v])
                       (get-in module-extend [name-v])
                       w)

        model-v (get w1 model-key)
        w2 (merge-with compiler-merge
                       (get-in config-extend [model-v])
                       (get-in module-extend [model-v])
                       w1)

        module-m (dissoc module-m name-key model-key sql-key extend-meta-key doc-key)
        f-config (select-keys f-config [param-key validation-key timeout-key])]
    (merge-with compiler-merge f-config module-m w2)))




(defn do-skip
  [m]
  (->> (into [] (skip-key m))
       (apply dissoc m)))


(defn assoc-default
  [m]
  (if (model-key m)
    (-> m
        (assoc dml-key (e/dml-type (sql-key m)))
        (update-in [sql-key] e/sql-str-emit)
        )
    (-> m
        (assoc dml-key (e/dml-type (sql-key m)))
        (update-in [sql-key] e/sql-str-emit)
        (assoc model-key (name-key m)))))


(defn remove-duplicate [m]
  (->> (keys m)
       (reduce (fn [acc k]
                 (condp = k
                   param-key (update-in acc [k] (fn [w] (cc/distinct-with-range 2 w)))
                   validation-key (update-in acc [k] (fn [w] (cc/distinct-with-range 2 w)))
                   acc)
                 ) m)))



(defn compile-one [m global-m]
  (let [model-m (map-name-model-sql (select-keys m [name-key model-key sql-key]))
        m (update-in m [extend-meta-key] #(apply hash-map %))]
    (reduce (fn [acc v]
              (->> (do-merge v m global-m)
                   (remove-duplicate)
                   (assoc-default)
                   ;(do-debug)
                   (do-skip)
                   (e/compiler-emit2)
                   (conj acc))
              ) [] model-m)))



(defn compile-batch [global-m m-coll]
  (reduce (fn [acc m]
            (reduce conj acc (compile-one m global-m))
            ) [] m-coll))


(defn compile-one-config [m]
  (-> m
      (join-to-extend-key)))


(defn reserve-compile [coll]
  (mapv (fn [m]
          (-> m
              (update-in [sql-key] (fn [v] (clojure.string/join ";" v))))
          ) coll))


(defn into-name-map
  [v]
  (hash-map (name-key v) v))


(defn do-compile [coll]
  (do-validate! coll)
  (let [{:keys [modules global reserve] :as w} (do-grouping coll)
        global  (join-to-extend-key global) (compile-one-config global)
        modules (compile-batch (select-keys global [extend-meta-key timeout-key]) modules)
        reserve (reserve-compile reserve)]
    (->> (concat [global] modules reserve)
         (into {} (map into-name-map)))))



(defn read-file [file-name]
  (do-compile (fr/read-file file-name)))



(comment




  (->> (fr/read-file "tie.edn.sql")
       (do-compile)
       ;(clojure.pprint/pprint)
       )

  (->> (fr/read-file "tie.edn2.sql")
       (do-compile)
       #_(s/conform :dadysql.compiler.spec/spec))


  (let [w (e/sql-emit (fr/read-file "tie.edn.sql"))
        {:keys [global module]} (s/conform :dadysql.compiler.spec/spec w)]
    (clojure.pprint/pprint module)
    (doseq [r1 module]
      (println
        (m/match r1
                 {:name  [:many _]
                  :model [:many _]} :name-many-model-many
                 {:name  [:many _]
                  :model [:one _]} :name-many-model-one
                 {:name  [:one _]
                  :model [:one _]} :name-one-model-one
                 :else nil
                 ))))



  (let [v {:name [:many [:insert-dept :update-dept :delete-dept]]}]
    (m/match v
             {:name [:many _]} :many
             {:name [:many _]} :many
             :else nil))

  )


