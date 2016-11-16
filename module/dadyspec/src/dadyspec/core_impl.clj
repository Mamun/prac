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


(defn- update-model-key-one [[model-k model-property]]
  (let [v (as-> model-property m
                (if (:req m)
                  (update m :req (fn [w] (update-model-key-m model-k w)))
                  m)
                (if (:opt m)
                  (update m :opt (fn [w] (update-model-key-m model-k w)))
                  m))]
    [model-k v]))


(defn update-model-key [model & ns-key]
  (let [w (reduce add-postfix-to-key ns-key)]
    (->> model
         (update-model-key-m w )
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


(defn convert-property-to-def [[_ {:keys [req opt]}] ]
  (->> (merge opt req)
       (map (fn [[k v]]
              `(clojure.spec/def ~k ~v)))))



(defn assoc-ns-join [base-ns-name [src rel dest]]
  (let [src (as-ns-keyword base-ns-name src)
        v (condp = rel
            :dadyspec.core/one-one (as-ns-keyword base-ns-name dest)
            :dadyspec.core/many-one (as-ns-keyword base-ns-name dest)
            :dadyspec.core/one-many (-> (as-ns-keyword base-ns-name dest)
                                        (add-postfix-to-key "-list")))]
    [src rel v]))


(defn model->spec
  [namespace-name m {:keys [postfix join] :as opt}]
  (let [namespace-name (if postfix
                         (add-postfix-to-key namespace-name postfix)
                         namespace-name)
        j-m (->> join
                 (mapv #(assoc-ns-join namespace-name %))
                 (group-by first))
        assoc-join-k (fn [w]
                       (->> (get j-m (first w))
                            (mapv #(nth % 2))))]
    (->> (update-model-key m namespace-name)
         (mapv (fn [w]
                 (-> w
                     (model-template (assoc-join-k w) opt)
                     (into (convert-property-to-def w)))))
         (apply concat))))



(comment
  (->> (map (fn [w] (update-model-key-one w :app "-ex")) {:student {:req {:di :id
                                                                      :name   :na}}})
       (map (fn [[k v]] (convert-property-to-def v)))
       )

  )








#_(defn format-join [base-ns-name join-list]
    (->> join-list
         (mapv (partial assoc-ns-join base-ns-name))
         (group-by first)))





#_(defn reverse-join [[src rel dest]]
    (condp = rel
      :1-n [dest :n-1 src]
      :n-1 [dest :1-n src]
      [src rel dest]))


