(ns tiesql.util
  (:require
    [clojure.walk :as w]
    [cognitect.transit :as t]))


(defn postwalk-remove-nils
  "remove pairs of key-value that has nil value from a (possibly nested) map. also transform map to nil if all of its value are nil"
  [nm]
  (w/postwalk
    (fn [el]
      (if (map? el)
        (let [m (into {} (remove (comp nil? second) el))]
          (when (seq m)
            m))
        el))
    nm))


(defn replace-map-tag-value
  [m]
  (let [f (fn [[k v]] (if (t/tagged-value? v)
                        [k (.-rep v)]
                        [k v]))]
    (into {} (map f m))))


(defn replace-vector-tag-value
  [w]
  (mapv (fn [v]
          (if (t/tagged-value? v)
            (.-rep v)
            v)) w))


(defn postwalk-replace-tag-value
  "Recursively transforms all map keys from strings to keywords."
  {:added "1.1"}
  [m]
  (w/postwalk (fn [x] (cond
                        (map? x)
                        (replace-map-tag-value x)
                        (vector? x)
                        (replace-vector-tag-value x)
                        :else x)) m))
