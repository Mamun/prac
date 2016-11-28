(ns app.core
  (:use compojure.core)
  (:require [ring.util.response :as resp]
            [ring.middleware.defaults :refer :all]
            [ring.middleware.dadysql :as t]
            [compojure.route :as route]
            [immutant.web :as im]
            [clojure.tools.logging :as log])
  (:import
    [com.mchange.v2.c3p0 ComboPooledDataSource])
  (:gen-class))

(defonce ds-atom (atom nil))
(defonce m-config (atom nil))


(defn init-state []
  (when (nil? @ds-atom)
    (reset! ds-atom {:datasource (ComboPooledDataSource.)}))
  (let [config [{:file-name "tie.edn.sql"
                 :init-name [:init-db :init-data]
                 :ds        ds-atom}]
        v (mapv #(t/load-module %) config)]
    (reset! m-config v)))


(defroutes app-routes
   (GET "/" _ (resp/resource-response "index.html" {:root "public"}))
   (route/resources "/")
   (route/not-found {:status 200 :body "Not found"}))


(defn warp-log [handler]
  (fn [req]
    (try
      (log/info "Http request  " req)
      (handler req)
      (catch Exception e
        (do
          (log/info "Execption in server " (.getMessage e)))))))


(def http-handler
  (-> app-routes
      (t/warp-dadysql-handler m-config)
      (warp-log)))


(defn -main
  [& args]
  (let [[port] args
        p (or port 3000)]
    (log/info "Starting server at  " p)
    (init-state)
    (im/run http-handler {:port p
                          :host "0.0.0.0"})))




