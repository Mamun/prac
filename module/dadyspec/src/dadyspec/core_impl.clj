(ns dadyspec.core-impl
  (:require [clojure.spec :as s]))



(defn convert-property-to-def [m]
  (map (fn [[k v]]
         (list 'clojure.spec/def k v))
       m))


(defn add-postfix-to-key [k v]
  (if (namespace k)
    (keyword (str (namespace k) "/" (name k) v))
    (keyword (str (name k) v))))




;; or does not work correctly for unfrom core api


(defn create-ns-key [ns-key r]
  (let [w (if (namespace ns-key)
            (str (namespace ns-key) "." (name ns-key))
            (name ns-key))]
    (if (namespace r)
      (keyword (str w "." (namespace r) "/" (name r)))
      (keyword (str w "/" (name r))))))


(defn assoc-ns-key [ns-key m]
  (if (nil? m)
    {}
    (->> (map (fn [v]
                (create-ns-key ns-key v)) (keys m))
         (interleave (keys m))
         (apply assoc {})
         (clojure.set/rename-keys m))))


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
         (into #{} ))))


(defn model-template
  [fixed? qualified? k req opt]
  (let [w-un (keys-template req opt qualified?)
        w-un-set (get-key-set req opt qualified?)]
    ;;conform does not work with merge
    (if fixed?
      `((clojure.spec/def ~k (clojure.spec/merge ~w-un (clojure.spec/map-of ~w-un-set any?)) )
        (clojure.spec/def ~(add-postfix-to-key k "-list")
        (clojure.spec/coll-of ~k :kind vector?)))
      `((clojure.spec/def ~k ~w-un )
         (clojure.spec/def ~(add-postfix-to-key k "-list")
           (clojure.spec/coll-of ~k :kind vector?))))))



(defn model->spec
  [join-m [model-k model-v] & {:keys [fixed? qualified?]
                                :or {fixed? true
                                     qualified? true}}]
  (let [j (->> (get join-m model-k)
               (mapv #(nth % 2)))
        ;req-k (assoc-ns-key model-k (:req model-v))
        req-k (:req model-v);(assoc-ns-key model-k (:req model-v))
        opt-k (:opt model-v);(assoc-ns-key model-k (:opt model-v))
        ;opt-k (assoc-ns-key model-k (:opt model-v))
        w (convert-property-to-def (merge req-k opt-k))
        req-key (into [] (keys req-k))
        opt-key (into (or j []) (keys opt-k))]
    (->> (into w (model-template fixed? qualified? model-k req-key opt-key))
         (reverse))))


(defn update-key [[model-k model-v] & n-postfix]
  (let [;model-k (reduce add-postfix-to-key model-k n-postfix)
        v (as-> model-v m
                (if (:req m)
                  (update m :req (fn [w] (assoc-ns-key model-k w ) ))
                  m)
                (if (:opt m )
                  (update m :opt (fn [w] (assoc-ns-key model-k w ) ))
                  m))]
    [model-k v]))



(comment



  (let [r (fn [w] {:w w} )
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
            :always             (or (str n)))
    )

  )




(defn assoc-ns-join [base-ns-name [src rel dest]]
  (let [src (create-ns-key base-ns-name src)
        v (condp = rel
            :dadyspec.core/one-one (create-ns-key base-ns-name dest)
            :dadyspec.core/many-one (create-ns-key base-ns-name dest)
            :dadyspec.core/one-many (-> (create-ns-key base-ns-name dest)
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


