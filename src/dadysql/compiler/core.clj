(ns dadysql.compiler.core
  (:require [dadysql.core :refer :all]
            [dadysql.core]
            [dady.common :as cc]
            [dadysql.compiler.util :as u]
            [clojure.tools.reader.edn :as edn]
            [dadysql.compiler.file-reader :as fr]
            [clojure.spec :as s]))



(defn do-debug [m]
  (println m)
  m
  )


(defn default-config
  []
  {:dadysql.core/file-reload true
   :dadysql.core/timeout     1000
   :dadysql.core/name        global-key
   :dadysql.core/tx-prop     [:isolation :serializable :read-only? true]})


(defn compiler-merge
  [old new]
  (cond (map? new) (merge old new)
        (vector? new) (into (empty old) (concat new old))
        :else (or new old)))


(defn reserve-regex []
  #":_.*")


(defn group-by-reserve-key
  [r-name-coll coll]
  (->> coll
       (group-by (fn [m]
                   (let [name (get-in m [:dadysql.core/name])]
                     (if (or (re-matches (reserve-regex) (str name))
                             (contains? r-name-coll name)
                             (= global-key name))
                       :reserve
                       :modules))))))

;(s/valid? (s/every integer?) [1 2 3])

(defn group-by-config-key
  [coll]
  (->> coll
       (group-by #(if (= global-key (:dadysql.core/name %))
                   :global
                   :modules))))


(defn do-grouping [coll]
  (let [{:keys [global modules]} (group-by-config-key coll)
        f-global (or (first global) {})
        {:keys [reserve modules]} (-> (get-in f-global [:dadysql.core/reserve-name])
                                      (group-by-reserve-key modules))]
    (hash-map :global f-global :reserve reserve :modules modules)))


(defn do-merge
  [w module-m f-config]
  (let [name-v (get w :dadysql.core/name)
        w1 (merge-with compiler-merge
                       (get-in f-config [:dadysql.core/extend name-v])
                       (get-in module-m [:dadysql.core/extend name-v])
                       w)

        model-v (get w1 :dadysql.core/model)
        w2 (merge-with compiler-merge
                       (get-in f-config [:dadysql.core/extend model-v])
                       (get-in module-m [:dadysql.core/extend model-v])
                       w1)

        module-m (dissoc module-m :dadysql.core/name :dadysql.core/model :dadysql.core/sql :dadysql.core/extend doc-key)
        f-config (select-keys f-config [:dadysql.core/param :dadysql.core/param-spec :dadysql.core/timeout])]
    (merge-with compiler-merge f-config module-m w2)))


(defn do-skip
  [m]
  (->> (into [] (skip-key m))
       (apply dissoc m)))


(def skip-key-for-call [:dadysql.core/join :dadysql.core/param-spec :dadysql.core/param])
(def skip-key-for-others [:dadysql.core/result :clojure.core/column])


(defn do-skip-for-dml-type
  [m]
  (condp = (:dadysql.core/dml-key m)
    dml-select-key m
    dml-call-key (apply dissoc m skip-key-for-call)
    (apply dissoc m skip-key-for-others)))


(defn do-merge-default
  [m]
  (if (:dadysql.core/model m)
    m
    (assoc m :dadysql.core/model (:dadysql.core/name m))))


(defn remove-duplicate [m]
  (->> (keys m)
       (reduce (fn [acc k]
                 (condp = k
                   :dadysql.core/param (update-in acc [k] (fn [w] (cc/distinct-with-range 2 w)))
                   acc)
                 ) m)))


(defn compiler-emit [m]
  (-> m
      (assoc :dadysql.core/dml-key (u/dml-type (:dadysql.core/sql m)))
      (update-in [:dadysql.core/sql] u/sql-str-emit)
      (cc/update-if-contains [:dadysql.core/param] #(mapv u/param-emit %))))



(defn compile-one [m global-m]

 ; (clojure.pprint/pprint m )
  (let [;m (edn/read-string (clojure.string/lower-case (str m)))
        model-m (u/map-name-model-sql (select-keys m [:dadysql.core/name :dadysql.core/model :dadysql.core/sql]))]
   ; (println model-m)
    (reduce (fn [acc v]
              (->> (do-merge v m global-m)
                   (remove-duplicate)
                   (do-merge-default)
                   (compiler-emit)
                   (do-skip)
                   (do-skip-for-dml-type)
                   (conj acc))
              ) [] model-m)))



(defn compile-batch [global-m m-coll]
  (reduce (fn [acc m]
            (reduce conj acc (compile-one m global-m))
            ) [] m-coll))


(defn compile-one-config [tm]
  (let [v (get-in tm [:dadysql.core/join])
        v (u/join-emit v)
        w (merge-with merge v (get-in tm [:dadysql.core/extend]))]
    (-> tm
        (dissoc :dadysql.core/join)
        (assoc :dadysql.core/extend w))))


(defn reserve-compile [coll]
  (mapv (fn [m]
          (-> m
              (update-in [:dadysql.core/sql] (fn [v] (clojure.string/join ";" v))))
          ) coll))


(defn into-name-map
  [v]
  (hash-map (:dadysql.core/name v) v))



(defn load-param-spec [coll]
  (let [w1 (filter keyword? (map :dadysql.core/param-spec coll))]
    (doseq [w (map namespace w1)]
      (require (symbol w) :reload))
    (doseq [r w1]
      (if (nil? (s/get-spec r))
        (throw (ex-info "Spec not found " {:spec r}))))))



#_(defn replace-mk
  [f1 m]
  (let [f (fn [[k v]] [(f1 k) v])]
    (into {} (map f m))))


(defn postwalk-rename-key
  "Recursively transforms all map and first  vector keys from keywords to strings."
  {:added "1.1"}
  [m]
  (clojure.walk/postwalk (fn [x]
                           (cond (map? x)
                                 (clojure.set/rename-keys x namespace-key )
                                 :else x)) m)
  )



#_(defn update-keys [coll]



  (mapv (fn [w]
          (clojure.set/rename-keys w )
          ) coll))



(defn do-compile [coll]
  (u/validate-input-spec! coll)
  (let [coll (postwalk-rename-key coll)]
    (u/validate-distinct-name! coll)
    (u/validate-name-sql! coll)
    (u/validate-name-model! coll)
    (u/validate-extend-key! coll)
    (u/validate-join-key! coll)
    (let [{:keys [modules global reserve]} (do-grouping coll)
          global (compile-one-config global)
          modules (compile-batch global modules)
          reserve (reserve-compile reserve)
          global (dissoc global :dadysql.core/extend)
          w (concat [global] modules reserve)]
      (load-param-spec w)
      (->> w

           (into {} (map into-name-map))))))



(defn read-file [file-name]
  (do-compile (fr/read-file file-name)))



(comment
  ;(require )

  ;(clojure.set/rename-keys {:a 3} {:b :v})

  (->> (fr/read-file "tie.edn2.sql")
     ;  (postwalk-rename-key  )
       (do-compile)
     ;  (s/explain-data :dadysql.core/compiler-input-spec )
       ;(clojure.pprint/pprint)
       )

  (->> (fr/read-file "tie.edn2.sql")
       (do-compile)
       #_(s/conform :dadysql.compiler.spec/compiler-input-spec))


  ;(clojure.set/rename-keys {:a 3 :b 4} {:a :tr/c})




  (let [v {:name [:many [:insert-dept :update-dept :delete-dept]]}]
    (m/match v
             {:name [:many _]} :many
             {:name [:many _]} :many
             :else nil))

  )


(comment

  ;(filter odd? [ 1 2 3])



  (s/registry)

  ;(add-quote Hello)

  ;'tie-edn
  ;`

  (let [w1 (mapv namespace (list :tie-edn1/hello))]
    (doseq [w w1]
      (require (symbol w) :reload)))





  (println (quote [a]))



  (let [v (mapv namespace (list :a/b))
        w (list 'quote v)]
    (println (eval w)))



  ;(println ''a)











  ;(s/registry)

  )