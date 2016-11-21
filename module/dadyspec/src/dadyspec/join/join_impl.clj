(ns dadyspec.join.join-impl
  (:require [clojure.walk :as w]
            [dadyspec.join.join-path-finder :as p]))


(defn select-root
  [j-data j-coll]
  (select-keys j-data [(get-in j-coll [0 0])]))


(defn- is-join-skip?
  [join data]
  (let [root (get-in join [0 0])]
    (if (or (nil? join)
            (empty? join)
            (empty? (root data))
            (and (not (map? (root data)))
                 (sequential? (first (root data)))))
      true false)))


(defn get-target-relational-key-value
  [target-rel-data-m data-m [s st rel d dt [_ nst _]]]
  (let [s-value (get-in data-m (conj s st))]
    (if (= :dadyspec.core/rel-n-n rel)
      (get-in target-rel-data-m [d nst s-value])
      (get-in target-rel-data-m [d dt s-value]))))


(defn assoc-to-source-entity-batch
  [target-rel-data-m data-m j-coll]
  (reduce (fn [acc [s _ _ d :as j]]
            (->> (get-target-relational-key-value target-rel-data-m data-m j)
                 (assoc-in acc (conj s d)))
            ) data-m j-coll))


(defn group-by-target-entity-key-one
  ""
  [[_ _ rel d dt [n nst _]] data-m]
  (if (= rel :dadyspec.core/rel-n-n)
    {d {nst (p/group-by-value nst (get data-m n))}}
    {d {dt (p/group-by-value dt (get data-m d))}}))



(defn group-by-target-entity-key-batch
  [j-coll data]
  (reduce (fn [acc j]
            (->> (group-by-target-entity-key-one j data)
                 (merge-with merge acc))
            ) {} j-coll))



(defn do-join
  " do data join "
  [data j-coll]
  (if (is-join-skip? j-coll data)
    data
    (-> j-coll
        (group-by-target-entity-key-batch data)
        (assoc-to-source-entity-batch data (p/replace-source-entity-path j-coll data))
        (select-root j-coll))))


(defn assoc-target-entity-key
  [data j]
  (let [[s-tab s _ d-tab d] j
        s-ks (conj s-tab s)
        d-ks (conj d-tab d)]
    (if (map? (get-in data d-tab))
      (assoc-in data d-ks (get-in data s-ks))
      data)))









;nj-data

#_(if (empty? n-join)
    {}
    (-> n-join
        (replace-target-entity-path data)
        (group-by-target-entity-batch data)))
;Assos relation key
;target-data-m

#_(->> data
       (replace-target-entity-path join)
       (reduce assoc-target-entity-key data)
       (group-by-target-entity-batch join))