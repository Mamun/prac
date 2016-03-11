(ns app
  (:use compojure.core)
  (:require [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            [ring.util.response :as resp]
            [ring.middleware.tiesql :as hs]
            [compojure.route :as route]
            [immutant.web :as im]
            [tiesql.jdbc :as tj])
  (:import
    [com.mchange.v2.c3p0 ComboPooledDataSource])
  (:gen-class))


(defn read-app-file [app-file]
  (-> app-file
      (io/resource)
      (slurp)
      (edn/read-string)))


(defn read-file [ds {:keys [tiesql-file tiesql-init]}]
  (let [v (tj/read-file tiesql-file)]
    (when tiesql-init
      (tj/db-do ds v tiesql-init))
    (tj/validate-dml! ds (tj/get-dml v))
    v))


(defonce config (read-app-file "tiesql.edn"))
(defonce ds-atom (atom {:datasource (ComboPooledDataSource.)}))
(defonce tms-atom (atom (read-file @ds-atom config)))


(defroutes app-handler
   (GET "/" _ (resp/resource-response "index.html" {:root "public"}))
   (route/resources "/")
   (route/not-found {:status 200 :body "Not found"}))


(def http-handler
  (-> app-handler
      (hs/warp-tiesql-handler ds-atom tms-atom)))


(defn -main
  [& args]
  (println "Starting tie app 3000 ")
  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        }))
