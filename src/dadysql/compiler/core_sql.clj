(ns dadysql.compiler.core-sql
  (:require [dady.common :as dc]))



(defn dml-type
  [v]
  (let [w (-> v
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
      (throw (ex-info "Undefined dml op" {:for v})))))



(def as-lower-case-keyword (comp keyword clojure.string/lower-case name))

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



(defn dispatch-type [m]
  (cond
    (and (keyword? (:dadysql.core/name m))
         (keyword? (:dadysql.core/model m)))
    :keyword
    (and (sequential? (:dadysql.core/name m))
         (sequential? (:dadysql.core/model m)))
    :sequential
    (and (sequential? (:dadysql.core/name m))
         (keyword? (:dadysql.core/model m)))
    :keyword-sequential

    (sequential? (:dadysql.core/name m))
    :name-sequential
    (keyword? (:dadysql.core/name m))
    :name-keyword))


(defmulti map-name-model-sql (fn [m] (dispatch-type m)))


(defmethod map-name-model-sql
  :name-keyword
  [m]
  (let [sql (-> (:dadysql.core/sql m)
                (first)
                )]
    [(-> m
         (assoc :dadysql.core/index 0)
         (assoc :dadysql.core/dml (dml-type sql))
         (assoc :dadysql.core/sql (sql-str-emit sql)))]))


(defmethod map-name-model-sql
  :name-sequential
  [m]
  (mapv (fn [i s n]
          {:dadysql.core/name  n
           :dadysql.core/index i
           :dadysql.core/dml   (dml-type s)
           :dadysql.core/sql   (sql-str-emit s)})
        (range)
        (get-in m [:dadysql.core/sql])
        (get-in m [:dadysql.core/name])))



(defmethod map-name-model-sql
  :keyword
  [m]
  (let [sql (-> (:dadysql.core/sql m)
                (first)
                )]
    [(-> m
         (assoc :dadysql.core/index 0)
         (assoc :dadysql.core/dml (dml-type sql))
         (assoc :dadysql.core/sql (sql-str-emit sql)))]))


(defmethod map-name-model-sql
  :sequential
  [m]
  (do
    (mapv (fn [i s n m]
            {:dadysql.core/name  n
             :dadysql.core/dml   (dml-type s)
             :dadysql.core/index i
             :dadysql.core/sql   (sql-str-emit s)
             :dadysql.core/model m})
          (range)
          (get-in m [:dadysql.core/sql])
          (get-in m [:dadysql.core/name])
          (get-in m [:dadysql.core/model]))))


(defmethod map-name-model-sql
  :keyword-sequential
  [m]
  (do
    (mapv (fn [i n s]
            (let [sql (sql-str-emit s)]
              {:dadysql.core/index i
               :dadysql.core/name  n
               :dadysql.core/dml   (dml-type s)
               :dadysql.core/sql   sql
               :dadysql.core/model (get-in m [:dadysql.core/model])}))
          (range)
          (get-in m [:dadysql.core/name])
          (get-in m [:dadysql.core/sql]))))

