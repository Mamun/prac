(ns dadyspec.join.disjoin-impl
  (:require [clojure.walk :as w]
            [dadyspec.join.util :as p]))


(defn group-by-target-entity-one
  [data j]


  (if (= :dadyspec.core/rel-n-n (nth j 2))
    (let [[st stc _ dt dtc [rdt s d]] j]
      {rdt [{s (get-in data (conj st stc))
             d (get-in data (conj dt dtc))}]})
    (let [[s _ _ d] j]
      (if-let [tdata (get-in data (conj s d))]
        {d (p/as-sequential tdata)}
        {d []}))))


(defn group-by-target-entity-batch
  [join-coll data]
  (->> join-coll
       (map #(group-by-target-entity-one data %))
       (apply merge-with (comp vec distinct concat))))


(defn replace-target-entity-path
  [data-m j-coll]
  ;(println "data:" data-m)
  ;(println "coll: " j-coll)
  (for [[s-tab-id _ _ d-tab-id _ :as j] j-coll
        :let [w (conj s-tab-id d-tab-id)]
        :when (get-in data-m w)
        c (p/get-path data-m [s-tab-id] d-tab-id)]
    (assoc j 3 c)))


(defn assoc-n-n-join-key [data join-coll]
  (let [n-join (filter (fn [[_ _ rel]]
                         (if (= rel :dadyspec.core/rel-n-n)
                           true
                           false)
                         ) join-coll)]
    (if (empty? n-join)
      {}
      (->
           (replace-target-entity-path data n-join)
           (group-by-target-entity-batch data)
           )
          )))


(defn assoc-1-join-key [data join-coll]
  (let [join (w/postwalk (fn [w]
                           (if (= :dadyspec.core/rel-n-n w)
                             :dadyspec.core/rel-1-n
                             w)
                           ) join-coll)
        acc-fn (fn [data [s-tab s _ d-tab d]]
                 (let [s-ks (conj s-tab s)
                       d-ks (conj d-tab d)]
                   (if (map? (get-in data d-tab))
                     (if-let [w (get-in data s-ks)]
                       (assoc-in data d-ks w)
                       data)
                     data)))]
    (->> (replace-target-entity-path data join)
         (reduce acc-fn data)
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
    (let [join-coll (-> (p/rename-join-key join-coll)
                        (p/replace-source-entity-path data))
          nj-data (assoc-n-n-join-key data join-coll)

          target-data-m (assoc-1-join-key data join-coll)


          data1 (update-target-data data join-coll target-data-m)]
      (merge data1 nj-data))))



(defn do-disjoin
  "Assoc relation key and dis-join relation model "
  [data join-coll]
  (if (empty? join-coll)
    data
    (->> (p/replace-source-entity-path join-coll data)
         (reduce (fn [acc j]
                   (let [[s _ _ d _] j
                         d-n (p/target-key-identifier j)]
                     (if-let [w (get-in data (conj s d-n))]
                       (-> (assoc acc d w)
                           (update-in s dissoc d-n))
                       (update-in acc s dissoc d-n)))
                   ) data))))





(comment


  (let [join [[:tab :id :dadyspec.core/rel-n-n :tab1 :tab1-id [:ntab :tab-id :tab1-id]]]
        data {:tab {:id   100
                    :tab1-list [{:tab-id 10}
                                {:tab-id 101}]}}
        ]
    (clojure.pprint/pprint (assoc-join-key data join))
    ;(do-disjoin (assoc-join-key data join) join)
    )

  ;(get-in {:a 3} [:v])

  (let [j [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]]
        ;j (rename-joi-key j)

        data {:dept
              {:id           0,
               :name         "",
               :student-list [{:name "asdf"}]
               :note         ""}}]
    (assoc-join-key data j)
    ;(do-disjoin (assoc-join-key data j) j)

    )

  )
