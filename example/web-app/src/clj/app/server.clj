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
            [tiesql.common :as cc]
            [clojure.tools.logging :as log])
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
   (route/not-found {:status 200
                     :body   "Not found From app "}))


(defn warp-state [handler]
  (fn [request]
    (init-state)
    (handler request)))


#_(defn warp-log [handler]
  (fn [req]
    (log/info "-----------------" req)
    (handler req)
    ))


(def http-handler
  (-> app-handler
      (hs/warp-tiesql-handler :tms tms-atom :ds ds-atom)
      (warp-state)
      ;(wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
   ;   (warp-log)
      (wrap-webjars)
      wrap-with-logger
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