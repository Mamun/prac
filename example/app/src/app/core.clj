(ns app.core
  (:use compojure.core)
  (:require [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            [ring.util.response :as resp]
            [ring.middleware.tiesql :as hs]
            [compojure.route :as route]
            [immutant.web :as im]
            [tiesql.jdbc :as tj]
            [tiesql.common :as cc])
  (:import
    [com.mchange.v2.c3p0 ComboPooledDataSource])
  (:gen-class))



(defn read-app-file [app-file]
  (-> app-file
      (io/resource)
      (slurp)
      (edn/read-string)))


(defonce app-config (read-app-file "tiesql.edn"))
(defonce ds-atom (atom nil))
(defonce tms-atom (atom nil))


(defn init-state []
  (when (nil? @ds-atom)
    (reset! ds-atom {:datasource (ComboPooledDataSource.)}))
  (if (nil? @tms-atom)
    (cc/try->> (tj/read-file (:tiesql-file app-config))
               (tj/db-do @ds-atom (:tiesql-init app-config))
               (tj/validate-dml! @ds-atom)
               (reset! tms-atom))))


(defroutes app-handler
           (GET "/" _ (resp/resource-response "index.html" {:root "public"}))
           (route/resources "/")
           (route/not-found {:status 200 :body "Not found"}))


(defn warp-state [handler]
  (fn [request]
    (init-state)
    (handler request)))


(def http-handler
  (-> app-handler
      (hs/warp-tiesql-handler :tms tms-atom :ds ds-atom)
      (warp-state)))


(defn -main
  [& args]
  (println "Starting tie app 3000 ")
  (init-state)
  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        }))

