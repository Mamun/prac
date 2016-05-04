(ns app.core
  (:use compojure.core)
  (:require [ring.util.response :as resp]
            [tiesql.middleware :as m]
            [ring.middleware.tiesql :as t]
            [ring.middleware.defaults :refer :all]
            [compojure.route :as route]
            [immutant.web :as im]
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
    (cc/try->> (tj/read-file "tie.edn.sql")
               (tj/db-do @ds-atom [:create-ddl :init-data])
               (tj/validate-dml! @ds-atom)
               (reset! tms-atom))))


(defn api-routes []
  (-> (routes
        (GET "/" _ (m/ok-response {:a3 3}))
        (POST "/hello" _ (m/ok-response {:a 7})))
      (m/warp-default)))

#_(defroutes api-routes
             (GET "/" [] (hs/ok-response {:result "Hello from api called "})))


(defroutes app-routes
           (GET "/" _ (resp/resource-response "index.html" {:root "public"}))
           (context "/api" _ (api-routes))
           (route/resources "/")
           (route/not-found {:status 200 :body "Not found"}))


(defn warp-log [handler]
  (fn [req]
    ;(log/info "Request -----------------" req)
    (let [w (handler req)]
     ; (log/info "Response -----------------" w)
      w
      )
    ))


(def http-handler
  (-> app-routes
      (t/warp-tiesql-handler :tms tms-atom :ds ds-atom)
      (warp-log)))


(defn -main
  [& args]
  (println "Starting tie app 3000 ")
  (init-state)
  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        }))



;(def hello (delay (atom {:a 3})))

(comment

  (init-state)
  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        })

  ; (swap! @hello (fn [r] (assoc r :b 4) ))
  ; (println @@hello)




  (cc/try->> (tj/read-file "tie.edn.sql")
             ;    (tj/warp-db-do @ds-atom (:tiesql-init app-config))
             ;   (tj/warp-validate-dml! @ds-atom)
             #_(reset! tms-atom))

  (println @tms-atom)

  )


