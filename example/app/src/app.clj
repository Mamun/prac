(ns app
  (:use compojure.core)
  (:require [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            [ring.util.response :as resp]
            [ring.middleware.params :as p]
            [ring.middleware.multipart-params :as mp]
            [ring.middleware.keyword-params :as kp]
            [ring.middleware.format-params :as fp]
            [ring.middleware.format-response :as fr]
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


(defn app-routes
  [{:keys [tiesql-file tiesql-init]}]
  (let [ds {:datasource (ComboPooledDataSource.)}
        tms-atom (atom (hs/read-init-validate-file tiesql-file ds tiesql-init))
        ds-atom  (atom ds)]
    (-> (routes
          (GET "/" [] (resp/response "App is running"))
          (GET "/hello" [] (resp/response "back from hello"))
          (route/resources "/")
          (route/not-found "Not found"))
        (hs/warp-tiesql ds-atom tms-atom)                   ;; Data service here
        (kp/wrap-keyword-params)
        (p/wrap-params)
        (mp/wrap-multipart-params)
        (fp/wrap-restful-params)
        (fr/wrap-restful-response))))


(defn boot
  [& {:keys [file-name]
      :or   {file-name "tiesql.edn"}}]
  (let [{:keys [port] :as w} (read-app-file file-name)]
    (im/run (app-routes w) {:port port})
    (tj/start-sql-execution-log)))



(defn -main
  [& args]
  (println "Starting tie app  ")
  (boot))
