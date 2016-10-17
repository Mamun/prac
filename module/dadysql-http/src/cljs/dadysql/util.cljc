(ns dadysql.util
  #?@(:cljs
      [(:require [cognitect.transit :as t])]))


;(def dadysql-param :params)
(def dadysql-name :name)


#?(:clj
   (defn as-str [v]
     (cond
       (string? v) v
       (keyword? v) (name v)
       (number? v) (str v)
       :else v)))


#?(:cljs
   (defn as-str [v]
     (if (t/tagged-value? v)
       (.-rep v)
       v)))



#_(defn merge-default
    [m]
    (-> m
        (update-in [:input] (fn [v] (or v :keyword)))
        (update-in [:output] (fn [v] (or v :keyword)))
        (update-in [:accept] (fn [v] (or v "application/transit+json")))))


#_(defn validate-and-assoc-default
    [request-m]
    (#?(:clj  cc/try->
        :cljs try->) request-m
                     (select-keys [:gname :name :params :dadysql.core/param-format :dadysql.core/output-format :input :accept :output])
                     (cc/validate-input!)
                     (merge-default)))








