(ns tiesql.util
  #?(:clj
     (:require [tiesql.common :as cc]
               [clojure.walk :as w]))
  #?@(:cljs
      [(:require [cognitect.transit :as t]
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






#?(:clj
   (defn as-str [v]
     (cond
       (string? v) v
       (keyword? v) (name v)
       (number? v)  (str v)
       :else v)))


#?(:cljs
  (defn as-str [v]
    (if (t/tagged-value? v)
      (.-rep v)
      v)))


(defn- replace-mv
  [f1 m]
  (let [f (fn [[k v]] [k (f1 v)] )]
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


(defn keyword->str
  [v]
  (if (keyword? v)
    (name v)
    v))


(defn replace-mk
  [f1 m]
  (let [f (fn [[k v]] [(f1 k) v] )]
    (into {} (map f m))))


(defn postwalk-replace-key-with
  "Recursively transforms all map and first  vector keys from keywords to strings."
  {:added "1.1"}
  [f m]
  (w/postwalk (fn [x]
                (cond (map? x)
                      (replace-mk f m)
                      (vector? x)
                      (mapv f x)
                      :else x)) m))






(comment

  (postwalk-replace-value-with [[:a :b]
                               [1 2]
                               [:a :b]
                               ])


  )




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
          (into {} (filter (partial is-include? filter-v)  el))
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




(comment

  (postwalk-filter "hello 3" {:hello 2 :check 3 :5 5 :check3 4})

  )