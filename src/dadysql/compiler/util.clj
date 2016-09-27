(ns dadysql.compiler.util
  (:use [dadysql.spec])
  (:require [clojure.spec :as s]
            [clojure.string]
            [dady.common :as dc]))


(defn validate-input-spec! [coll]
  (let [w (s/conform :dadysql.core/compiler-input-spec coll)]
    (if (= w :clojure.spec/invalid)
      (do
        (println (s/explain :dadysql.core/compiler-input-spec coll))
        (throw (ex-info "Compile failed " (s/explain-data :dadysql.core/compiler-input-spec coll)))))))


(defn validate-distinct-name! [coll]
  (let [i-coll (flatten (mapv :dadysql.core/name coll))]
    (if-not (apply distinct? i-coll)
      (let [w (->> (frequencies i-coll)
                   (filter (fn [[_ v]]
                             (if (< 1 v) true false)))
                   (into {}))]
        (throw (ex-info (format "Found duplicate name %s" (str (keys w))) w))))))


(defn validate-name-model! [coll]
  (let [i-coll (->> (mapv (juxt :dadysql.core/name :dadysql.core/model) coll)
                    (filter (fn [v] (every? vector? v))))]
    (doseq [[name-coll model-coll] i-coll]
      (let [t-iden (count name-coll)
            t-sqls (count model-coll)]
        (when-not (= t-sqls t-iden)
          (if (> t-sqls t-iden)
            (throw (Exception. (format "Name not found for \" %s \" " (str model-coll))))
            (throw (Exception. (format "Model not found for \" %s \" " (str name-coll))))))))))


(defn validate-name-sql! [coll]
  (let [i-coll (->> (mapv (juxt :dadysql.core/name :dadysql.core/sql) coll)
                    (filter (fn [v] (every? vector? v))))]
    (doseq [[name-coll sql-coll] i-coll]
      (let [t-iden (count name-coll)
            t-sqls (count sql-coll)]
        (when-not (= t-sqls t-iden)
          (if (> t-sqls t-iden)
            (throw (Exception. (format "Name not found for \" %s \" " (str sql-coll))))
            (throw (Exception. (format "Sql statement not found \" %s \" " (str name-coll))))))))))


(defn validate-extend-key! [coll]
  (let [i-coll (->> (mapv (juxt :dadysql.core/name :dadysql.core/extend) coll)
                    (mapv (fn [[n e]]
                            (let [n (if (vector? n)
                                      (into #{} n)
                                      #{n}
                                      )]
                              [n (keys e)])))
                    (filter (fn [[n e]] (if (or
                                              (nil? e)
                                              (empty? e))
                                          false true))))]
    (doall
      (for [[name-coll ex-name-list] i-coll
            ex-name ex-name-list]
        (if-not (contains? name-coll ex-name)
          (throw (Exception. (format "Extend name not found for \" %s \" " ex-name))))))))




(defn find-join-model [[s-tab _ join-key d-tab _ [r-tab]]]
  (condp = join-key
    :dadysql.core/many-many [d-tab s-tab r-tab]
    [d-tab s-tab]))



(defn find-join-model-batch [j-coll]
  (->> j-coll
       (reduce (fn [acc v]
                 (conj acc (find-join-model v))
                 ) [])
       (flatten)
       (into #{})))


(defn validate-join-key! [coll]
  (let [i-coll (->> (mapv (juxt :dadysql.core/join :dadysql.core/model) coll)

                    )]
    ;(println i-coll)
    coll
    )
  )


(defn validation-ns [coll]
  (remove nil? (map :dadysql.core/param-spec coll))
  )




(comment



  (->> [[:department :id :1-n :employee :dept_id]
        [:employee :id :1-1 :employee-detail :employee_id]
        [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]

       (find-join-model-batch)

       )




  [[[[:department :id :1-n :employee :dept_id]
     [:employee :id :1-1 :employee-detail :employee_id]
     [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]] nil]
   [nil [:department :department :employee :meeting :employee-meeting]]]

  (let [w [[[[:department :id :1-n :employee :dept_id]
             [:employee :id :1-1 :employee-detail :employee_id]
             [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]]
           [nil]]
        w1 (->> w
                ;(apply concat)
                ;(apply concat)
                )]
    w1

    )

  )


;;;;;;;;;;;;;;;;;;;



(defn map-name-model-sql [m]
  ;(clojure.pprint/pprint m )
  (cond

    (and (keyword? (:dadysql.core/name m))
         (keyword? (:dadysql.core/model m)))
    (do
      [(-> m
           (assoc :dadysql.core/index 0)
           (update-in [:dadysql.core/sql] first))])

    (and (sequential? (:dadysql.core/name m))
         (sequential? (:dadysql.core/model m)))
    (do
      (mapv (fn [i s n m]
              {:dadysql.core/name  n
               :dadysql.core/index i
               :dadysql.core/sql   s
               :dadysql.core/model m})
            (range)
            (get-in m [:dadysql.core/sql])
            (get-in m [:dadysql.core/name])
            (get-in m [:dadysql.core/model])))

    (and (sequential? (:dadysql.core/name m))
         (keyword? (:dadysql.core/model m)))
    (do
      (mapv (fn [i n s]
              {:dadysql.core/index i
               :dadysql.core/name  n
               :dadysql.core/sql   s
               :dadysql.core/model (get-in m [:dadysql.core/model])})
            (range)
            (get-in m [:dadysql.core/name])
            (get-in m [:dadysql.core/sql])))

    (sequential? (:dadysql.core/name m))
    (mapv (fn [i s n]
            {:dadysql.core/name  n
             :dadysql.core/index i
             :dadysql.core/sql   s})
          (range)
          (get-in m [:dadysql.core/sql])
          (get-in m [:dadysql.core/name]))

    (keyword? (:dadysql.core/name m))
    [(-> m
         (assoc :dadysql.core/index 0)
         (update-in [:dadysql.core/sql] first))]

    :else
    (do
      (throw (ex-info "Does not match " m)))))



;;;;;;;;;;;;;;;;;;;;;;



(def as-lower-case-keyword (comp keyword clojure.string/lower-case name))


;;;;;;;;;;;;;;;;,Join emit ;

(defn map-reverse-join
  [join-coll]
  (let [f (fn [[s-tab s-id join-key d-tab d-id [r-tab r-id r-id2] :as j]]
            (condp = join-key
              :dadysql.core/one-one [d-tab d-id :dadysql.core/one-one s-tab s-id]
              :dadysql.core/one-many [d-tab d-id :dadysql.core/many-one s-tab s-id]
              :dadysql.core/many-one [d-tab d-id :dadysql.core/one-many s-tab s-id]
              :dadysql.core/many-many [d-tab d-id :dadysql.core/many-many s-tab s-id [r-tab r-id2 r-id]]
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
              {k {:dadysql.core/join coll}}))
       (into {})))



(defn join-emit [j-coll]
  (->> j-coll
       ;(join-emission-batch)
       (map-reverse-join)
       (group-by-join-src)))


;;;;;;;;;;;;;;;;,,Emit sql ;;;;;;
#_(defn dml-type
    [v]
    ;(println v)
    (-> v

        (clojure.string/trim)
        (clojure.string/lower-case)
        (clojure.string/split #"\s+")
        (first)
        (keyword)))

(defn dml-type
  [v]
  ;(println v)
  (let [w (-> v
              ; (first)
              (clojure.string/trim)
              (clojure.string/lower-case)
              (clojure.string/split #"\s+")
              (first)
              (keyword))]

    (condp = w
      :select :dadysql.core/dml-select
      :update :dadysql.core/dml-update
      :insert :dadysql.core/dml-insert
      :delete :dadysql.core/dml-delete
      :call :dadysql.core/dml-call
      (throw (ex-info "Undefined dml op" {:for v})))
    ))



(def sql-param-regex #"\w*:[\w|\-|#]+")


(defn sql-str-emit
  [sql-str]
  (->> (re-seq sql-param-regex sql-str)
       (transduce (comp (map read-string)) conj)
       (reduce (fn [acc v]
                 (let [w (as-lower-case-keyword v)
                       sql-str-w (-> (first acc)
                                     (clojure.string/replace-first (re-pattern (dc/as-string v)) (dc/as-string w)))]
                   (-> (assoc-in acc [0] sql-str-w)
                       (conj w)))
                 ) [sql-str])))


#_(defn sql-emit
    [sql-str]
    (let [p (comp (filter not-empty)
                  (map sql-str-emit)
                  (map (fn [v] {:dadysql.core/sql v
                                dml-key           (dml-type v)})))
          sql (-> (clojure.string/trim sql-str)
                  (clojure.string/lower-case)
                  (clojure.string/split #";"))]
      (->> (transduce p conj [] sql)
           (mapv (fn [i m]
                   (assoc m index i)
                   ) (range)))))


;;;;;;;;;;;;;;;;;;;;;


(defn param-emit [w]
  (condp = (second w)
    :dadysql.core/ref-fn-key (assoc w 2 (resolve (nth w 2)))
    w))




#_(defn validation-emit [v]
    (condp = (second v)
      validation-type-key (assoc v 2 (resolve (nth v 2)))
      validation-contain-key (assoc v 2 (resolve (nth v 2)))
      v))





(comment

  ;(resolve 'inc)

  (->> [[:id validation-type-key 'inc :a]]
       (validation-emit-batch))

  )




