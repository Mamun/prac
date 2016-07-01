(ns dadysql.compiler.core
  (:require [dadysql.constant :refer :all]
            [dadysql.compiler.spec]
            [dady.common :as cc]
            [clojure.spec :as s]
            [clojure.core.match :as m]

            [dadysql.compiler.core-emit :as e]
            [dady.common :as dc]
            [dadysql.constant :as c]))


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
                   (let [name (get-in m [name-key 1])]
                     (if (or (re-matches (reserve-regex) (str name))
                             (contains? r-name-coll name)
                             (= global-key name))
                       :reserve
                       :module))))))


(defn do-grouping [coll]
  (let [{:keys [global module]} coll
        {:keys [reserve module]} (group-by-reserve-key (get global reserve-name-key) module)]
    {:global  global
     :reserve reserve
     :module  module}))


(defn do-validate! [coll]
  (let [w (s/conform :dadysql.compiler.spec/spec coll)]
    (if (= w :clojure.spec/invalid)
      (do
        (println (s/explain :dadysql.compiler.spec/spec coll))
        (throw (ex-data (s/explain :dadysql.compiler.spec/spec coll))))
      w)))


(defn join-to-extend-key
  "Assoc join key with model "
  [tm]
  (let [v (get-in tm [join-key])
        v (e/join-emit v)
        w (merge-with merge v (get-in tm [extend-meta-key]))]
    (-> tm
        (dissoc join-key)
        (assoc extend-meta-key w))))





#_(defn get-format [r]
  (m/match
    r
    {:name [:many _] :model [:many _]}
    :name-many-model-many
    {:name [:many _] :model [:one _]}
    :name-many-model-one
    {:name [:one _] :model [:one _]}
    :name-one-model-one
    :else nil))


(defn map-name-model-sql [m]
  (m/match
    m

    {:name [:many _] :model [:many _]}
    (do
      (mapv (fn [s n m]
              {name-key  n
               sql-key   s
               dml-key   (e/dml-type s)
               model-key m})
            (get-in m [sql-key])
            (get-in m [name-key 1])
            (get-in m [model-key 1])))

    {:name [:many _] :model [:one _]}
    (do
      ; (println "m o" (map vector (get-in m [name-key 1]) (get-in m [sql-key]) ))
      (mapv (fn [n s]
              {name-key  n
               sql-key   s
               dml-key   (e/dml-type s)
               model-key (get-in m [model-key 1])})
            (get-in m [name-key 1])
            (get-in m [sql-key])))

    {:name [:one _] :model [:one _]}
    (do
      ;(println "o o ")
      [{name-key  (get-in m [name-key])
        model-key (get-in m [module-key])
        dml-key   (e/dml-type (get-in m [sql-key 0]))
        sql-key   (get-in m [sql-key 0])}])


    {:name [:many _]}
    (do
      (mapv (fn [n s] {name-key n
                       dml-key  (e/dml-type s)
                       sql-key  s})
            (get-in m [name-key 1])
            (get-in m [sql-key])))

    {:name [:one _]}
    [{name-key (get-in m [name-key 1])
      dml-key  (e/dml-type (get-in m [sql-key 0]))
      sql-key  (get-in m [sql-key 0])}]

    :else (throw (ex-data (str "does not match " m)))))



(defn merge-select [m1 module-m global-m]
  (let [n (name-key m1)
        m (model-key m1)]
    [(select-keys global-m [param-key validation-key timeout-key])
     (dissoc module-m name-key model-key sql-key extend-meta-key doc-key)
     (get-in global-m [extend-meta-key m])
     (get-in module-m [extend-meta-key m])
     (get-in global-m [extend-meta-key n])
     (get-in module-m [extend-meta-key n])
     m1]))



(defn compiler-merge
  [old new]
  (cond (map? new) (merge old new)
        (vector? new) (into (empty old) (concat new old))
        :else (or new old)))


(defn do-merge [m module global-m]
  (->> (merge-select m module global-m)
       ; (do-debug)
       (remove nil?)
       (into [])
       (apply merge-with compiler-merge)))



(defn do-skip
  [m]
  (->> (into [] (skip-key m))
       (apply dissoc m)))


(defn assoc-fnil-model
  [m]
  (if (model-key m)
    m
    (assoc m model-key (name-key m))))




(defn convert-v [m]
  (->>
    (map (fn [w]
           (mapv (fn [[k v]] v) w)
           ) m)
    (into {})))



(defn do-module-compile-one [m global-m]
  (let [m (dc/update-if-contains m [extend-meta-key] convert-v)
        model-m (map-name-model-sql (select-keys m [name-key model-key sql-key]))]
    (reduce (fn [acc v]
              (->> (do-merge v m global-m)
                   (do-skip)
                   (assoc-fnil-model)
                   (e/compiler-emit2)
                   (conj acc))
              ) [] model-m)))



(defn do-compile-batch [global-m m-coll]
  (reduce (fn [acc m]
            (reduce conj acc (do-module-compile-one m global-m))
            ) [] m-coll))


(defn global-compile [m]
  (-> m
      (update-in [name-key] second)
      (dc/update-if-contains m [extend-meta-key] convert-v)
      (join-to-extend-key)))


(defn reserve-compile [coll]
  (mapv (fn [m]
          (-> m
              (update-in [name-key] second)
              (update-in [sql-key] (fn [v] (clojure.string/join ";" v))))
          ) coll))


(defn into-name-map
  [v]
  (hash-map (name-key v) v))


(defn do-compile [coll]
  (let [{:keys [module global reserve]} (do-grouping (do-validate! coll))
        global (global-compile global)
        module (do-compile-batch (select-keys global [extend-meta-key timeout-key]) module)
        reserve (reserve-compile reserve)]
    (->> (concat [global] module reserve)
         (into {} (map into-name-map)))))



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


