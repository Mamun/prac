(ns dadyspec.core-impl
  (:require [clojure.spec :as s]
            [dadyspec.util :as u]))


(defn add-list [model-k]
  (let [k-list (u/add-postfix-to-key model-k "-list")]
    `(clojure.spec/def ~k-list
       (clojure.spec/coll-of ~model-k :kind vector?))))


(defn model-spec-template
  ([model-k qualified?]
   (if qualified?
     (let [n (keyword (str (namespace model-k) ".spec/" (name model-k)))]
       (list `(clojure.spec/def ~n (clojure.spec/keys :req [~model-k]))
             (add-list n)))
     (let [n (keyword (str (namespace model-k) ".spec/" (name model-k)))
           r (keyword (name model-k))]
       (list `(clojure.spec/def ~n (clojure.spec/keys :req-un [~model-k]))
             (add-list n)))))
  ([model-k]
   (model-spec-template model-k true)))



(defn type-dispatch [{:keys [fixed? qualified?]
                      :or   {fixed?     true
                             qualified? true}}]
  (if qualified?
    :default
    :un-qualified))


(defmulti model-template (fn [_ _ _ m] (type-dispatch m)))

(defmethod model-template
  :default
  [model-k req opt {:keys [fixed? qualified?]
                    :or   {fixed?     true
                           qualified? true}}]
  (concat (list `(clojure.spec/def ~model-k (clojure.spec/keys :req ~req :opt ~opt))
                (add-list model-k))
          (model-spec-template model-k)))


(defmethod model-template
  :un-qualified
  [model-k req opt {:keys [fixed? qualified?]
                    :or   {fixed?     true
                           qualified? true}}]
  (list `(clojure.spec/def ~model-k (clojure.spec/keys :req-un ~req :opt-un ~opt))
        (add-list model-k)
        (model-spec-template model-k false)))


(defmethod model-template
  :qualified-fixed
  [model-k req opt {:keys [fixed? qualified?]
                    :or   {fixed?     true
                           qualified? true}}]
  (let [w-un-set (into #{} (into req opt)) #_(get-key-set req opt qualified?)]
    ;;conform does not work with merge
    (list `(clojure.spec/def ~model-k
             (clojure.spec/merge (clojure.spec/keys :req ~req :opt ~opt)
                                 (clojure.spec/map-of ~w-un-set any?)))
          (add-list model-k))))



(defn property-template [req opt]
  (->> (merge opt req)
       (map (fn [[k v]]
              `(clojure.spec/def ~k ~v)))))




(defn app-spec-template [namespace-name coll]
  (let [w (interleave  coll #_(map (comp keyword name)  coll)  coll)]
    `(s/def ~(u/as-ns-keyword namespace-name :spec) ~(cons 'clojure.spec/or w)) )
  )



(defn- model->spec-one [namespace-name opt-m j-m [k v]]
  (let [model-k (u/as-ns-keyword namespace-name k)
        {:keys [req opt]} (u/update-model-key-one model-k v)
        j (->> (get j-m model-k)
               (mapv #(u/assoc-ns-join namespace-name %)))
        opt-list (into (or j []) (keys opt))
        req-list (into [] (keys req))]
    (concat (property-template req opt)
            (model-template model-k req-list opt-list opt-m))))


(defn model->spec
  [namespace-name m {:keys [postfix join] :as opt}]
  (let [namespace-name (u/add-prefix-to-key namespace-name postfix)
        w (mapv (fn [w]
                  (keyword (str (name namespace-name) ".spec") (name w))
                  ) (keys m))
        w1 (concat w (mapv #(u/add-postfix-to-key % "-list") w))

        j-m (->> join
                 (mapv u/reverse-join)
                 (into join)
                 (distinct)
                 (group-by first))]
    (->> m
         (map (partial model->spec-one namespace-name opt j-m))
         (apply concat)
         (reverse)
         (cons (app-spec-template namespace-name w1))
         (reverse)
         )))



(comment



  (model->spec :app.hello {:student {:opt {:id :a}}} {:qualified? true :postfix "un-"})


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





