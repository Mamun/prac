(ns cljc.common
  (:require [clojure.string :as s]
            [clojure.walk :as w]
            ))


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


(defrecord Failure [error])


(defprotocol ComputationFailed
  (-failed? [self]))


(extend-protocol ComputationFailed
  #?(:cljs object
     :clj  Object) (-failed? [self] false)
  nil (-failed? [self] false)
  Failure (-failed? [self] self)
  #?(:cljs js/Error
     :clj  Exception) (-failed? [self] self))


(defn fail [error] (Failure. error))

(defn failed? [v]
  (-failed? v))


(defmacro try->>
  [expr & forms]
  (let [g (gensym)
        pstep (fn [step] `(if (failed? ~g) ~g (->> ~g ~step)))]
    `(let [~g ~expr
           ~@(interleave (repeat g) (map pstep forms))]
       ~g)))


(defmacro try->
  [expr & forms]
  (let [g (gensym)
        pstep (fn [step] `(if (failed? ~g) ~g (-> ~g ~step)))]
    `(let [~g ~expr
           ~@(interleave (repeat g) (map pstep forms))]
       ~g)))


(defn try!
  [form & v]
  (try
    (apply form v)
    (catch #?(:clj  Exception
              :cljs js/Error) e
      ;  (log/error e)
      (fail {:function form #_(:name (:meta (get-meta form)))
             :value    v
             :detail   e}))))


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



(defn xf-until
  [pred]
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result input]
       (if (pred input)
         (reduced input)
         (rf result input))))))


(defn comp-xf-until
  [& steps-coll]
  (->> (interleave steps-coll (repeat (xf-until failed?)))
       (cons (xf-until failed?))
       (apply comp)))


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



(defn- replace-mv
  [f1 m]
  (let [f (fn [[k v]] [k (f1 v)])]
    (into {} (map f m))))


(defn postwalk-replace-value-with
  "Recursively transforms all map keys from strings to keywords."
  {:added "1.1"}
  [f m]
  (w/postwalk (fn [x] (cond
                        (map? x)
                        (replace-mv f x)
                        (vector? x)
                        (mapv f x)
                        :else x)) m))


(defn keyword->str
  [v]
  (if (keyword? v)
    (name v)
    v))


(defn replace-mk
  [f1 m]
  ; (println "--" m)
  (let [f (fn [[k v]] [(f1 k) v])]
    (into {} (map f m))))


(defn postwalk-replace-key-with
  "Recursively transforms all map and first  vector keys from keywords to strings."
  {:added "1.1"}
  [f m]
  (w/postwalk (fn [x]
                (cond (map? x)
                      (replace-mk f x)
                      (vector? x)
                      (mapv f x)
                      :else x)) m))






(comment

  (replace-mk keyword->str [{:a 2}])

  (postwalk-replace-key-with keyword->str
                             [{:a 3}])

  (postwalk-replace-value-with [[:a :b]
                                [1 2]
                                [:a :b]
                                ])

  )




(defn is-include? [filter-v w]
  (reduce (fn [acc v]
            (if (or (clojure.string/includes? (clojure.string/lower-case (str (first w)))
                                              (clojure.string/lower-case v))
                    (clojure.string/includes? (clojure.string/lower-case (str (second w)))
                                              (clojure.string/lower-case v)))
              (reduced true)
              acc))
          false
          (clojure.string/split filter-v #" ")))


(defn postwalk-filter
  "remove pairs of key-value that has nil value from a (possibly nested) map. also transform map to nil if all of its value are nil"
  [filter-v nm]
  (if (or (nil? filter-v)
          (empty? filter-v))
    nm
    (w/postwalk
      (fn [el]
        (if (map? el)
          (into {} (filter (partial is-include? filter-v) el))
          el))
      nm)))


(defn postwalk-remove-with
  [f nm]
  (w/postwalk
    (fn [el]
      (if (map? el)
        (let [m (into {} (remove (comp f second) el))]
          (when (seq m)
            m))
        el))
    nm))


(defn postwalk-remove-nils
  "remove pairs of key-value that has nil value from a (possibly nested) map. also transform map to nil if all of its value are nil"
  [nm]
  (postwalk-remove-with nil? nm))




(comment

  (postwalk-filter "hello 3" {:hello 2 :check 3 :5 5 :check3 4})

  )