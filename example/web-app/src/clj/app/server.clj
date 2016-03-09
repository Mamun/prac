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
      ;wrap-with-logger
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


(comment

   (clojure.string/includes? "application/transit+json; charset=UTF-8" "application/transit+json" )



  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        })

  {:ssl-client-cert nil, :remote-addr "127.0.0.1", :handler-type :undertow,
     :headers         {"origin" "http://localhost:3000", "host" "localhost:3000", "user-agent" "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36", "content-type" "application/transit+json; charset=UTF-8", "cookie" "s_fid=40812FA87D52F8B3-1E11AD5C5828DB61; 55_previousPage=DE%20%3A%20B2C%20%3A%20Credit%20%3A%20PL%20%3A%20Form%20%3A%201.%20Project; 55_allsourcesPV=false; 55_campaign1stPV=false", "content-length" "111", "referer" "http://localhost:3000/pull?name=get-dept-list", "connection" "keep-alive", "accept" "application/transit+json; charset=utf-8", "accept-language" "en-US,en;q=0.8", "accept-encoding" "gzip, deflate"},
     :server-port     3000, :content-length 111,
     :content-type    "application/transit+json; charset=UTF-8",
     :path-info       "/pull", :character-encoding "UTF-8",
     :context         "", :uri "/pull", :server-name "localhost", :query-string nil,
      :scheme :http, :request-method :post}


  {"origin" "http://localhost:3001", "host" "localhost:3001", "user-agent" "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36", "content-type" "application/transit+json; charset=UTF-8", "cookie" "s_fid=40812FA87D52F8B3-1E11AD5C5828DB61; 55_previousPage=DE%20%3A%20B2C%20%3A%20Credit%20%3A%20PL%20%3A%20Form%20%3A%201.%20Project; 55_allsourcesPV=false; 55_campaign1stPV=false", "content-length" "137", "referer" "http://localhost:3001/pull?name=get-dept-list", "connection" "keep-alive", "accept" "application/transit+json; charset=utf-8", "accept-language" "en-US,en;q=0.8", "accept-encoding" "gzip, deflate"}


  )