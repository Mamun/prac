(ns dadysql.compiler.core-sql
  (:require [dadysql.clj.common :as dc]))



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



(defn do-format [m]
  (cond
    (and (keyword? (:dadysql.core/name m))
         (keyword? (:dadysql.core/model m)))
    (mapv vector
          (range)
          (:dadysql.core/sql m)
          (repeat (:dadysql.core/name m))
          (repeat (:dadysql.core/model m)))

    (and (sequential? (:dadysql.core/name m))
         (sequential? (:dadysql.core/model m)))
    (mapv vector
          (range)
          (:dadysql.core/sql m)
          (:dadysql.core/name m)
          (:dadysql.core/model m))

    (and (sequential? (:dadysql.core/name m))
         (keyword? (:dadysql.core/model m)))
    (mapv vector
          (range)
          (get-in m [:dadysql.core/sql])
          (get-in m [:dadysql.core/name])
          (repeat (:dadysql.core/model m)))

    (sequential? (:dadysql.core/name m))
    (mapv vector
          (range)
          (:dadysql.core/sql m)
          (:dadysql.core/name m)
          (repeat (:dadysql.core/model m)))

    (keyword? (:dadysql.core/name m))
    (mapv vector
          (range)
          (:dadysql.core/sql m)
          (repeat (:dadysql.core/name m))
          (repeat (:dadysql.core/model m)))))


(defn- map-sql [[i s n m]]
  {:dadysql.core/name  n
   :dadysql.core/dml   (dml-type s)
   :dadysql.core/index i
   :dadysql.core/sql   (sql-str-emit s)
   :dadysql.core/model m})


(defn map-sql-with-name-model
  [m]
  (->> m
       (do-format)
       (mapv map-sql)))




