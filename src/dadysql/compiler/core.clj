(ns dadysql.compiler.core
  (:require [dadysql.compiler.spec :as cs]
            [dadysql.clj.common :as cc]
            [dadysql.compiler.validation :as u]
            [dadysql.impl.param-spec-impl :as su]
            [dadysql.compiler.core-sql :as sql]
            [dadysql.compiler.core-inheritance :as ci]
            [clojure.spec :as s]))



(defn do-debug [m]
  (println m)
  m
  )


(defn default-config
  []
  {:dadysql.core/file-reload true
   :dadysql.core/timeout     1000
   :dadysql.core/name        :_global_
   :dadysql.core/tx-prop     [:isolation :serializable :read-only? true]})




(defn reserve-regex []
  #":_.*")


(defn group-by-reserve-key
  [r-name-coll coll]
  (->> coll
       (group-by (fn [m]
                   (let [name (get-in m [:dadysql.core/name])]
                     (if (or (re-matches (reserve-regex) (str name))
                             (contains? r-name-coll name)
                             (= :_global_ name))
                       :reserve
                       :modules))))))


(defn group-by-config-key
  [coll]
  (->> coll
       (group-by #(if (= :_global_ (:dadysql.core/name %))
                   :global
                   :modules))))


(defn do-grouping [coll]
  (let [{:keys [global modules]} (group-by-config-key coll)
        f-global (or (first global) {})
        {:keys [reserve modules]} (-> (get-in f-global [:dadysql.core/reserve-name])
                                      (group-by-reserve-key modules))]
    (hash-map :global [f-global] :reserve reserve :modules modules)))



(defn map-reverse-join
  [join-coll]
  (let [f (fn [[s-tab s-id join-key d-tab d-id [r-tab r-id r-id2] :as j]]
            (condp = join-key
              :dadymodel.core/rel-1-1 [d-tab d-id :dadymodel.core/rel-1-1 s-tab s-id]
              :dadymodel.core/rel-1-n [d-tab d-id :dadymodel.core/rel-n-1 s-tab s-id]
              :dadymodel.core/rel-n-1 [d-tab d-id :dadymodel.core/rel-1-n s-tab s-id]
              :dadymodel.core/rel-n-n [d-tab d-id :dadymodel.core/rel-n-n s-tab s-id [r-tab r-id2 r-id]]
              j))]
    (->> (map f join-coll)
         (concat join-coll)
         (distinct)
         (sort-by first)
         (into []))))


(defn group-by-join-src
  [join-coll]
  (->> join-coll
       (group-by first)
       (map (fn [[k coll]]
              {k {:dadymodel.core/join coll}}))
       (into {})))




(defn do-skip
  [m]
  (->> (into [] (:dadysql.core/skip m))
       (apply dissoc m)))


(def skip-key-for-call [:dadymodel.core/join :dadysql.core/param-spec :dadysql.core/default-param])
(def skip-key-for-others [:dadysql.core/result :clojure.core/column])


(defn do-skip-for-dml-type
  [m]
  (condp = (:dadysql.core/dml m)
    :dadysql.core/dml-select m
    :dadysql.core/dml-call (apply dissoc m skip-key-for-call)
    (apply dissoc m skip-key-for-others)))


(defn assoc-default-key
  [m]
  (if (:dadysql.core/model m)
    m
    (assoc m :dadysql.core/model (:dadysql.core/name m))))


#_(defn remove-duplicate [m]
  (->> (keys m)
       (reduce (fn [acc k]
                 (condp = k
                   :dadysql.core/default-param (update-in acc [k] (fn [w] (cc/distinct-with-range 2 w)))
                   acc)
                 ) m)))


(defmulti compile-m (fn [type _ _] type))
(def compile-module (partial compile-m :modules))
(def compile-global (partial compile-m :global))
(def compile-reserve (partial compile-m :reserve))


(defmethod compile-m
  :modules
  [_ tm global-m]
  (let [model-m (sql/map-sql-with-name-model tm)]
    (reduce (fn [acc v]
              (->> (ci/do-inheritance v tm global-m)
                   ;                (remove-duplicate)
                   (assoc-default-key)
                   (do-skip)
                   (do-skip-for-dml-type)
                   (conj acc))
              ) [] model-m)))



(defmethod compile-m
  :global
  [_ tm _]
  (let [v (->> (get-in tm [:dadymodel.core/join])
               (map-reverse-join)
               (group-by-join-src))
        w (merge-with merge v (get-in tm [:dadysql.core/extend]))]
    (-> tm
        (dissoc :dadymodel.core/join)
        (assoc :dadysql.core/extend w))))


(defmethod compile-m
  :reserve
  [_ tm _]
  (update-in tm [:dadysql.core/sql] (fn [v] (clojure.string/join ";" v))))




(defn into-name-map
  [v]
  (hash-map (:dadysql.core/name v) v))


(defn do-validation [coll]
  (do
    (cs/validate-input-spec! coll)
    (u/validate-distinct-name! coll)
    (u/validate-name-sql! coll)
    (u/validate-name-model! coll)
    (u/validate-extend-key! coll)
    (u/validate-join-key! coll)))


(defn do-compile [coll file-name]
  (do-validation coll)
  (let [{:keys [modules global reserve]} (do-grouping coll)
        global (first (mapv #(compile-global % nil) global))
        modules (apply concat (mapv #(compile-module % global) modules))
        reserve (mapv #(compile-reserve % nil) reserve)
        global (dissoc global :dadysql.core/extend)]
    (->> (concat [global] modules reserve)
         (su/eval-and-assoc-spec file-name)
         (into {} (map into-name-map)))))





(comment
  ;(require )

  ;(symbol 'he-hcsdf)
  ;(symbol "asdf")
  ;(clojure.set/rename-keys {:a 3} {:b :v})
  ;(as-parent-ns "tie.edn.sql")

  (clojure.string/split "tie.edn.sql" #"\.")

  (
    (->
      (read-file "tie.edn.sql")
      ;(second)
      ;(second)
      ;(get-in [:dadysql.core/default-param 0 2])

      (clojure.pprint/pprint))
    2
    )

  (-> (read-file "tie3.edn.sql")
      ;(:get-dept-by-id)
      (get-in [:get-dept-by-id :dadysql.core/param-spec-defined :id])
      (eval)
      (s/form)
      ;(first )
      ;(eval)
      ;(name)
      ;(clojure.pprint/pprint)
      )


  (-> (read-file "tie3.edn.sql")
      ;(:get-dept-by-id)
      (get-in [:get-dept-employee :dadysql.core/param-spec-defined :id])
      (eval)
      ;(s/form)
      ;(first )

      ;(name)

      )




  (->>
    ((fr/read-file "tie3.edn.sql") alais-map)
    (s/conform :dadysql.core/compiler-spec)
    ;(do-compile)
    ;  (s/explain-data :dadysql.core/compiler-spec )
    (clojure.pprint/pprint)
    )

  (clojure.pprint/pprint
    (s/exercise :dadysql.core/compiler-spec 1))

  (->> (read-file "tie3.edn.sql")
       #_(s/conform :dadysql.compiler.spec/compiler-input-spec))

  (do (read-file "tie.edn.sql")
      nil)


  (:hello.get-dept-by-id/spec
    (sp/registry))
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