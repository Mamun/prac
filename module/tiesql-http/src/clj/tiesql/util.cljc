(ns tiesql.util
  #?(:clj
     (:require [tiesql.common :as cc]
               [clojure.walk :as w]))
  #?@(:cljs
      [
       (:require [cognitect.transit :as t]
         [tiesql.common :as cc]
         [clojure.walk :as w])
       (:require-macros [tiesql.common :refer [try->]])]))


(def tiesql-param :params)
(def tiesql-name :name)
(def url-endpoint :default)
(def api-endpoint :api)



(defn merge-default
  [m]
  (-> m
      (update-in [:input]  (fn [v] (or v :keyword)))
      (update-in [:output] (fn [v] (or v :keyword)))
      (update-in [:accept] (fn [v] (or v "application/transit+json")))))


(defn validate-and-assoc-default
  [request-m]
  (#?(:clj  cc/try->
      :cljs try->) request-m
                   (select-keys [:gname :name :params :pformat :rformat :input :accept :output])
                   (cc/validate-input!)
                   (merge-default)))



(defn response-format
  [m]
  (if (cc/failed? m)
    [nil (into {} m)]
    [m nil]))



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


#?(:cljs
   (defn get-tag-value [v]
     (if (t/tagged-value? v)
       (.-rep v)
       v)))


#?(:cljs
   (defn replace-map-tag-value
     [m]
     (let [f (fn [[k v]] [k (get-tag-value v)] )]
       (into {} (map f m)))))


#?(:cljs
   (defn postwalk-replace-tag-value
     "Recursively transforms all map keys from strings to keywords."
     {:added "1.1"}
     [m]
     (w/postwalk (fn [x] (cond
                           (map? x)
                           (replace-map-tag-value x)
                           (vector? x)
                           (mapv get-tag-value x)
                           :else x)) m)))


(defn postwalk-stringify-keys
  "Recursively transforms all map and first  vector keys from keywords to strings."
  {:added "1.1"}
  [m]
  (let [f (fn [[k v]] (if (keyword? k) [(name k) v] [k v]))
        fv (fn [v] (update-in v [0] (fn [w]
                                      (if-not (vector? w)
                                        w
                                        (mapv #(name %) w)))))]
    ;; only apply to maps and vector
    (w/postwalk (fn [x]
                  (cond (map? x)
                        (into {} (map f x))
                        (vector? x) (fv x)
                        :else x)) m)))
