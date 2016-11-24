(ns dadymodel.spec-generator
  (:require [clojure.spec :as s]
            [dadymodel.util :as u]
            [clojure.spec :as s]))


(defn add-list [model-k opt-config-m]
  (let [k-list (u/add-postfix-to-key model-k "-list")]
    (when (:dadymodel.core/gen-list? opt-config-m)
      `(clojure.spec/def ~k-list
         (clojure.spec/coll-of ~model-k :kind vector?))
      )
    ))


(defn model-spec-template
  [model-k opt-config-m]
  (let [ns-identifer (name (:dadymodel.core/ns-identifier opt-config-m))
        prefix (:dadymodel.core/prefix opt-config-m)
        type (:dadymodel.core/gen-type opt-config-m)
        entity-identifier (name (:dadymodel.core/entity-identifer opt-config-m))
        k (if prefix
            (keyword (str (name prefix) "." entity-identifier "." ns-identifer "/" (name model-k)))
            (keyword (str entity-identifier "." ns-identifer "/" (name model-k))))]
    (if (= type :dadymodel.core/qualified)
      (list `(clojure.spec/def ~k (clojure.spec/keys :req [~model-k]))
            (add-list k opt-config-m))
      (list `(clojure.spec/def ~k (clojure.spec/keys :req-un [~model-k]))
            (add-list k opt-config-m)))))


(defn model-template [model-k req opt opt-config-m]
  (if (= :dadymodel.core/qualified
         (:dadymodel.core/gen-type opt-config-m))

    (if (:dadymodel.core/fixed-key? opt-config-m)
      (let [t (into #{} (into req opt))]
        (list `(clojure.spec/def ~model-k (clojure.spec/merge (clojure.spec/keys :req ~req :opt ~opt)
                                                              (s/map-of ~t any?)))
              (add-list model-k opt-config-m)))
      (list `(clojure.spec/def ~model-k (clojure.spec/keys :req ~req :opt ~opt))
            (add-list model-k opt-config-m)))


    (if (:dadymodel.core/fixed-key? opt-config-m)
      (let [t (into #{} (mapv (comp keyword name) (into req opt)))]
        (list `(clojure.spec/def ~model-k (clojure.spec/merge (clojure.spec/keys :req-un ~req :opt-un ~opt)
                                                              (s/map-of ~t any?)))
              (add-list model-k opt-config-m)))
      (list `(clojure.spec/def ~model-k (clojure.spec/keys :req-un ~req :opt-un ~opt))
            (add-list model-k opt-config-m)))))



(defn property-template [req opt]
  (->> (merge opt req)
       (map (fn [[k v]]
              `(clojure.spec/def ~k ~v)))))


(defn app-spec-template [opt-config-m k-coll]
  (let [prefix (:dadymodel.core/prefix opt-config-m)
        ns-identifier (:dadymodel.core/ns-identifier opt-config-m)
        entity-identifier (keyword (:dadymodel.core/entity-identifer opt-config-m))

        n (-> ns-identifier
              (u/add-prefix-to-key prefix)
              (u/as-ns-keyword :model))

        model-coll (mapv (fn [w] (-> (u/as-ns-keyword ns-identifier w)
                                     (u/add-prefix-to-key prefix))) k-coll)
        model-coll-with-list (when (:dadymodel.core/gen-list? opt-config-m)
                               (mapv #(u/add-postfix-to-key % "-list") model-coll))

        entity-coll (when (:dadymodel.core/gen-entity? opt-config-m)
                      (mapv (fn [w]
                              (-> (u/as-ns-keyword ns-identifier w)
                                  (u/add-prefix-to-key entity-identifier)
                                  (u/add-prefix-to-key prefix))) k-coll))
        entity-coll-with-list (when (:dadymodel.core/gen-list? opt-config-m)
                                (mapv #(u/add-postfix-to-key % "-list") entity-coll))


        w1 (remove nil? (concat model-coll model-coll-with-list entity-coll entity-coll-with-list))
        w (interleave w1 w1)]

    `(s/def ~n ~(cons 'clojure.spec/or w))))



(defn- model->spec-one [opt-config-m j-m [k v]]
  (let [ns-identifier (:dadymodel.core/ns-identifier opt-config-m)
        prefix (:dadymodel.core/prefix opt-config-m)
        namespace-name (u/add-prefix-to-key ns-identifier prefix)
        j (->> (get j-m k)
               (mapv #(u/assoc-ns-join namespace-name %)))

        model-k (u/as-ns-keyword namespace-name k)
        {:keys [req opt]} (u/update-model-key-one model-k v)

        opt-list (into (or j []) (keys opt))
        req-list (into [] (keys req))]
    (concat (property-template req opt)
            (model-template model-k req-list opt-list opt-config-m)
            (when (:dadymodel.core/gen-entity? opt-config-m)
              (model-spec-template model-k opt-config-m)  )
            )))


(defn join-m [join]
  (->> join
       (mapv u/reverse-join)
       (into join)
       (distinct)
       (group-by first)))


(defn model->spec [m opt-config-m]
  (let [join (:dadymodel.core/join opt-config-m)
        j-m (join-m join)]
    (->> m
         (map (partial model->spec-one opt-config-m j-m))
         (apply concat)
         (reverse)
         (cons (app-spec-template opt-config-m (keys m)))
         (reverse)
         (remove nil?)
         )))




(comment

  (reduce u/add-prefix-to-key (reverse [:a :c :w.b]))

  ;(u/add-prefix-to-key :a :v)


  (model->spec {:student {:opt {:id :a}}}
               {:dadymodel.core/gen-type         :dadymodel.core/un-qualified
                :dadymodel.core/gen-list?        true
                :dadymodel.core/gen-entity?      true
                :dadymodel.core/ns-identifier    :app
                :dadymodel.core/entity-identifer :enitiy
                :dadymodel.core/fixed-key?       true
                ;:dadymodel.core/prefix           :ex
                }
               ;:dadymodel.core/un-qualified
               ;:dadymodel.core/qualified

               )




  (join-m [[:dept :id :dadymodel.core/rel-one-many :student :dept-id]])



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





