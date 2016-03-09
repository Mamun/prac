(ns app.server
  (:use compojure.core)
  (:require [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            [ring.util.response :as resp]
            [ring.middleware.params :as p]
            [ring.middleware.multipart-params :as mp]
            [ring.middleware.keyword-params :as kp]
            [ring.middleware.format-params :as fp]
            [ring.middleware.format-response :as fr]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [compojure.route :as route]
            [immutant.web :as im]
            [ring.middleware.tiesql :as hs]
            [clojure.tools.logging :as log])
  (:import
    [com.mchange.v2.c3p0 ComboPooledDataSource])
  (:gen-class))


(defn read-app-file [app-file]
  (-> app-file
      (io/resource)
      (slurp)
      (edn/read-string)))


(defn log-request
  [handler]
  (fn [req]
    (when (or (.contains (:uri req) "pull")
              (.contains (:uri req) "push"))
      (log/info (select-keys req [:params :uri :path-info])))
    (handler req)))


(defonce config (read-app-file "tiesql.edn"))
(defonce ds-atom (atom {:datasource (ComboPooledDataSource.)}))
(defonce tms-atom (atom (hs/read-init-validate-file (:tiesql-file config) @ds-atom (:tiesql-init config))))


(defroutes app-routes
           (GET "/" _ (resp/resource-response "index.html" {:root "public"}))
           (route/resources "/")
           (route/not-found {:status 200
                             :body   "Not found"}))


(def http-handler
  (-> app-routes
      (hs/warp-tiesql ds-atom tms-atom)                     ;; Data service here
      (log-request)
      (wrap-webjars)
      (kp/wrap-keyword-params)
      (p/wrap-params)
      (mp/wrap-multipart-params)
      (fp/wrap-restful-params)
      (fr/wrap-restful-response)
      wrap-with-logger
      ;(wrap-defaults api-defaults)
      wrap-with-logger
      ;wrap-gzip
      ))


(defn -main
  [& args]
  (println "Starting tie app  ")
  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        }))


