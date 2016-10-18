(ns dady.spec
  (:require [clojure.spec :as s]
            [clojure.walk :as w]))


(defn filename-as-keyword [file-name-str]
  (-> (clojure.string/split file-name-str #"\.")
      (first)
      (keyword)))


(defn build-ns-keyword [& nas]
  (keyword (apply str (interpose "." (map name nas)))))


(defn add-ns-to-keyword [k & nas]
  (if (empty? nas)
    k
    (-> (apply build-ns-keyword nas)
        (name)
        (str "/" (name k))
        (keyword))))


(defn rename-nskeys-one [mapkey-fn m]
  (let [k (keys m)
        w (map mapkey-fn k)
        nk (apply assoc {} (interleave k w))]
    (clojure.set/rename-keys m nk)))


(defn rename-ns-key-batch [m]
  (->> m
       (map (fn [[k v-m]]
              {k (rename-nskeys-one (fn [v]
                                      (add-ns-to-keyword v k)) v-m)}))
       (into {})))


(defn as-ns-key-format [parent-ns-keyword m]
  (->> (rename-nskeys-one (fn [k] (build-ns-keyword parent-ns-keyword k)) m)
       (rename-ns-key-batch)
       (rename-nskeys-one (fn [k] (add-ns-to-keyword :spec k)))))


(defn build-spec-one [m]
  (map
    (fn [[k v]]
      (list 'clojure.spec/def k v))
    m))


(defn build-spec-batch [m]
  (->> m
       (map (fn [[k v]]
              (cons
                (list 'clojure.spec/def k (list 'clojure.spec/keys :req-un (into [] (keys v))))
                (build-spec-one v))))))


(defn map->spec [parent-ns-keyword m]
  (->> m
       (as-ns-key-format parent-ns-keyword)
       (build-spec-batch)
       (apply concat)))


(defn eval-spec [coll-v]
  (doseq [v coll-v]
    (eval v)))


(defn registry-by-namespace [n-name]
  (->> (s/registry)
       (w/postwalk (fn [v]
                     (if (map? v)
                       (->> v
                            (filter (fn [[k _]]
                                      (clojure.string/includes? (str k) (str n-name))))
                            (into {}))
                       v)))))


;(as-ns-keyword :a :n)

(defn merge-spec [coll-spec]
  (eval
    (cons 'clojure.spec/merge
          coll-spec)))









