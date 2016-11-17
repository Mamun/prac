(ns dadyspec.core-impl
  (:require [clojure.spec :as s]
            [dadyspec.util :as u]))


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

(defn- get-key-set [req opt qualified?]
  (if qualified?
    (into #{} (into req opt))
    (->> (into req opt)
         (mapv name)
         (mapv keyword)
         (into #{}))))



(defn model-template
  [model-k req opt {:keys [fixed? qualified?]
                    :or   {fixed?     true
                           qualified? true}}]
  (let [w-un (keys-template req opt qualified?)
        w-un-set (get-key-set req opt qualified?)
        k-list (u/add-postfix-to-key model-k "-list")]
    ;;conform does not work with merge
    (if fixed?
      `((clojure.spec/def ~model-k (clojure.spec/merge ~w-un (clojure.spec/map-of ~w-un-set any?)))
         (clojure.spec/def ~k-list
           (clojure.spec/coll-of ~model-k :kind vector?)))
      `((clojure.spec/def ~model-k ~w-un)
         (clojure.spec/def ~k-list
           (clojure.spec/coll-of ~model-k :kind vector?))))))


(defn property-template [req opt]
  (->> (merge opt req)
       (map (fn [[k v]]
              `(clojure.spec/def ~k ~v)))))


(defn spec-template [namespace-name model-k {:keys [fixed? qualified?]
                                             :or   {fixed?     true
                                                    qualified? true}}]
  (let [k (-> (u/as-ns-keyword namespace-name :spec)
              (u/as-ns-keyword model-k))
        v (-> (u/as-ns-keyword namespace-name model-k))
        k-list (u/add-postfix-to-key k "-list")
        v-list (u/add-postfix-to-key v "-list")]
    (if qualified?
      `((clojure.spec/def ~k (clojure.spec/merge (clojure.spec/keys :req [~v])
                                                 (clojure.spec/map-of #{~v} any?)))
         (clojure.spec/def ~k-list (clojure.spec/keys :req [~v-list])))
      `((clojure.spec/def ~k (clojure.spec/merge (clojure.spec/keys :req-un [~v])
                                                 (clojure.spec/map-of #{~model-k} any?)))
         (clojure.spec/def ~k-list (clojure.spec/keys :req-un [~v-list]))))))



(defn model->spec-one [namespace-name opt-m j-m [k v]]
  (let [[model-k {:keys [req opt]}] (-> {k v}
                                        (u/rename-model-key-to-namespace-key namespace-name)
                                        (first))
        j (->> (get j-m model-k)
               (mapv #(nth % 2)))
        opt-list (into (or j []) (keys opt))
        req-list (into [] (keys req))]

    (into (into (spec-template namespace-name k opt-m)
                (reverse (model-template model-k req-list opt-list opt-m)))
          (property-template req opt))))



(defn model->spec
  [namespace-name m {:keys [postfix join] :as opt}]
  (let [namespace-name (u/add-postfix-to-key namespace-name postfix)
        j-m (u/rename-join-key-to-ns-key namespace-name join)]
    (->> m
         (map (partial model->spec-one namespace-name opt j-m))
         (apply concat))))



(comment



  (model->spec :app {:student {:req {:id :a}}} {:qualified? false})


  (->> (map (fn [w] (update-model-key-one w :app "-ex")) {:student {:req {:di   :id
                                                                          :name :na}}})
       (map (fn [[k v]] (property-template v)))
       )

  )



(defn relational-merge-spec-template [p-spec child-spec-coll {:keys [qualified?]
                                                              :or   {qualified? true}}]
  (if (or (nil? child-spec-coll)
          (empty? child-spec-coll))
    p-spec
    (if qualified?
      (list 'clojure.spec/merge p-spec
            (list 'clojure.spec/keys :opt (into [] child-spec-coll)))
      (list 'clojure.spec/merge p-spec
            (list 'clojure.spec/keys :opt-un (into [] child-spec-coll))))))





