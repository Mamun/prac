(ns dadysql.impl.util
  (:require
    [clojure.set]))


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









(comment


  (get-path {:id 3} :id)
  (get-path-batch {:id 3 :id2 5} [] (list :id :id2) )

  )