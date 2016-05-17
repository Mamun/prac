(ns dady.common
  (:require [clojure.string :as s]))


(def as-lower-case-keyword (comp keyword s/lower-case name))


(defn as-sequential
  [input]
  (when name
    (if (sequential? input)
      input
      [input])))


(defn as-keyword
  [v-str]
  (if (and v-str
           (string? v-str)
           (not (keyword? v-str)))
    (keyword v-str)
    v-str))


(defn as-keyword-batch
  [w]
  (if w
    (if (sequential? w)
      (mapv as-keyword (remove nil? w))
      (keyword w))))


(defn as-string
  [v]
  (if (string? v)
    v
    (str v)))



(defn select-values
  [m keyseq]
  (if-not (map? m) m (mapv #(%1 m) keyseq)))


(defn last-index
  [seq]
  (.indexOf seq (last seq)))


(defn replace-last-in-vector
  [seq v]
  (let [index (last-index seq)]
    (if (<= 0 index)
      (assoc seq index v)
      seq)))






(defn merge-with-key-type
  [f & maps]
  (when (some identity maps)
    (let [merge-entry (fn [m e]
                        (let [k (key e) v (val e)]
                          (if (contains? m k)
                            (assoc m k (f k (get m k) v))
                            (assoc m k v))))
          acc-fn (fn [acc v]
                   (reduce merge-entry (or acc {}) (seq v)))]
      (reduce acc-fn maps))))


(defn seql-contains?
  [range coll v]
  (let [v1 (take range v)]
    (some #(= v1
              (take range %)
              ) coll)))


(defn xf-distinct-with [pred?]
  (fn [rf]
    (let [seen (volatile! #{})]
      (fn
        ([] (rf))                                           ;; init arity
        ([result] (rf result))                              ;; completion arity
        ([result input]                                     ;; reduction arity
         (if (pred? @seen input)
           result
           (do (vswap! seen conj input)
               (rf result input))))))))


(defn distinct-with-range
  [range coll]
  (into [] (xf-distinct-with (partial seql-contains? range)) coll))


(defn acc-with-range
  [range acc v]
  (if (seql-contains? range acc v)
    acc
    (conj acc v)))


(defn concat-with-range
  [range old-coll new-coll]
  (->> old-coll
       (reduce #(acc-with-range range %1 %2) new-coll)
       (into [])))


(defn concat-distinct-with-range
  [range old-coll new-coll]
  (concat-with-range
    range
    (distinct-with-range range old-coll)
    (distinct-with-range range new-coll)))



(defn contain-all?
  [coll key]
  (reduce (fn [acc v]
            (if-not (= v key)
              (reduced nil)
              acc)
            ) key coll))



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







#_(defn xf-assoc-key-coll
    [k coll]
    (fn [rf]
      (let [coll2 (volatile! coll)]
        (fn
          ([] (rf))                                         ;; init arity
          ([result] (rf result))                            ;; completion arity
          ([result input]                                   ;; reduction arity
           (let [[f & r] @coll2]
             (vreset! coll2 r)
             (rf result (assoc input k f))
             ))))))


(defn xf-skip-type
  [pred]
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result input]
       (if (pred input)
         (conj result input)
         (rf result input))))))





#_(defn xf-take-until
    [pred]
    (fn [rf]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (if (pred input)
           (reduced (rf result input))
           (rf result input))))))




;;;; Path finder











(comment

  (postwalk-filter "hello 3" {:hello 2 :check 3 :5 5 :check3 4})

  )