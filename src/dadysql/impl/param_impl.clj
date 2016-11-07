(ns dadysql.impl.param-impl
  (:require [dady.fail :as f]
            [clojure.walk :as w]
            [clojure.spec :as s]
            [dady.spec-util :as ds]
            [dady.spec-generator :as sg]
            [dadysql.impl.join-impl :as ji]
            [dadysql.impl.util :as ccu]))


(defn temp-generator [_]
  -1)



(defn- assoc-param-path [m param-m root-path]
  (->> (keys (:dadysql.core/default-param m))
       (map (fn [w] (first (ccu/get-path param-m root-path w))))))


(defmulti param-paths (fn [input-format _ _] input-format))


(defmethod param-paths
  :dadysql.core/format-nested
  [_ [root-m & child-m] param-m]
  (let [rp (ccu/get-path param-m (get root-m :dadysql.core/model))
        r (->> (assoc-param-path root-m param-m rp)
               (assoc root-m :dadysql.core/param-path))]
    (->> child-m
         (mapv (fn [v]
                 (->> (ccu/get-path param-m rp (:dadysql.core/model v))
                      (assoc-param-path v param-m)
                      (assoc v :dadysql.core/param-path))))
         (cons r))))



(defmethod param-paths
  :dadysql.core/format-map
  [_ tm-coll param-m]
  (->> (map :dadysql.core/default-param tm-coll)
       (apply merge)
       (keys)
       (map (fn [w] (first (ccu/get-path param-m w))))
       (repeat)
       (mapv (fn [m p]
               (assoc m :dadysql.core/param-path p)
               ) tm-coll)))




(comment

  (->> (interleave (mapv (fn [v] {:v v}) [1 2]) [{:a 3} {:a 4}])
       (split-at 2)
       (map (fn [v] (apply merge v)))
       )


  (let [coll [{:dadysql.core/default-param {:id :a}}
              {:dadysql.core/default-param {:id3 :b}}]
        actual-result (param-paths :dadysql.core/format-map coll {:id2 1})]
    actual-result
    )

  )


(defn param-exec2 [acc-input m]
  (reduce (fn [acc-input path]
            (if (get-in acc-input path)
              acc-input
              (let [p-exec (get-in m [:dadysql.core/default-param (last path)])
                    p-value (get-in acc-input (into [] (butlast path)))
                    new-in (p-exec p-value)
                    new-in (if (fn? new-in)
                             (new-in)
                             new-in
                             )
                    ]
                (if (f/failed? new-in)
                  (reduced new-in)
                  (assoc-in acc-input path new-in))))
            ) acc-input (:dadysql.core/param-path m)))




(defn param-exec [tm-coll rinput input-format]
  (let [param-paths (param-paths input-format tm-coll rinput)]
    (reduce (fn [acc-input path]
              (param-exec2 acc-input path)
              ) rinput param-paths)))



(defn assoc-generator [tm-coll gen]
  (mapv (fn [m]
          (if (:dadysql.core/default-param m)
            (update-in m [:dadysql.core/default-param]
                       (fn [w]
                         (into {} (mapv (fn [[k f]] {k (f gen)}) w))))
            m)
          ) tm-coll))




(comment

  (let [coll [{:dadysql.core/default-param {:transaction_id (fn [w] (:id w))},
               :dadysql.core/model         :employee}
              {:dadysql.core/default-param {:transaction_id (fn [w] (:id w))},
               :dadysql.core/model         :employee}]
        input {:id 2}

        expected-result {:id 2 :transaction_id 2}
        actual-result (param-exec coll input :dadysql.core/format-map identity)]

    actual-result
    )


  (let [coll [{:dadysql.core/default-param {:transaction_id (fn [w] (:id w))},
               :dadysql.core/model         :employee}
              {:dadysql.core/model :employee}
              {:dadysql.core/default-param {:transaction_id (fn [w] (:id w))},
               :dadysql.core/model         :employee}]
        input {:employee {:id 2}}
        expected-result {:employee {:id 2, :transaction_id 3}}
        actual-result (param-exec coll input :dadysql.core/format-nested (fn [_] 3))]
    actual-result)

  )


(defn disptach-input-format [req-m]
  (if (and
        (= :dadysql.core/op-push! (:dadysql.core/op req-m))
        (or (:dadysql.core/group req-m)
            (sequential? (:dadysql.core/name req-m))))
    :dadysql.core/format-nested
    :dadysql.core/format-map))


(defmulti bind-param (fn [_ request-m] (disptach-input-format request-m)))


(defmethod bind-param :dadysql.core/format-map
  [tm-coll request-m]
  (let [input (:dadysql.core/param request-m)
        input (param-exec tm-coll input :dadysql.core/format-map)]
    (if (f/failed? input)
      input
      (mapv (fn [m] (assoc m :dadysql.core/param input)) tm-coll))))


(defmethod bind-param :dadysql.core/format-nested
  [tm-coll request-m]
  (let [input (:dadysql.core/param request-m)
        input (param-exec tm-coll input :dadysql.core/format-nested)
        input (f/try-> input
                       (ji/do-disjoin (get-in tm-coll [0 :dadysql.core/join])))]
    (if (f/failed? input)
      input
      (mapv (fn [m] (assoc m :dadysql.core/param ((:dadysql.core/model m) input))) tm-coll))))
;;;;;;;;;;;;;;;;;


(defn validate-param-spec [tm-coll req-m]
  (let [param-spec (condp = (:dadysql.core/op req-m)
                     :dadysql.core/op-push
                     (-> (map :dadysql.core/spec tm-coll)
                         (remove nil?)
                         (sg/join-spec))
                     (-> (map :dadysql.core/spec tm-coll)
                         (doall)
                         (sg/union-spec)))]
    (if (and (nil? param-spec)
             (empty? param-spec) )
      tm-coll
      (if (s/valid? (eval param-spec)  (:dadysql.core/param req-m))
        tm-coll
        (f/fail (s/explain-str (eval param-spec)  (:dadysql.core/param req-m)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;, Reader util ;;;;;;;;;


(defn map-core-fn
  [w]
  (let [w1 (eval w) ]
    (fn [_]
      (fn [m]
        (w1 m)))))


(defn map-seq-fn
  [w]
  (fn [caller-fn]
    (fn [_]
      (caller-fn {:dadysql.core/name w
                  :dadysql.core/op   :dadysql.core/op-db-seq}))))



(defn convert-param-fn [v]
  (if (empty? v)
    v
    (->> (vals v)
         (w/postwalk (fn [w]
                       (if (keyword? w)
                         (list 'get-in 'm [w])
                         w)))
         (mapv (fn [w] (list 'fn ['m] w)))
         (mapv map-core-fn)
         (interleave (keys v))
         (apply assoc {}))))


(defn convert-param-fn-keyword [m]
  (if (empty? m)
    m
    (do
      (->> (vals m)
           (mapv map-seq-fn)
           (interleave (keys m))
           (apply assoc {})))))



(defn convert-param-t [v]
  (let [[f f2] (split-with (fn [[k r]]
                             (if (keyword? r)
                               true false ) ) v)]
    (merge
      (convert-param-fn-keyword (into {} f))
      (convert-param-fn (into {} f2)))))



(comment

  (let [p {:b 1}
        w (convert-param-t {:b :a :a '(constantly 3)  })
        fn1 (fn [r] (do
                      (println "--form in pout " r)
                      3)  )
        w1 (into {} (mapv (fn [[k f]] {k (f fn1)}  ) w ))
        ]
    ( (:a w1) p)

    ( (:b w1) p)

    )


  )
