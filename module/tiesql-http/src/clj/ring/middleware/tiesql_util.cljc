(ns ring.middleware.tiesql-util
  (:require [tiesql.common :as cc])
  #?(:cljs
     (:require-macros [tiesql.common :refer [try->]])))


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



