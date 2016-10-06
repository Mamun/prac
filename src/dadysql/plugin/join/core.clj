(ns dadysql.plugin.join.core
  (:require [dady.common :as cc]
            [dady.fail :as f]
            [dadysql.plugin.util :as cu]
            #_[dadysql.spec :refer :all]))


(defn get-source-relational-key-value
  [j-coll data-m]
  (reduce (fn [acc j1]
            (let [[s st rel _ dt [_ sdt _]] j1
                  w (keys (cc/group-by-value st (s data-m)))]
              (if (= rel :dadysql.core/many-many)
                (merge acc {sdt w})
                (merge acc {dt w})))
            ) {} j-coll))


(defn group-by-target-entity-key-one
  ""
  [[_ _ rel d dt [n nst _]] data-m]
  (if (= rel :dadysql.core/many-many)
    {d {nst (cc/group-by-value nst (get data-m n))}}
    {d {dt (cc/group-by-value dt (get data-m d))}}))



(defn group-by-target-entity-key-batch
  [j-coll data]
  (reduce (fn [acc j]
            (->> (group-by-target-entity-key-one j data)
                 (merge-with merge acc))
            ) {} j-coll))



(defn get-target-relational-key-value
  [target-rel-data-m data-m [s st rel d dt [_ nst _]]]
  (let [s-value (get-in data-m (conj s st))]
    (if (= :dadysql.core/many-many rel)
      (get-in target-rel-data-m [d nst s-value])
      (get-in target-rel-data-m [d dt s-value]))))



(defn assoc-to-source-entity-batch
  [target-rel-data-m data-m j-coll]
  (reduce (fn [acc [s _ _ d :as j]]
            (->> (get-target-relational-key-value target-rel-data-m data-m j)
                 (assoc-in acc (conj s d)))
            ) data-m j-coll))



(defn replace-source-entity-path
  [j-coll data-m]
  (for [[g-key coll] (group-by first j-coll)
        j coll
        em (cu/get-path data-m g-key)]
    (assoc j 0 em)))


(defn replace-target-entity-path
  [j-coll data-m]
  (for [[s-tab-id _ _ d-tab-id _ :as j] j-coll
        :let [w (conj s-tab-id d-tab-id)]
        :when (get-in data-m w)
        c (cu/get-path data-m [s-tab-id] d-tab-id)]
    (assoc j 3 c)))


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


(defn do-join
  " do data join "
  [data j-coll]
  (if (is-join-skip? j-coll data)
    data
    (-> j-coll
        (group-by-target-entity-key-batch data)
        (assoc-to-source-entity-batch data (replace-source-entity-path j-coll data))
        (select-root j-coll))))


(defn split-join-n-n-key
  [j-coll]
  (split-with (fn [[_ _ rel]]
                (if (= rel :dadysql.core/many-many)
                  true
                  false))
              j-coll))


(defn group-by-target-entity-one
  [data j]
  (if (= :dadysql.core/many-many (nth j 2))
    (let [[st stc _ dt dtc [rdt s d]] j]
      {rdt [{s (get-in data (conj st stc))
             d (get-in data (conj dt dtc))}]})
    (let [[s _ _ d] j
          tdata (get-in data (conj s d))]
      {d (cc/as-sequential tdata)})))


(defn group-by-target-entity-batch
  [join-coll data]
  (->> join-coll
       (map #(group-by-target-entity-one data %))
       (apply merge-with (comp vec distinct concat))))


(defn assoc-target-entity-key
  [data j]
  (let [[s-tab s _ d-tab d] j
        s-ks (conj s-tab s)
        d-ks (conj d-tab d)]
    (if (map? (get-in data d-tab))
      (assoc-in data d-ks (get-in data s-ks))
      data)))



(defn do-disjoin
  "Assoc relation key and dis-join relation model "
  [data join-coll]
  (if (or (f/failed? data)
          (empty? join-coll))
    data
    (let [join-coll (replace-source-entity-path join-coll data)
          [n-join join] (split-join-n-n-key join-coll)
          ;Find n-n relation data
          nj-data (if (empty? n-join)
                    {}
                    (-> n-join
                        (replace-target-entity-path data)
                        (group-by-target-entity-batch data)))
          ;Assos relation key
          target-data-m (->> data
                             (replace-target-entity-path join)
                             (reduce assoc-target-entity-key data)
                             (group-by-target-entity-batch join))
          ;Dassoc relation
          data (->> join-coll
                    (reduce (fn [acc j]
                              (update-in acc (first j) dissoc (nth j 3))
                              ) data))]
      ;merage all of them
      (merge data target-data-m nj-data))))



