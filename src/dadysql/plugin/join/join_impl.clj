(ns dadysql.plugin.join.join-impl
  (:use [dady.proto])
  (:require [dadysql.constant :refer :all]
            [dady.common :as cc]
            [schema.core :as s]))


(defrecord JoinKey [cname corder])

(defn new-join-key []
  (map->JoinKey {:cname  join-key
                 :corder 2}))

(extend-protocol ILeafNode
  JoinKey
  (-node-name [this] (:cname this))
  (-corder [this] (:corder this)))




(defn join-emission-batch [j-coll]
  (mapv (fn [j]
          (condp = (nth j 2)
            join-n-n-key
            (-> j
                (update-in [0] cc/as-lower-case-keyword)
                (update-in [1] cc/as-lower-case-keyword)
                (update-in [3] cc/as-lower-case-keyword)
                (update-in [4] cc/as-lower-case-keyword)
                (update-in [5 1] cc/as-lower-case-keyword)
                (update-in [5 2] cc/as-lower-case-keyword))
            (-> j
                (update-in [0] cc/as-lower-case-keyword)
                (update-in [1] cc/as-lower-case-keyword)
                (update-in [3] cc/as-lower-case-keyword)
                (update-in [4] cc/as-lower-case-keyword)))
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
              {k {join-key coll}}))
       (into {})))





(extend-protocol INodeCompiler
  JoinKey
  (-spec [this]
    `{(schema.core/optional-key ~(-node-name this))
      [[(schema.core/one schema.core/Keyword "Source Data Model")
        (schema.core/one schema.core/Keyword "Source Model Id")
        (schema.core/one (schema.core/enum ~join-1-n-key ~join-1-1-key ~join-n-1-key ~join-n-n-key) "Relationship")
        (schema.core/one schema.core/Keyword "Dest Model")
        (schema.core/one schema.core/Keyword "Dest Model Id")
        (schema.core/optional [(schema.core/one schema.core/Keyword "Join Model ")
                     (schema.core/one schema.core/Keyword "Join Model Id1")
                     (schema.core/one schema.core/Keyword "Join Model Id2")] "JoinSingleNTNSchema")]]})
  (-emit [_ j-coll]
    (->> j-coll
         (join-emission-batch)
         (map-reverse-join)
         (group-by-join-src))))







(defn filter-join-key-coll
  [join model-coll]
  (->> join
       (filter (fn [[_ _ rel d-table _ nr]]
                 (if (= rel join-n-n-key)
                   (some #{(first nr)} model-coll)
                   (some #{d-table} model-coll))))
       (into [])))
