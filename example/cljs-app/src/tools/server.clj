(ns tools.server
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
            [compojure.route :as route]
            [immutant.web :as im]
            [com.stuartsierra.component :as c]
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


(defn log-request
  [handler]
  (fn [req]
    (when (or (.contains (:uri req) "pull")
              (.contains (:uri req) "push"))
      (log/info (select-keys req [:params :uri :path-info])))
    (handler req)))


(defn app-routes
  [ds-atom tms-atom]
  (-> (routes
        (GET "/" [] (resp/response "App is running"))
        (GET "/hello" [] (resp/response "Back from hello"))
        (route/resources "/")
        (route/not-found {}))
      (hs/warp-tiesql ds-atom tms-atom)                     ;; Data service here
      (log-request)
      (wrap-webjars)
      (kp/wrap-keyword-params)
      (p/wrap-params)
      (mp/wrap-multipart-params)
      (fp/wrap-restful-params)
      (fr/wrap-restful-response)))


(defrecord App [config-file config connection tms routes]
  c/Lifecycle
  (start [component]
    (let [{:keys [tiesql-file tiesql-init] :as c} (read-app-file config-file)
          ds-atom (atom {:datasource (ComboPooledDataSource.)})
          tms-atom (atom (hs/read-init-validate-file tiesql-file @ds-atom tiesql-init))]
      (-> component
          (assoc :config c)
          (assoc :connection ds-atom)
          (assoc :tms tms-atom)
          (assoc :routes (app-routes ds-atom tms-atom)))))
  (stop [component]
    (log/info "Stoping app")
    (.close (:datasource @connection))
    (dissoc component :connection :routes :tms :config)))


(defn app-system
  ([] (app-system "tiesql.edn"))
  ([config-file]
   (map->App {:config-file config-file})))


(defrecord WebServer [app server]
  c/Lifecycle
  (start [component]
    (let [port (get-in app [:config :port])
          routes (get-in app [:routes])
          s (im/run routes {:port port
                            :host "0.0.0.0"})]
      (log/info "Starting server.. ")
      (assoc component :server s)))
  (stop [component]
    (log/info "Stoping server ")
    (when server
      (im/stop server))
    (dissoc component :server)))


(defn boot-system
  ([] (boot-system "tiesql.edn"))
  ([config-file]
   (-> (c/system-map
         :server (map->WebServer {})
         :app (map->App {:config-file config-file}))
       (c/system-using
         {:server {:app :app}}))))


(defn boot
  []
  (do
    (c/start (boot-system))
    (tj/start-sql-execution-log)))


(comment



  (-> (boot-system)
      (c/start)
      (c/stop))


  (-> (app-system)
      (c/start)
      (c/stop))


  (def system (boot-system))

  (clojure.pprint/pprint system)

  (alter-var-root #'system c/start)

  (alter-var-root #'system c/stop)

  )


