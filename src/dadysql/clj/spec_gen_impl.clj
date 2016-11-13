(ns dadysql.clj.spec-gen-impl
  (:require [clojure.spec :as s]))


(defn convert-property-to-def [m]
  (map (fn [[k v]]
         (list 'clojure.spec/def k v))
       m))


(defn add-postfix-to-key [k v]
  (if (namespace k)
    (keyword (str (namespace k) "/" (name k) v))
    (keyword (str (name k) v))))


(defn build-key [req opt qualified?]
  (let [req-key (if qualified? :req :req-un )
        opt-key (if qualified? :opt :opt-un )
        w ['clojure.spec/keys]
        w (if (not-empty req)
            (into w [req-key req])
            w)
        w (if (not-empty opt)
            (into w [opt-key opt])
            w)
        w (apply list w)]
    w))


;; or does not work correctly for unfrom core api
(defn convert-model-tp-def
  [k req opt]
  (let [                                                    ;w (build-key req opt true )
        w-un (build-key req opt false )]
    `((clojure.spec/def ~k ~w-un
        #_(clojure.spec/or :qualified ~w
                         :unqualified ~w-un))
       (clojure.spec/def ~(add-postfix-to-key k "-list")
         (clojure.spec/coll-of ~k :kind vector?)))))


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


(defn model->spec2 [join-m [model-k model-v]]
  (let [j (->>  (get join-m model-k )
                (mapv #(nth % 2)))
        ;   _ (println ".." j)
        req-k (assoc-ns-key model-k (:req model-v))
        opt-k (assoc-ns-key model-k (:opt model-v))
        w (convert-property-to-def (merge req-k opt-k))
        rk (into [] (keys req-k))
        ok (into (or j [])  (keys opt-k))]
    (->> (into w (convert-model-tp-def model-k rk ok ))
         (reverse))))



(defn assoc-ns-join [base-ns-name [src rel dest]]
  (let [src (create-ns-key base-ns-name src)
        v (condp = rel
            :1-1 (create-ns-key base-ns-name dest)
            :n-1 (create-ns-key base-ns-name dest)
            :1-n (-> (create-ns-key base-ns-name dest)
                     (add-postfix-to-key "-list")))]
    [src rel v]))


#_(defn reverse-join [[src rel dest]]
  (condp = rel
    :1-n [dest :n-1 src]
    :n-1 [dest :1-n src]
    [src rel dest]))


(defn gen-spec-impl [conformer-fn base-ns-name join-list m ]
  (let [j-m (->> join-list
                 ;(map reverse-join )
                 ;(concat join-list)
                 ;(distinct)
                 (mapv (partial assoc-ns-join base-ns-name) )
                 (group-by first))
        f (->> m
               (assoc-ns-key base-ns-name)
               (map (partial model->spec2 j-m))
               (apply concat))
        w (->> m
               (clojure.walk/postwalk conformer-fn)
               (assoc-ns-key (add-postfix-to-key base-ns-name "-ex"))
               (map (partial model->spec2 j-m))
               (apply concat))]
    (into w (reverse f))))