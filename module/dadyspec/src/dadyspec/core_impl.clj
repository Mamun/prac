(ns dadyspec.core-impl
  (:require [clojure.spec :as s]
            [dadyspec.util :as u]))


#_(defn- get-key-set [req opt qualified?]
    (if qualified?
      (into #{} (into req opt))
      (->> (into req opt)
           (mapv name)
           (mapv keyword)
           (into #{}))))

(defn add-list [model-k]
  (let [k-list (u/add-postfix-to-key model-k "-list")]
    `(clojure.spec/def ~k-list
       (clojure.spec/coll-of ~model-k :kind vector?))))


(defn add-spec
  ([model-k qualified? ]
   (if qualified?
     (let [n (keyword (str (namespace model-k) ".spec/" (name model-k)))]
       `(clojure.spec/def ~n (clojure.spec/merge (clojure.spec/keys :req [~model-k])
                                                 (clojure.spec/map-of #{~model-k} any?))))
     (let [n (keyword (str (namespace model-k) ".spec/" (name model-k)))
           r (keyword (name model-k)) ]
       `(clojure.spec/def ~n (clojure.spec/merge (clojure.spec/keys :req-un [~model-k])
                                                 (clojure.spec/map-of #{~r} any?))))))
  ([model-k]
   (add-spec model-k true)))


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

  (list `(clojure.spec/def ~model-k (clojure.spec/keys :req ~req :opt ~opt))
        (add-list model-k)
        (add-spec model-k)
        (add-spec (u/add-postfix-to-key model-k "-list") )
        ))


(defmethod model-template
  :un-qualified
  [model-k req opt {:keys [fixed? qualified?]
                    :or   {fixed?     true
                           qualified? true}}]
  (list `(clojure.spec/def ~model-k (clojure.spec/keys :req-un ~req :opt-un ~opt))
        (add-list model-k)
        (add-spec model-k false)
        (add-spec (u/add-postfix-to-key model-k "-list") false  )))


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


#_(defn spec-template [namespace-name model-k {:keys [fixed? qualified?]
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
  (let [model-k (u/as-ns-keyword namespace-name k)
        {:keys [req opt]} (u/update-model-key-one model-k v)
        j (->> (get j-m model-k)
               (mapv #(u/assoc-ns-join namespace-name %)))
        opt-list (into (or j []) (keys opt))
        req-list (into [] (keys req))]
    (concat (property-template req opt)
            (model-template model-k req-list opt-list opt-m)
            #_(spec-template namespace-name k opt-m))))


(defn model->spec
  [namespace-name m {:keys [postfix join] :as opt}]
  (let [namespace-name (u/add-postfix-to-key namespace-name postfix)
        w (mapv (fn [w]
                  (keyword (str (name namespace-name) ".spec") (name w))
                  ) (keys m ))
        w1 (concat w (mapv #(u/add-postfix-to-key % "-list") w))
        _ (println "--" w1)

        j-m (->> join
                 (mapv u/reverse-join)
                 (into join)
                 (distinct)
                 (group-by first))]
    (->> m
         (map (partial model->spec-one namespace-name opt j-m))
         (apply concat))))



(comment



  (model->spec :app {:student {:opt {:id :a}}} {:qualified? true})


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





