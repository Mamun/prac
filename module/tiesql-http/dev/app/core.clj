(ns app.core
  (:use compojure.core)
  (:require [ring.util.response :as resp]
            [ring.middleware.tiesql :as hs]
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


(defroutes app-handler
           (GET "/" _ (resp/resource-response "index.html" {:root "public"}))
           (route/resources "/")
           (route/not-found {:status 200 :body "Not found"}))


(defn warp-log [handler]
  (fn [req]
    (log/info "-----------------" req)
    (let [w (handler req)]
      (log/info "-----------------" w)
      w
      )
    ))


(def http-handler
  (-> app-handler
      (hs/warp-tiesql-handler :tms tms-atom :ds ds-atom)
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


