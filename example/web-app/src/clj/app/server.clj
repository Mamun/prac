(ns app.server
  (:use compojure.core)
  (:require [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            [ring.util.response :as resp]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [compojure.route :as route]
            [immutant.web :as im]
            [ring.middleware.tiesql :as hs]
            [tiesql.jdbc :as tj]
            [clojure.tools.logging :as log])
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


(defonce config   (read-app-file "tiesql.edn"))
(defonce ds-atom  (atom {:datasource (ComboPooledDataSource.)}))
(defonce tms-atom (atom (read-file @ds-atom config)))


(defroutes app-handler
   (GET "/" _ (resp/resource-response "index.html" {:root "public"}))
   (route/resources "/")
   (route/not-found {:status 200
                     :body   "Not found From app "}))


#_(defn warp-log [handler]
  (fn [req]
    (log/info "-----------------" req)
    (handler req)
    ))


(def http-handler
  (-> app-handler
      (hs/warp-tiesql-handler ds-atom tms-atom)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
   ;   (warp-log)
      (wrap-webjars)
      wrap-with-logger
      ;wrap-gzip
      ))


(defn -main
  [& args]
  (println "Starting tie app  ")
  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        }))


(comment


  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        })

  )