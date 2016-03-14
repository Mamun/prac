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


(defonce config (read-app-file "tiesql.edn"))
(defonce ds-atom (atom nil))
(defonce tms-atom (atom nil))


(defn init-state []
  (when (nil? @ds-atom)
    (reset! ds-atom {:datasource (ComboPooledDataSource.)}))
  (if (nil? @tms-atom)
    (->> (tj/read-file (:tiesql-file config))
         (tj/warp-db-do @ds-atom (:tiesql-init config))
         (tj/warp-validate-dml! @ds-atom)
         (reset! tms-atom))))


(defroutes app-handler
   (GET "/" _ (resp/resource-response "index.html" {:root "public"}))
   (route/resources "/")
   (route/not-found {:status 200 :body "Not found"}))


(def http-handler
  (-> app-handler
      (hs/warp-tiesql-handler :tms tms-atom :ds ds-atom)))


(defn -main
  [& args]
  (println "Starting tie app 3000 ")
  (init-state)
  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        }))
