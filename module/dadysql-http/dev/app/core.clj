(ns app.core
  (:use compojure.core)
  (:require [ring.util.response :as resp]
            [dadysql.http-service :as dhttp]
            [ring.middleware.dadysql :as t]
            [ring.middleware.defaults :refer :all]
            [compojure.route :as route]
            [immutant.web :as im]
            [dadysql.jdbc :as tj]
            [dadysql.clj.common :as cc]
            [clojure.tools.logging :as log])
  (:import
    [com.mchange.v2.c3p0 ComboPooledDataSource])
  (:gen-class))

;(defonce ds-atom (atom nil))
(defonce m-config (atom nil))

#_(comment

  (tj/write-spec-to-file (tj/read-file "tie.edn.sql") "dev" )
  (tj/write-spec-to-file (tj/read-file "tie2.edn.sql") "dev" )

  (t/load-module {:file-name "tie.edn.sql"
                  :init-name [:init-db :init-data]
                  ;:spec-dir "dev"
                  :ds        {:datasource (ComboPooledDataSource.)}})

  )


(defn init-state []
  (let [ds {:datasource (ComboPooledDataSource.)}
        config [{:file-name "tie.edn.sql"
                 :init-name [:init-db :init-data]
                 :spec-dir "dev"
                 :ds        ds}
                {:file-name "tie2.edn.sql"
                 :spec-dir "dev"
                 :ds        ds}
                {:file-name "tie5.edn.sql"
                 :spec-dir "dev"
                 :ds        ds}]
        v (mapv #(t/load-module %) config )]
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


(defn warp-execption [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (do
          (log/info "Execption in server " (.getMessage e))
          )
        ))
    )
  )


(def http-handler
  (-> app-routes
      (t/warp-dadysql-handler m-config )
      (warp-execption)
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







  ;(init-state)
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


