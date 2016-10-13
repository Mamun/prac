(ns dadysql.compiler.util
  (:require [clojure.string]))


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
    :dadysql.core/join-many-many [d-tab s-tab r-tab]
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




;;;;;;;;;;;;;;;;,Join emit ;

(defn map-reverse-join
  [join-coll]
  (let [f (fn [[s-tab s-id join-key d-tab d-id [r-tab r-id r-id2] :as j]]
            (condp = join-key
              :dadysql.core/join-one-one [d-tab d-id :dadysql.core/join-one-one s-tab s-id]
              :dadysql.core/join-one-many [d-tab d-id :dadysql.core/join-many-one s-tab s-id]
              :dadysql.core/join-many-one [d-tab d-id :dadysql.core/join-one-many s-tab s-id]
              :dadysql.core/join-many-many [d-tab d-id :dadysql.core/join-many-many s-tab s-id [r-tab r-id2 r-id]]
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
       (map-reverse-join)
       (group-by-join-src)))


;;;;;;;;;;;;;;;;,,Emit sql ;;;;;;






;;;;;;;;;;;;;;;;;;;;;


#_(defn param-emit [w]
  (condp = (second w)
    :dadysql.core/param-ref-fn-key (assoc w 2 (resolve (nth w 2)))
    w))







(comment

  ;(resolve 'inc)

  (->> [[:id validation-type-key 'inc :a]]
       (validation-emit-batch))

  )




