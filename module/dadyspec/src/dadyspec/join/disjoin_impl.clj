(ns dadyspec.join.disjoin-impl
  (:require [clojure.walk :as w]
            [dadyspec.join.join-path-finder :as p]))


(defn group-by-target-entity-one
  [data j]
  (if (= :dadyspec.core/rel-n-n (nth j 2))
    (let [[st stc _ dt dtc [rdt s d]] j]
      {rdt [{s (get-in data (conj st stc))
             d (get-in data (conj dt dtc))}]})
    (let [[s _ _ d] j
          tdata (get-in data (conj s d))]
      {d (p/as-sequential tdata)})))


(defn group-by-target-entity-batch
  [join-coll data]
  (->> join-coll
       (map #(group-by-target-entity-one data %))
       (apply merge-with (comp vec distinct concat))))


(defn assoc-n-n-join-key [data join-coll]
  (let [n-join (filter (fn [[_ _ rel]]
                         (if (= rel :dadyspec.core/rel-n-n)
                           true
                           false)
                         ) join-coll)]
    (if (empty? n-join)
      {}
      (-> n-join
          (p/replace-target-entity-path data)
          (group-by-target-entity-batch data)))))


(defn assoc-target-entity-key
  [data j]
  (let [[s-tab s _ d-tab d] j
        s-ks (conj s-tab s)
        d-ks (conj d-tab d)]
    (if (map? (get-in data d-tab))
      (assoc-in data d-ks (get-in data s-ks))
      data)))


(defn assoc-1-join-key [data join-coll]
  (let [join (w/postwalk (fn [w]
                           (if (= :dadyspec.core/rel-n-n w)
                             :dadyspec.core/rel-1-n
                             w)
                           ) join-coll)]
    (->> data
         (p/replace-target-entity-path join)
         (reduce assoc-target-entity-key data)
         (group-by-target-entity-batch join))))

(defn update-target-data [data join-coll target-data-m]
  (->> join-coll
       (reduce (fn [acc [s _ r d _]]
                 (update-in acc s
                            (fn [m]
                              (if (or (= r :dadyspec.core/rel-1-1)
                                      (= r :dadyspec.core/rel-n-1))
                                (assoc m d (first (get target-data-m d)))
                                (assoc m d (get target-data-m d)))))
                 ) data)))



(defn assoc-join-key
  [data join-coll]
  (if (empty? join-coll)
    data
    (let [join-coll (p/replace-source-entity-path join-coll data)
          nj-data (assoc-n-n-join-key data join-coll)
          ;Assos relation key
          target-data-m (assoc-1-join-key data join-coll)
          data1 (update-target-data data join-coll target-data-m)]
      (merge data1 nj-data))))



(defn do-disjoin
  "Assoc relation key and dis-join relation model "
  [data join-coll]
  (if (empty? join-coll)
    data
    (->> (p/replace-source-entity-path join-coll data)
         (reduce (fn [acc [s _ _ d _]]
                   (-> (assoc acc d (get-in data (conj s d)))
                       (update-in s dissoc d))
                   ) data))))


(comment


  (let [j [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]]
        ;j (rename-joi-key j)

        data {:dept
              {:id 0,
               :name "",
               :student-list
               [{:name "", :id 0}
                {:name "", :id -1}]
               :note ""}}]
    #_(assoc-join-key data j)
    #_(do-disjoin (assoc-join-key data j) j)
    #_(p/replace-source-entity-path j data)
    (p/rename-join-key j)

    )

  )
