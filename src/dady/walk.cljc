(ns dady.walk
  (:require [clojure.walk :as w]))

(defn is-include? [filter-v w]
  (reduce (fn [acc v]
            (if (or (clojure.string/includes? (clojure.string/lower-case (str (first w)))
                                              (clojure.string/lower-case v))
                    (clojure.string/includes? (clojure.string/lower-case (str (second w)))
                                              (clojure.string/lower-case v)))
              (reduced true)
              acc))
          false
          (clojure.string/split filter-v #" ")))


(defn postwalk-filter
  "remove pairs of key-value that has nil value from a (possibly nested) map. also transform map to nil if all of its value are nil"
  [filter-v nm]
  (if (or (nil? filter-v)
          (empty? filter-v))
    nm
    (w/postwalk
      (fn [el]
        (if (map? el)
          (into {} (filter (partial is-include? filter-v) el))
          el))
      nm)))


(defn postwalk-remove-with
  [f nm]
  (w/postwalk
    (fn [el]
      (if (map? el)
        (let [m (into {} (remove (comp f second) el))]
          (when (seq m)
            m))
        el))
    nm))


(defn postwalk-remove-nils
  "remove pairs of key-value that has nil value from a (possibly nested) map. also transform map to nil if all of its value are nil"
  [nm]
  (postwalk-remove-with nil? nm))



(defn keyword->str
  [v]
  (if (keyword? v)
    (name v)
    v))


(defn replace-mk
  [f1 m]
  (let [f (fn [[k v]] [(f1 k) v])]
    (into {} (map f m))))


(defn postwalk-replace-key-with
  "Recursively transforms all map and first  vector keys from keywords to strings."
  {:added "1.1"}
  [f m]
  (w/postwalk (fn [x]
                (cond (map? x)
                      (replace-mk f x)
                      (vector? x)
                      (mapv f x)
                      :else x)) m))



(defn- replace-mv
  [f1 m]
  (let [f (fn [[k v]] [k (f1 v)])]
    (into {} (map f m))))


(defn postwalk-replace-value-with
  "Recursively transforms all map keys from strings to keywords."
  {:added "1.1"}
  [f m]
  (w/postwalk (fn [x] (cond
                        (map? x)
                        (replace-mv f x)
                        (vector? x)
                        (mapv f x)
                        :else x)) m))






(comment

  (replace-mk keyword->str [{:a 2}])

  (postwalk-replace-key-with keyword->str
                             [{:a 3}])

  (postwalk-replace-value-with [[:a :b]
                                [1 2]
                                [:a :b]
                                ])

  )

