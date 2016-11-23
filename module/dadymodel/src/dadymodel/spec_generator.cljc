(ns dadymodel.spec-generator
  (:require [clojure.spec :as s]
            [dadymodel.util :as u]))


(defn add-list [model-k]
  (let [k-list (u/add-postfix-to-key model-k "-list")]
    `(clojure.spec/def ~k-list
       (clojure.spec/coll-of ~model-k :kind vector?))))


#_(defn inject-entity-key [namespace-n ]
  (let [[f r] (split-at 1
                        (clojure.string/split (namespace namespace-n)  #"\.")
                        )]
    (-> (clojure.string/join "." (reverse (into f (cons "entity" r))) )
        (str "/" (name namespace-n) )
        (keyword))))


;(inject-entity-key :a.b/c)



(defn model-spec-template
  [ns-coll model-k type]
  (let [[prefix namesp] ns-coll
        ;r (name model-k)
        k (if prefix
            (keyword (str (name prefix) "." "entity." (name namesp)   "/" (name model-k)) )
            (keyword (str "entity." (name namesp)   "/" (name model-k)) )
            )

        ]

    (if (= type :dadymodel.core/qualified)
      (list `(clojure.spec/def ~k (clojure.spec/keys :req [~model-k]))
             (add-list k))

      (list `(clojure.spec/def ~k (clojure.spec/keys :req-un [~model-k]))
            (add-list k))
      #_(let [n (keyword (str "entity." (namespace model-k) "/" (name model-k)))
            r (keyword (name model-k))]
        ))
    )
  )






(defmulti model-template (fn [_ _ _ m] (:dadymodel.core/gen-type m)))

(defmethod model-template
  :dadymodel.core/qualified
  [model-k req opt _]
  (concat (list `(clojure.spec/def ~model-k (clojure.spec/keys :req ~req :opt ~opt))
                (add-list model-k))
          #_(model-spec-template model-k :dadymodel.core/qualified)))


(defmethod model-template
  :dadymodel.core/un-qualified
  [model-k req opt _]
  (concat
    (list `(clojure.spec/def ~model-k (clojure.spec/keys :req-un ~req :opt-un ~opt))
          (add-list model-k))
    #_(model-spec-template model-k :dadymodel.core/un-qualified)))


(defn property-template [req opt]
  (->> (merge opt req)
       (map (fn [[k v]]
              `(clojure.spec/def ~k ~v)))))



(defn app-spec-template [ns-coll k-coll]
  (let [[postfix namespace-name] ns-coll
        w (mapv (fn [w]
                  (-> (u/as-ns-keyword namespace-name w )
                      (u/add-prefix-to-key  :entity)
                      (u/add-prefix-to-key  postfix))) k-coll)
        w1 (concat w (mapv #(u/add-postfix-to-key % "-list") w))
        w (interleave  w1 w1)
        n (-> namespace-name
              (u/add-prefix-to-key   postfix)
              (u/as-ns-keyword   :entity))
        ]
    `(s/def ~n ~(cons 'clojure.spec/or w)) ))



(defn- model->spec-one [namespace-coll opt-m j-m [k v]]
  (let [[postfix n-name] namespace-coll
        namespace-name (u/add-prefix-to-key n-name postfix)
        j (->> (get j-m k)
               (mapv #(u/assoc-ns-join namespace-name %)))

        model-k (u/as-ns-keyword namespace-name k)
        {:keys [req opt]} (u/update-model-key-one model-k v)

        opt-list (into (or j []) (keys opt))
        req-list (into [] (keys req))]
    (concat (property-template req opt)
            (model-template model-k req-list opt-list opt-m)
            (model-spec-template namespace-coll model-k (:dadymodel.core/gen-type opt-m))
            )))


(defn join-m [join ]
  (->> join
       (mapv u/reverse-join)
       (into join)
       (distinct)
       (group-by first)))

(defn model->spec
  [namespace-name1 m {:keys [postfix ] :as opt}]
  (let [join (:dadymodel.core/join opt )


        ;namespace-name (u/add-prefix-to-key namespace-name postfix)
        ns-coll [postfix namespace-name1]


        j-m  (join-m join)]
    (->> m
         (map (partial model->spec-one ns-coll opt j-m))
         (apply concat)
         (reverse)
         (cons (app-spec-template ns-coll (keys m ) ))
         (reverse))))




(comment

  (reduce u/add-prefix-to-key (reverse [:a :c :w.b ]) )

  ;(u/add-prefix-to-key :a :v)


  (model->spec :app {:student {:opt {:id :a}}}
               {:dadymodel.core/gen-type :dadymodel.core/un-qualified
                :postfix                :ex})

  (model->spec :app/hello {:student {:opt {:id :a}}}
               {:dadymodel.core/gen-type :dadymodel.core/un-qualified
                :postfix                :un
                })

  (model->spec :app {:student {:opt {:id :a}}}
               {:dadymodel.core/gen-type :dadymodel.core/qualified
                ;:postfix                :ex
                })


  (join-m [[:dept :id :dadymodel.core/rel-one-many :student :dept-id]])


  (model->spec :app
               {:dept {:opt {:id :a}}}
               {:dadymodel.core/gen-type :dadymodel.core/unqualified
                :dadymodel.core/join     [[:dept :id :dadymodel.core/rel-one-many :student :dept-id]]
                :postfix "un-"})


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





