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

;(concat [ 1 2 3] [ 3 4 5])
;(map first (partition 2 [1 2 3 4]) )

(defn- assoc-param-path [m param-m root-path]
  (->> (:dadysql.core/default-param m)
       (partition 2)
       (map first)
       (map (fn [w] {w (first (ccu/get-path param-m root-path w))}))
       (into {})))


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
       (apply concat)
       (partition 2)
       (map first)
       (map (fn [w] {w (first (ccu/get-path param-m w))}))
       (into {})
       (repeat)
       (mapv (fn [m p]
               (assoc m :dadysql.core/param-path p)
               ) tm-coll)))




(comment

  (->> (interleave (mapv (fn [v] {:v v}) [1 2]) [{:a 3} {:a 4}])
       (split-at 2)
       (map (fn [v] (apply merge v)))
       )


  (let [coll [{:dadysql.core/default-param [:id :a]}
              {:dadysql.core/default-param [:id2 :b]}]
        actual-result (param-paths :dadysql.core/format-map coll {:id2 1})]
    actual-result
    )

  )


(defn param-exec2 [acc-input m]
  (let [path-m (:dadysql.core/param-path m)]
    (->> (:dadysql.core/default-param m)
         (partition 2)
         (reduce (fn [acc-input [k f]]
                   (if (get-in acc-input (get path-m k))
                     acc-input
                     (let [p (into [] (butlast (get path-m k)))
                           v (get-in acc-input p)
                           new-in (f v)]
                       (if (f/failed? new-in)
                         (reduced new-in)
                         (assoc-in acc-input (get path-m k) new-in))))
                   ) acc-input))))



(defn param-exec [tm-coll rinput input-format]
  (let [param-paths (param-paths input-format tm-coll rinput)]
    (reduce (fn [acc-input path]
              (let [r (param-exec2 acc-input path)]
                (if (f/failed? r)
                  (reduced r)
                  r))
              ) rinput param-paths)))


(defn assoc-temp-gen [w gen]
  (->> (partition 2 w)
       (mapv (fn [[k f]] [k (f gen)]))
       (flatten)))


(defn assoc-generator [tm-coll gen]
  (mapv (fn [m]
          (if (:dadysql.core/default-param m)
            (update-in m [:dadysql.core/default-param] assoc-temp-gen gen)
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
             (empty? param-spec))
      tm-coll
      (if (s/valid? (eval param-spec) (:dadysql.core/param req-m))
        tm-coll
        (f/fail (s/explain-str (eval param-spec) (:dadysql.core/param req-m)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;, Reader util ;;;;;;;;;

(defmulti map-param-fn (fn [v] (type v)))

(defmethod map-param-fn
  :default
  [w]
  (fn [_]
    (fn [_]
      w)))



(defmethod map-param-fn
  clojure.lang.PersistentList
  [v]
  (let [w1 (->> v
                (w/postwalk (fn [w]
                              (if (keyword? w)
                                (list 'get-in 'm [w])
                                w)))
                (list 'fn ['m])
                (eval))]
    (fn [_]
      (fn [m]
        (w1 m)))))



(defmethod map-param-fn
  clojure.lang.Keyword
  [w]
  (fn [caller-fn]
    (fn [_]
      (caller-fn {:dadysql.core/name w
                  :dadysql.core/op   :dadysql.core/op-db-seq}))))


;(keyword? :As)
;(type :keyword)
;(type (fn [t]))


(defn convert-param-t [w]
  (->> (partition 2 w)
       (mapv (fn [[k v]]
               [k (map-param-fn v)]))
       (flatten)))



(comment

  (let [{a true b false}
        (group-by (fn [[k v]] (keyword? v)) {:id inc :a :b})]
    b)

  (clojure.pprint/pprint
    (convert-param-t [:a '(constantly 3) :b :a])
    )


  (let [p {:b 1}
        fn1 (fn [r] (do
                      (println "--form in pout " r)
                      3))
        w (-> (convert-param-t [:a '(constantly 3) :b :a])
              (assoc-temp-gen fn1))]

    (reduce (fn [acc [k f]]
              (assoc acc k (f acc))
              ) p (partition 2 w))

    )

  )
