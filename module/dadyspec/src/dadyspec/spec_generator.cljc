(ns dadyspec.spec-generator
  (:require [clojure.spec :as s]
            [dadyspec.util :as u]))


(defn add-list [model-k]
  (let [k-list (u/add-postfix-to-key model-k "-list")]
    `(clojure.spec/def ~k-list
       (clojure.spec/coll-of ~model-k :kind vector?))))


(defn model-spec-template
  [model-k type]
  (if (= type :dadyspec.core/qualified)
    (let [n (keyword (str "entity." (namespace model-k) "/" (name model-k)))]
      (list `(clojure.spec/def ~n (clojure.spec/keys :req [~model-k]))
            (add-list n)))
    (let [n (keyword (str "entity." (namespace model-k) "/" (name model-k)))
          r (keyword (name model-k))]
      (list `(clojure.spec/def ~n (clojure.spec/keys :req-un [~model-k]))
            (add-list n)))))


(defmulti model-template (fn [_ _ _ m] (:dadyspec.core/gen-type m)))

(defmethod model-template
  :dadyspec.core/qualified
  [model-k req opt _]
  (concat (list `(clojure.spec/def ~model-k (clojure.spec/keys :req ~req :opt ~opt))
                (add-list model-k))
          (model-spec-template model-k :dadyspec.core/qualified)))


(defmethod model-template
  :dadyspec.core/un-qualified
  [model-k req opt _]
  (concat
    (list `(clojure.spec/def ~model-k (clojure.spec/keys :req-un ~req :opt-un ~opt))
          (add-list model-k))
    (model-spec-template model-k :dadyspec.core/un-qualified)))


(defn property-template [req opt]
  (->> (merge opt req)
       (map (fn [[k v]]
              `(clojure.spec/def ~k ~v)))))



(defn app-spec-template [namespace-name coll]
  (let [w (interleave  coll coll)]
    `(s/def ~(u/as-ns-keyword namespace-name :entity) ~(cons 'clojure.spec/or w)) ))



(defn- model->spec-one [namespace-name opt-m j-m [k v]]
  (let [j (->> (get j-m k)
               (mapv #(u/assoc-ns-join namespace-name %)))

        model-k (u/as-ns-keyword namespace-name k)
        {:keys [req opt]} (u/update-model-key-one model-k v)

        ;_ (println j-m)
        opt-list (into (or j []) (keys opt))
        req-list (into [] (keys req))]
    (concat (property-template req opt)
            (model-template model-k req-list opt-list opt-m))))


(defn join-m [join ]
  (->> join
       (mapv u/reverse-join)
       (into join)
       (distinct)
       (group-by first)))

(defn model->spec
  [namespace-name m {:keys [postfix ] :as opt}]
  (let [join (:dadyspec.core/join opt )
        namespace-name (u/add-prefix-to-key namespace-name postfix)
        w (mapv (fn [w]
                  (keyword (str "entity." (name namespace-name) ) (name w))
                  ) (keys m))
        w1 (concat w (mapv #(u/add-postfix-to-key % "-list") w))

        j-m  (join-m join)]
    (->> m
         (map (partial model->spec-one namespace-name opt j-m))
         (apply concat)
         (reverse)
         (cons (app-spec-template namespace-name w1))
         (reverse))))




(comment


  (model->spec :app {:student {:opt {:id :a}}}
               {:dadyspec.core/gen-type :dadyspec.core/un-qualified
                :postfix                "ex-"})

  (join-m [[:dept :id :dadyspec.core/rel-one-many :student :dept-id]])


  (model->spec :app
               {:dept {:opt {:id :a}}}
               {:dadyspec.core/gen-type :dadyspec.core/unqualified
                :dadyspec.core/join     [[:dept :id :dadyspec.core/rel-one-many :student :dept-id]]
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





