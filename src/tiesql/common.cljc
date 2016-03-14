(ns tiesql.common
  (:require [clojure.string :as s]
            [clojure.walk :as w]))


(defonce global-key :_global_)
(defonce module-key :_module_)
(defonce process-context-key :process-context)
(defonce reserve-name-key :reserve-name)
(defonce file-name-key :file-name)
(defonce file-reload-key :file-reload)
(defonce ds-key :datasource)
(defonce ds-conn-key :datasource-conn)
(defonce tx-prop :tx-prop)


(defonce name-key :name)
(defonce column-key :column)
(defonce doc-key :doc)
(defonce model-key :model)
(defonce skip-key :skip)
(defonce timeout-key :timeout)
(defonce group-key :group)
(defonce index :index)
(defonce sql-key :sql)


;(def root-meta :meta)
(defonce extend-meta-key :extend)

;(def meta-with-extend #{root-meta extend-meta-key})

(def nested-map-format :nested)
(def nested-array-format :nested-array)
(def nested-join-format :nested-join)
(def map-format :map)
(def array-format :array)
(def value-format :value)


(defonce output-key :output)
(defonce input-key :input)


(defonce result-key :result)
(defonce result-array-key :array)
(defonce result-single-key :single)

;(def in-type :input-type)
;(def out-type :output-type)


(defonce param-key :params)
(defonce param-ref-con-key :ref-con)
(defonce param-ref-key :ref-key)
(defonce param-ref-fn-key :ref-fn-key)
(defonce param-ref-gen-key :ref-gen)


(defonce validation-key :validation)
(defonce validation-type-key :type)
(defonce validation-range-key :range)
(defonce validation-contain-key :contain)


(defonce join-key :join)
(defonce join-1-1-key :1-1)
(defonce join-1-n-key :1-n)
(defonce join-n-1-key :n-1)
(defonce join-n-n-key :n-n)


(defonce dml-key :dml-type)
(defonce dml-select-key :select)
(defonce dml-insert-key :insert)
(defonce dml-update-key :update)
(defonce dml-delete-key :delete)
(defonce dml-call-key :call)



(defonce commit-key :commit)
(defonce commit-all-key :all)
(defonce commit-any-key :any)
(defonce commit-none-key :none)


;(defonce error-key :error)
(defonce exec-time-total-key :exec-total-time)
(defonce exec-time-start-key :exec-start-time)
(defonce query-exception-key :query-exception)


(def all-oformat #{nested-join-format nested-map-format nested-array-format
                   map-format array-format value-format})
(def all-pformat #{nested-map-format map-format})




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


(defn as-string
  [v]
  (if (string? v)
    v
    (str v)))


(defn stringify-keys2
  "Recursively transforms all map and first  vector keys from keywords to strings."
  {:added "1.1"}
  [m]
  (let [f (fn [[k v]] (if (keyword? k) [(name k) v] [k v]))
        fv (fn [v] (update-in v [0] (fn [w]
                                      (if-not (vector? w)
                                        w
                                        (mapv #(name %) w)))))]
    ;; only apply to maps and vector
    (w/postwalk (fn [x]
                  (cond (map? x)
                        (into {} (map f x))
                        (vector? x) (fv x)
                        :else x)) m)))



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








(defn validate-input!
  [{:keys [gname name params oformat pformat] :as request-m}]
  (cond
    (and (nil? name)
         (nil? gname))
    (fail "Need value either name or gname")
    (and (not (nil? gname))
         (not (keyword? gname)))
    (fail "gname will be keyword")
    (and (not (nil? name))
         (not (sequential? name))
         (not (keyword? name)))
    (fail "name will be keyword")
    (and
      (sequential? name)
      (not (every? keyword? name)))
    (fail "name will be sequence of keyword")
    (and                                                    ;(= map-format out-format)
      (sequential? name)
      (contains? #{map-format array-format value-format} oformat))
    (fail #?(:clj  (format "only one name keyword is allowed for %s format " oformat)
             :cljs "Only one name keyword is allowed"))
    (and
      (not (nil? params))
      (not (map? params)))
    (fail "params will be map format ")
    (and
      (not (nil? pformat))
      (not (contains? all-pformat pformat)))
    (fail #?(:clj  (format "pformat is not correct, it will be %s ", (str all-pformat))
             :cljs "pformat is not correct"))
    (and
      (not (nil? oformat))
      (not (contains? all-oformat oformat)))
    (fail #?(:clj  (format "oformat is not correct, it will be %s" (str all-oformat))
             :cljs "oformat is not correct"))
    :else
    request-m))
