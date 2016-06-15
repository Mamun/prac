(ns dadysql.compiler.spec-util
  (:use [dadysql.constant])
  (:require [clojure.string :as s]
            [dady.common :as dc]))

(def as-lower-case-keyword (comp keyword s/lower-case name))


;;;;;;;;;;;;;;;;,Join emit ;

(defn join-emission-batch [j-coll]
  (mapv (fn [j]
          (condp = (nth j 2)
            :n-n
            (-> j
                (update-in [0] as-lower-case-keyword)
                (update-in [1] as-lower-case-keyword)
                (update-in [3] as-lower-case-keyword)
                (update-in [4] as-lower-case-keyword)
                (update-in [5 1] as-lower-case-keyword)
                (update-in [5 2] as-lower-case-keyword))
            (-> j
                (update-in [0] as-lower-case-keyword)
                (update-in [1] as-lower-case-keyword)
                (update-in [3] as-lower-case-keyword)
                (update-in [4] as-lower-case-keyword)))
          ) j-coll))



(defn map-reverse-join
  [join-coll]
  (let [f (fn [[s-tab s-id join-key d-tab d-id [r-tab r-id r-id2] :as j]]
            (condp = join-key
              join-1-1-key [d-tab d-id join-1-1-key s-tab s-id]
              join-1-n-key [d-tab d-id join-n-1-key s-tab s-id]
              join-n-1-key [d-tab d-id join-1-n-key s-tab s-id]
              join-n-n-key [d-tab d-id join-n-n-key s-tab s-id [r-tab r-id2 r-id]]
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
              {k {:join coll}}))
       (into {})))



(defn join-emit [j-coll]
  (->> j-coll
       (join-emission-batch)
       (map-reverse-join)
       (group-by-join-src)))


;;;;;;;;;;;;;;;;,,Emit sql ;;;;;;
(defn dml-type
  [v]
  (-> v
      (first)
      (clojure.string/trim)
      (clojure.string/lower-case)
      (clojure.string/split #"\s+")
      (first)
      (keyword)))


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


(defn sql-emit
  [sql-str]
  (let [p (comp (filter not-empty)
                (map sql-str-emit)
                (map (fn [v] {sql-key v
                              dml-key (dml-type v)})))
        sql (clojure.string/split (clojure.string/trim sql-str) #";")]
    (->> (transduce p conj [] sql)
         (mapv (fn [i m]
                 (assoc m index i)
                 ) (range)))))



;;;;;;;;;;;;;;;;;;;;;





