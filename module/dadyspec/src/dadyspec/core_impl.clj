(ns dadyspec.core-impl
  (:require [clojure.spec :as s]))




(defn add-postfix-to-key [k v]
  (if (namespace k)
    (keyword (str (namespace k) "/" (name k) v))
    (keyword (str (name k) v))))


;; or does not work correctly for unfrom core api
(defn as-ns-keyword [ns-key r]
  (let [w (if (namespace ns-key)
            (str (namespace ns-key) "." (name ns-key))
            (name ns-key))]
    (if (namespace r)
      (keyword (str w "." (namespace r) "/" (name r)))
      (keyword (str w "/" (name r))))))




(defn update-model-key-m [ns-key m]
  (if (nil? m)
    {}
    (->> (map (fn [v]
                (as-ns-keyword ns-key v)) (keys m))
         (interleave (keys m))
         (apply assoc {})
         (clojure.set/rename-keys m))))


(defn update-model-key [[model-k model-v] & ns-key ]
  (let [model-k (-> (reduce add-postfix-to-key ns-key)
                    (as-ns-keyword model-k) )
        v (as-> model-v m
                (if (:req m)
                  (update m :req (fn [w] (update-model-key-m model-k w)))
                  m)
                (if (:opt m)
                  (update m :opt (fn [w] (update-model-key-m model-k w)))
                  m))]
    [model-k v]))






(defn keys-template [req opt qualified?]
  (let [req-key (if qualified? :req :req-un)
        opt-key (if qualified? :opt :opt-un)
        w ['clojure.spec/keys]
        w (if (not-empty req)
            (into w [req-key req])
            w)
        w (if (not-empty opt)
            (into w [opt-key opt])
            w)
        w (apply list w)]
    w))

(defn get-key-set [req opt qualified?]
  (if qualified?
    (into #{} (into req opt))
    (->> (into req opt)
         (mapv name)
         (mapv keyword)
         (into #{}))))



(defn model-template
  [[model-k {:keys [req opt]}] join-m & {:keys [fixed? qualified?]
                                         :or   {fixed?     true
                                                qualified? true}} ]
  (let [j (->> (get join-m model-k)
               (mapv #(nth % 2)))
        req (into [] (keys req))
        opt (into (or j []) (keys opt))

        w-un     (keys-template req opt qualified?)
        w-un-set (get-key-set req opt qualified?)]
    ;;conform does not work with merge
    (if fixed?
      `((clojure.spec/def ~model-k (clojure.spec/merge ~w-un (clojure.spec/map-of ~w-un-set any?)))
        (clojure.spec/def ~(add-postfix-to-key model-k "-list")
           (clojure.spec/coll-of ~model-k :kind vector?)))
      `((clojure.spec/def ~model-k ~w-un)
         (clojure.spec/def ~(add-postfix-to-key model-k "-list")
           (clojure.spec/coll-of ~model-k :kind vector?))))))


(defn convert-property-to-def [{:keys [req opt]}]
  (->> (merge req opt)
       (map (fn [[k v]]
              `(clojure.spec/def ~k ~v)))))


(comment
  (->> (map (fn [w] ( update-model-key  w :app "-ex") ) {:student {:req {:di :id
                                                                         :name :na}}} )
       (map (fn [[k v]] (convert-property-to-def v) ) )
       )

  )



#_(defn model->spec
  [join-m cm & {:keys [fixed? qualified?]
                               :or   {fixed?     true
                                      qualified? true}}]
  (let [[model-k model-v] cm
        w (convert-property-to-def model-v)]

    (->> (into w (model-template cm join-m :fixed? fixed? :qualified? qualified?))
         (reverse))))





(comment



  (let [r (fn [w] {:w w})
        w (as-> {:c 1 :b 2} m
                (if (:a m)
                  (update m :a + 10)
                  m)
                (if (:c m)
                  (update m :b + 10)
                  m)

                )
        ]
    w
    )




  (let [m {:a 2}]
    (cond-> m
            value (assoc :b 3)
            :always m
            )

    )

  (defn divisible-by? [divisor number]
    (zero? (mod number divisor)))

  (let [n 3]
    (cond-> nil
            (divisible-by? 3 n) (str "Fizz")
            (divisible-by? 5 n) (str "Buzz")
            :always (or (str n)))
    )

  )




(defn assoc-ns-join [base-ns-name [src rel dest]]
  (let [src (as-ns-keyword base-ns-name src)
        v (condp = rel
            :dadyspec.core/one-one (as-ns-keyword base-ns-name dest)
            :dadyspec.core/many-one (as-ns-keyword base-ns-name dest)
            :dadyspec.core/one-many (-> (as-ns-keyword base-ns-name dest)
                                        (add-postfix-to-key "-list")))]
    [src rel v]))

(defn format-join [base-ns-name join-list]
  (->> join-list
       (mapv (partial assoc-ns-join base-ns-name))
       (group-by first)))





#_(defn reverse-join [[src rel dest]]
    (condp = rel
      :1-n [dest :n-1 src]
      :n-1 [dest :1-n src]
      [src rel dest]))


