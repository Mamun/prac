(ns dadysql.plugin.join.join-impl
  (:use [dady.proto])
  (:require #_[dadysql.spec :refer :all]
            [dady.common :as cc]
    #_[schema.core :as s]))


(defrecord JoinKey [cname corder])

(defn new-join-key []
  (map->JoinKey {:cname  :dadysql.core/join
                 :corder 2}))

(extend-protocol ILeafNode
  JoinKey
  (-node-name [this] (:cname this))
  (-corder [this] (:corder this)))




(defn join-emission-batch [j-coll]
  (mapv (fn [j]
          (condp = (nth j 2)
            :dadysql.core/many-many
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


#_(defn map-reverse-join
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







