(ns dady.fail)


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


(comment

  (-> (map (fn [v] (assoc v :a 1)))
      (comp-xf-until)
      (transduce conj [(fail "Hello") {:b 1}])

      )

  )
