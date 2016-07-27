(ns dadysql.compiler.core
  (:require [dadysql.spec :refer :all]
            [dadysql.spec]
            [dady.common :as cc]
            [dadysql.compiler.util :as u]
            [clojure.tools.reader.edn :as edn]
            [dadysql.compiler.file-reader :as fr]
            [clojure.spec :as s]
            [dady.spec :as dsp]
            [clojure.spec :as sp]))



(defn do-debug [m]
  (println m)
  m
  )


(defn default-config
  []
  {:dadysql.spec/file-reload true
   :dadysql.spec/timeout     1000
   :dadysql.spec/name        global-key
   :dadysql.spec/tx-prop     [:isolation :serializable :read-only? true]})


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
                   (let [name (get-in m [:dadysql.spec/name])]
                     (if (or (re-matches (reserve-regex) (str name))
                             (contains? r-name-coll name)
                             (= global-key name))
                       :reserve
                       :modules))))))

;(s/valid? (s/every integer?) [1 2 3])

(defn group-by-config-key
  [coll]
  (->> coll
       (group-by #(if (= global-key (:dadysql.spec/name %))
                   :global
                   :modules))))


(defn do-grouping [coll]
  (let [{:keys [global modules]} (group-by-config-key coll)
        f-global (or (first global) {})
        {:keys [reserve modules]} (-> (get-in f-global [:dadysql.spec/reserve-name])
                                      (group-by-reserve-key modules))]
    (hash-map :global f-global :reserve reserve :modules modules)))


(defn do-merge
  [w module-m f-config]
  (let [name-v (get w :dadysql.spec/name)
        w1 (merge-with compiler-merge
                       (get-in f-config [:dadysql.spec/extend name-v])
                       (get-in module-m [:dadysql.spec/extend name-v])
                       w)

        model-v (get w1 :dadysql.spec/model)
        w2 (merge-with compiler-merge
                       (get-in f-config [:dadysql.spec/extend model-v])
                       (get-in module-m [:dadysql.spec/extend model-v])
                       w1)

        module-m (dissoc module-m :dadysql.spec/name :dadysql.spec/model :dadysql.spec/sql :dadysql.spec/extend :dadysql.spec/doc)
        f-config (select-keys f-config [:dadysql.spec/param :dadysql.spec/param-spec :dadysql.spec/timeout])]
    (merge-with compiler-merge f-config module-m w2)))


(defn do-skip
  [m]
  (->> (into [] (:dadysql.spec/skip m))
       (apply dissoc m)))


(def skip-key-for-call [:dadysql.spec/join :dadysql.spec/param-spec :dadysql.spec/param])
(def skip-key-for-others [:dadysql.spec/result :clojure.core/column])


(defn do-skip-for-dml-type
  [m]
  (condp = (:dadysql.spec/dml-key m)
    :dadysql.spec/dml-select m
    :dadysql.spec/dml-call (apply dissoc m skip-key-for-call)
    (apply dissoc m skip-key-for-others)))


(defn do-merge-default
  [m]
  (if (:dadysql.spec/model m)
    m
    (assoc m :dadysql.spec/model (:dadysql.spec/name m))))


(defn remove-duplicate [m]
  (->> (keys m)
       (reduce (fn [acc k]
                 (condp = k
                   :dadysql.spec/param (update-in acc [k] (fn [w] (cc/distinct-with-range 2 w)))
                   acc)
                 ) m)))


(defn compiler-emit [m]
  (-> m
      (assoc :dadysql.spec/dml-key (u/dml-type (:dadysql.spec/sql m)))
      (update-in [:dadysql.spec/sql] u/sql-str-emit)
      (cc/update-if-contains [:dadysql.spec/param] #(mapv u/param-emit %))))



(defn compile-one [m global-m]

  ; (clojure.pprint/pprint m )
  (let [;m (edn/read-string (clojure.string/lower-case (str m)))
        model-m (u/map-name-model-sql (select-keys m [:dadysql.spec/name :dadysql.spec/model :dadysql.spec/sql]))]
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
  (let [v (get-in tm [:dadysql.spec/join])
        v (u/join-emit v)
        w (merge-with merge v (get-in tm [:dadysql.spec/extend]))]
    (-> tm
        (dissoc :dadysql.spec/join)
        (assoc :dadysql.spec/extend w))))


(defn reserve-compile [coll]
  (mapv (fn [m]
          (-> m
              (update-in [:dadysql.spec/sql] (fn [v] (clojure.string/join ";" v))))
          ) coll))


(defn into-name-map
  [v]
  (hash-map (:dadysql.spec/name v) v))



(defn load-param-spec [spec-file coll]
  (if spec-file
    (require (symbol spec-file) :reload))
  (doseq [r (filter keyword? (map :dadysql.spec/param-spec coll))]
    (if (nil? (s/get-spec r))
      (throw (ex-info "Spec not found " {:spec r})))))


(defn key->nskey
  [m mk]
  (clojure.walk/postwalk (fn [x]
                           (if-let [v (get mk x)]
                             v
                             x)) m))


(defn do-compile [coll]
  (u/validate-input-spec! coll)
  (let [coll (key->nskey coll alais-map)]
    (u/validate-distinct-name! coll)
    (u/validate-name-sql! coll)
    (u/validate-name-model! coll)
    (u/validate-extend-key! coll)
    (u/validate-join-key! coll)
    (let [{:keys [modules global reserve]} (do-grouping coll)
          global (compile-one-config global)
          modules (compile-batch global modules)
          reserve (reserve-compile reserve)
          global (dissoc global :dadysql.spec/extend)
          w (concat [global] modules reserve)]
      (load-param-spec (:dadysql.spec/spec-file global) w)
      (->> w
           (into {} (map into-name-map))))))



(defn read-file [file-name]
  (do-compile (fr/read-file file-name)))



(comment
  ;(require )

  ;(symbol 'he-hcsdf)
  ;(symbol "asdf")
  ;(clojure.set/rename-keys {:a 3} {:b :v})

  (->> (fr/read-file "tie.edn.sql")
       ;  (postwalk-rename-key  )
       (do-compile)
       ;  (s/explain-data :dadysql.spec/compiler-input-spec )
       ;(clojure.pprint/pprint)
       )

  (->> (fr/read-file "tie.edn2.sql")
       (do-compile)
       #_(s/conform :dadysql.compiler.spec/compiler-input-spec))


  (sp/registry)
  ;(clojure.set/rename-keys {:a 3 :b 4} {:a :tr/c})




  (let [v {:name [:many [:insert-dept :update-dept :delete-dept]]}]
    (m/match v
             {:name [:many _]} :many
             {:name [:many _]} :many
             :else nil))

  )


(comment

  ;(filter odd? [ 1 2 3])



  (sp/registry)

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