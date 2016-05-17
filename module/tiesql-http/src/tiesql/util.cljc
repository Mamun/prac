(ns tiesql.util
  #?(:clj
     (:require [clojure.walk :as w]))
  #?@(:cljs
      [(:require [cognitect.transit :as t]
         [tiesql.common :as cc]
         [clojure.walk :as w])
       (:require-macros [dady.common :refer [try->]])]))


(def tiesql-param :params)
(def tiesql-name :name)
(def url-endpoint :default)
(def api-endpoint :api)


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
                     (select-keys [:gname :name :params :pformat :rformat :input :accept :output])
                     (cc/validate-input!)
                     (merge-default)))








