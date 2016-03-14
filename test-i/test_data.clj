(ns test-data
  (:require [tiesql.common :refer :all]
            [tiesql.jdbc :refer :all])
  (:import [com.mchange.v2.c3p0 ComboPooledDataSource]))


(defonce ds (atom nil))
(defonce tms (atom nil))


(defn get-ds
  []
  (when (nil? @ds)
    (reset! ds {:datasource (ComboPooledDataSource.)})
    (println "Init datasource connection  "))
  @ds)


(defn get-tms
  []
  (when (nil? @tms)
    (let [w (read-file "tie.edn.sql")]
      (warp-db-do (get-ds)  [:create-ddl :init-data] w)
      (reset! tms w))
    (println "reading "))
  @tms)
