(ns app.server
  (:use compojure.core)
  (:require [clojure.java.io :as io]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [compojure.route :as route]
            [immutant.web :as im]
            [ring.middleware.tiesql :as hs]
            [tiesql.jdbc :as tj]
            [tiesql.common :as cc]
            [clojure.tools.logging :as log])
  (:import
    [com.mchange.v2.c3p0 ComboPooledDataSource])
  (:gen-class))


(defonce ds-atom (atom nil))
(defonce tms-atom (atom nil))


(defn init-state []
  (when (nil? @ds-atom)
    (reset! ds-atom {:datasource (ComboPooledDataSource.)}))
  (when (nil? @tms-atom)
    (cc/try->> (tj/read-file "tie.edn.sql" )
               (tj/db-do @ds-atom [:create-ddl :init-data])
               (tj/validate-dml! @ds-atom)
               (reset! tms-atom))))


(defroutes app-handler
           (GET "/" _
             {:status  200
              :headers {"Content-Type" "text/html; charset=utf-8"}
              :body    (io/input-stream (io/resource "public/index.html"))} )
           (route/resources "/")
           (route/not-found {:status 200
                             :body   "Not found From app "}))


(defn warp-log [handler]
  (fn [req]
    (log/info "-----------------" req)
    (handler req)
    ))


(def http-handler
  (-> app-handler
      (hs/warp-tiesql-handler :tms tms-atom :ds ds-atom)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      ;  (warp-log)
      (wrap-webjars)
      ;  wrap-with-logger
      ;wrap-gzip
      ))


(defn -main
  [& args]
  (println "Starting tie app  ")
  (init-state)
  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        }))


(comment


  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        })

  )