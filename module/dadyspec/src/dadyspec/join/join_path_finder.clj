(ns dadyspec.join.join-path-finder)


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


(defn as-sequential
  [input]
  (when name
    (if (sequential? input)
      input
      [input])))



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


#_(defn get-source-relational-key-value
  [j-coll data-m]
  (reduce (fn [acc j1]
            (let [[s st rel _ dt [_ sdt _]] j1
                  w (keys (group-by-value st (s data-m)))]
              (if (= rel :dadyspec.core/rel-n-n)
                (merge acc {sdt w})
                (merge acc {dt w})))
            ) {} j-coll))






(defn replace-source-entity-path
  [j-coll data-m]
  (for [[g-key coll] (group-by first j-coll)
        j coll
        em (get-path data-m g-key)]
    (assoc j 0 em)))


(defn replace-target-entity-path
  [j-coll data-m]
  (for [[s-tab-id _ _ d-tab-id _ :as j] j-coll
        :let [w (conj s-tab-id d-tab-id)]
        :when (get-in data-m w)
        c (get-path data-m [s-tab-id] d-tab-id)]
    (assoc j 3 c)))


(defn target-key-identifier [s-tab s-id join-key d-tab d-id [r-tab r-id r-id2] :as j]
  (condp = join-key
    :dadyspec.core/rel-1-1 d-tab
    :dadyspec.core/rel-1-n (keyword (str (name d-tab)  "-list"))
    :dadyspec.core/rel-n-1 d-tab
    :dadyspec.core/rel-n-n (keyword (str (name d-tab) "-list"))
    j))


(defn rename-join-key [join-coll]
  (mapv (fn [[s-tab s-id join-key d-tab d-id [r-tab r-id r-id2] :as j]]

          (condp = join-key
            :dadyspec.core/rel-1-1 [s-tab s-id join-key d-tab d-id]
            :dadyspec.core/rel-1-n [s-tab s-id join-key (keyword (str (name d-tab)  "-list")) d-id]
            :dadyspec.core/rel-n-1 [s-tab s-id :dadyspec.core/rel-1-n d-tab d-id]
            :dadyspec.core/rel-n-n [s-tab s-id :dadyspec.core/rel-n-n (keyword (str (name d-tab) "-list")) d-id [r-tab r-id2 r-id]]
            j)
          ) join-coll))
