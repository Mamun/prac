(ns dadysql.plugin.util
  (:require [dadysql.spec :refer :all]
            [dady.proto :refer :all]
            [dady.fail :as f]
            [clojure.set]
            [dady.common :as cc]
            [dadysql.plugin.join.join-impl :as j]))





(defn empty-path
  []
  [[]])


(defn conj-index
  [data c-path]
  (let [path-value (get-in data c-path)]
    (if (sequential? path-value)
      (->> (count path-value)
           (range 0)
           (mapv #(conj c-path %)))
      [c-path])))


(defn get-path
  ([data name]
   (get-path data (empty-path) name))
  ([data p-path-coll name]
   (for [c-path p-path-coll
         i-path (conj-index data c-path)
         :let [n-path (conj i-path name)]
         w (conj-index data n-path)]
     w)))


