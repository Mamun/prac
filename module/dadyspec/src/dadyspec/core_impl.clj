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




(defn rename-key-to-namespace-key [namespace-key m]
  (if (nil? m)
    {}
    (->> (map (fn [v]
                (as-ns-keyword namespace-key v)) (keys m))
         (interleave (keys m))
         (apply assoc {})
         (clojure.set/rename-keys m))))


(defn- update-model-key-one [[model-k model-property]]
  (let [v (as-> model-property m
                (if (:req m)
                  (update m :req (fn [w] (rename-key-to-namespace-key model-k w)))
                  m)
                (if (:opt m)
                  (update m :opt (fn [w] (rename-key-to-namespace-key model-k w)))
                  m))]
    [model-k v]))


(defn rename-model-key-to-namespace-key [model & namespace-key-list]
  (let [w (reduce add-postfix-to-key namespace-key-list)]
    (->> model
         (rename-key-to-namespace-key w)
         (map update-model-key-one)
         (into {}))))


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
  [[model-k {:keys [req opt]}] j {:keys [fixed? qualified?]
                                  :or   {fixed?     true
                                         qualified? true}}]
  (let [req (into [] (keys req))
        opt (into (or j []) (keys opt))

        w-un (keys-template req opt qualified?)
        w-un-set (get-key-set req opt qualified?)]
    ;;conform does not work with merge
    (if fixed?
      `((clojure.spec/def ~model-k (clojure.spec/merge ~w-un (clojure.spec/map-of ~w-un-set any?)))
         (clojure.spec/def ~(add-postfix-to-key model-k "-list")
           (clojure.spec/coll-of ~model-k :kind vector?)))
      `((clojure.spec/def ~model-k ~w-un)
         (clojure.spec/def ~(add-postfix-to-key model-k "-list")
           (clojure.spec/coll-of ~model-k :kind vector?))))))


(defn convert-property-to-def [[_ {:keys [req opt]}]]
  (->> (merge opt req)
       (map (fn [[k v]]
              `(clojure.spec/def ~k ~v)))))



(defn assoc-ns-join [base-ns-name [src rel dest]]
  (let [src (as-ns-keyword base-ns-name src)
        v (condp = rel
            :dadyspec.core/rel-one-one (as-ns-keyword base-ns-name dest)
            :dadyspec.core/rel-many-one (as-ns-keyword base-ns-name dest)
            :dadyspec.core/rel-one-many (-> (as-ns-keyword base-ns-name dest)
                                           (add-postfix-to-key "-list")))]
    [src rel v]))


(defn reverse-join [[src rel dest]]
  (condp = rel
    :dadyspec.core/rel-one-one  [dest :dadyspec.core/rel-one-one src]
    :dadyspec.core/rel-many-one [dest :dadyspec.core/rel-one-many src]
    :dadyspec.core/rel-one-many [dest :dadyspec.core/rel-many-one src]))


(defn model->spec
  [namespace-name m {:keys [postfix join] :as opt}]
  (let [namespace-name (if postfix
                         (add-postfix-to-key namespace-name postfix)
                         namespace-name)
        j-m (->> join
                 (mapv reverse-join)
                 (into join )
                 (distinct)
                 (mapv #(assoc-ns-join namespace-name %))
                 (group-by first))
        assoc-join-k (fn [w]
                       (->> (get j-m (first w))
                            (mapv #(nth % 2))))]
    (->> (rename-model-key-to-namespace-key m namespace-name)
         (mapv (fn [w]
                 (-> w
                     (model-template (assoc-join-k w) opt)
                     (into (convert-property-to-def w)))))
         (apply concat))))



(comment
  (->> (map (fn [w] (update-model-key-one w :app "-ex")) {:student {:req {:di   :id
                                                                          :name :na}}})
       (map (fn [[k v]] (convert-property-to-def v)))
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





