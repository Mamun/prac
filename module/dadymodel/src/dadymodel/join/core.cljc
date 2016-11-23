(ns dadymodel.join.core
  (:require [dadymodel.join.util :as p]))


(defn- join-acc-fn [acc j]
  (let [[s _ _ d _] j
        d-n (p/target-key-identifier j)]
    (if-let [v (get acc d)]
      (assoc-in acc (conj s d-n) v)
      acc)))


(defn do-join-impl
  [data join-coll]
  (if (empty? join-coll)
    data
    (let [j-coll (p/replace-source-entity-path join-coll data)
          target-name (distinct (mapv #(nth % 3) j-coll))]
      (apply dissoc
             (reduce join-acc-fn data j-coll)
             target-name))))



(defn- dis-join-acc-fn [acc j]
  (let [[s _ _ d _] j
        d-n (p/target-key-identifier j)]
    (if-let [w (get-in acc (conj s d-n))]
      (-> (assoc acc d w)
          (update-in s dissoc d-n))
      (update-in acc s dissoc d-n))))



(defn do-disjoin-impl
  "Assoc relation key and dis-join relation model "
  [data join-coll]
  (if (empty? join-coll)
    data
    (->> (p/replace-source-entity-path join-coll data)
         (reduce dis-join-acc-fn data))))







#_(comment

    (defn group-by-value
      [k v]
      (let [fv (first v)]
        (cond
          (map? v) (if (get v k)
                     {(get v k) v}
                     nil)
          (map? fv) (group-by k v)
          (vector? fv) (let [index (.indexOf fv k)]
                         (->> (group-by #(get %1 index) (rest v))
                              (reduce (fn [acc [k w]]
                                        (assoc acc k (reduce conj [fv] w))
                                        ) {})))
          :else v)))



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
        (if (= :dadymodel.core/rel-n-n rel)
          (get-in target-rel-data-m [d nst s-value])
          (get-in target-rel-data-m [d dt s-value]))))


    (defn assoc-to-source-entity-batch
      [target-rel-data-m data-m j-coll]
      (reduce (fn [acc [s _ rel d :as j]]
                (let [d-level (if (or (= rel :dadymodel.core/rel-n-n)
                                      (= rel :dadymodel.core/rel-1-n)
                                      )
                                (keyword (str (name d) "-list"))
                                d)]
                  (->> (get-target-relational-key-value target-rel-data-m data-m j)
                       (assoc-in acc (conj s d-level))))
                ) data-m j-coll))


    (defn group-by-target-entity-key-one
      ""
      [[_ _ rel d dt [n nst _]] data-m]
      (if (= rel :dadymodel.core/rel-n-n)
        {d {nst (group-by-value nst (get data-m n))}}
        {d {dt (group-by-value dt (get data-m d))}}))



    (defn group-by-target-entity-key-batch
      [j-coll data]
      (reduce (fn [acc j]
                (->> (group-by-target-entity-key-one j data)
                     (merge-with merge acc))
                ) {} j-coll))



    (defn do-join-impl2
      " do data join "
      [data j-coll]
      (if (is-join-skip? j-coll data)
        data
        (-> j-coll
            (group-by-target-entity-key-batch data)
            (assoc-to-source-entity-batch data (p/replace-source-entity-path j-coll data))
            (select-root j-coll))))

    )