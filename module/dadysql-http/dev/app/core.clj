(ns app.core
  (:use compojure.core)
  (:require [ring.util.response :as resp]
            [dadysql.http-service :as dhttp]
            [ring.middleware.dadysql :as t]
            [ring.middleware.defaults :refer :all]
            [compojure.route :as route]
            [immutant.web :as im]
            [dadysql.jdbc :as tj]
            [dady.common :as cc]
            [clojure.tools.logging :as log])
  (:import
    [com.mchange.v2.c3p0 ComboPooledDataSource])
  (:gen-class))

;(defonce ds-atom (atom nil))
(defonce m-config (atom nil))


(defn init-state []
  (let [ds {:datasource (ComboPooledDataSource.)}
        v (mapv #(t/load-file-one %) [{:file-name "tie.edn.sql"
                                       :init-name [:create-ddl :init-data]
                                       :ds        ds}
                                      {:file-name "tie2.edn.sql"
                                       :ds        ds}
                                      {:file-name "tie5.edn.sql"
                                       :ds        ds}])]
    (reset! m-config v)))


(defn api-routes []
  (-> (routes
        (GET "/" _ (dhttp/ok-response {:a3 3}))
        (GET "/1" _ (dhttp/ok-response {:a3 3}))
        ;(GET "/2" req (ser/pull @ds-atom @tms-atom req))
        (POST "/hello" _ (dhttp/ok-response {:a 7})))
      (dhttp/warp-default)))


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
      ; (println "---" w)
      w
      )
    ))


(def http-handler
  (-> app-routes
      (t/warp-dadysql-handler m-config)
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
             ;    (tj/warp-db-do @ds-atom (:dadysql-init app-config))
             ;   (tj/warp-validate-dml! @ds-atom)
             #_(reset! tms-atom))

  (println @tms-atom)

  )


