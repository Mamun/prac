(ns dadyspec.util
  (:require [clojure.set]))


(defn add-postfix-to-key [namespace-key post-fix-str]
  (if post-fix-str
    (if (namespace namespace-key)
      (keyword (str (namespace namespace-key) "/" (name namespace-key) post-fix-str))
      (keyword (str (name namespace-key) post-fix-str)))
    namespace-key))


(defn add-prefix-to-key [namespace-key post-fix-str]
  (if post-fix-str
    (if (namespace namespace-key)
      (keyword (str post-fix-str (namespace namespace-key) "/" (name namespace-key)))
      (keyword (str post-fix-str (name namespace-key))))
    namespace-key))



;; or does not work correctly for unfrom core api
(defn as-ns-keyword [ns-key r]
  ;(println ns-key "--" r)

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


(defn update-model-key-one [model-k model-property]
  (as-> model-property m
        (if (:req m)
          (update m :req (fn [w] (rename-key-to-namespace-key model-k w)))
          m)
        (if (:opt m)
          (update m :opt (fn [w] (rename-key-to-namespace-key model-k w)))
          m)))



(defn get-spec-model [base-ns-name m]
  (let [w (-> (as-ns-keyword base-ns-name :spec)
              (rename-key-to-namespace-key m)
              (keys))]
    (->> (mapv #(add-postfix-to-key % "-list") w)
         (concat w))))



#_(defn reverse-join [[src rel dest]]
  (condp = rel
    :dadyspec.core/rel-one-one [dest :dadyspec.core/rel-one-one src]
    :dadyspec.core/rel-many-one [dest :dadyspec.core/rel-one-many src]
    :dadyspec.core/rel-one-many [dest :dadyspec.core/rel-many-one src]))


(defn reverse-join [[s-tab s-id join-key d-tab d-id [r-tab r-id r-id2] :as j]]
  (condp = join-key
    :dadyspec.core/join-one-one [d-tab d-id :dadyspec.core/join-one-one s-tab s-id]
    :dadyspec.core/join-one-many [d-tab d-id :dadyspec.core/join-many-one s-tab s-id]
    :dadyspec.core/join-many-one [d-tab d-id :dadyspec.core/join-one-many s-tab s-id]
    :dadyspec.core/join-many-many [d-tab d-id :dadyspec.core/join-many-many s-tab s-id [r-tab r-id2 r-id]]
    j))



#_(defn map-reverse-join
  [join-coll]
  (let [f (fn [[s-tab s-id join-key d-tab d-id [r-tab r-id r-id2] :as j]]
            (condp = join-key
              :dadyspec.core/join-one-one [d-tab d-id :dadyspec.core/join-one-one s-tab s-id]
              :dadyspec.core/join-one-many [d-tab d-id :dadyspec.core/join-many-one s-tab s-id]
              :dadyspec.core/join-many-one [d-tab d-id :dadyspec.core/join-one-many s-tab s-id]
              :dadyspec.core/join-many-many [d-tab d-id :dadyspec.core/join-many-many s-tab s-id [r-tab r-id2 r-id]]
              j))]
    (->> (map f join-coll)
         (concat join-coll)
         (distinct)
         (sort-by first)
         (into []))))



(defn assoc-ns-join [base-ns-name [src _ rel dest _ ]]
  (condp = rel
    :dadyspec.core/rel-one-one (as-ns-keyword base-ns-name dest)
    :dadyspec.core/rel-many-one (as-ns-keyword base-ns-name dest)
    :dadyspec.core/rel-one-many (-> (as-ns-keyword base-ns-name dest)
                                    (add-postfix-to-key "-list"))))



