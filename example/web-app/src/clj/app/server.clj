(ns app.server
  (:use compojure.core)
  (:require [ring.middleware.webjars :refer [wrap-webjars]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [compojure.route :as route]
            [immutant.web :as im]
            [ring.middleware.tiesql :as hs]
            [clojure.tools.logging :as log]
            [app.state :as s]
            [tiesql.middleware :as m]
            [app.api-routes :as api]
            [app.html-routes :as htr])
  (:gen-class))





(defroutes
  app-routes
  htr/html-page-routes

  (context "/api" _ (-> api/api-routes
                        (m/warp-default)))
  (route/resources "/")
  (route/not-found {:status 200
                    :body   "Not found From app "}))


(defn warp-log [handler]
  (fn [req]
    (log/info "-----------------" req)
    (handler req)
    ))


(def http-handler
  (-> app-routes
      (hs/warp-tiesql-handler :tms s/tms-atom :ds s/ds-atom)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      ;  (warp-log)
      (wrap-webjars)
      ;  wrap-with-logger
      ;wrap-gzip
      ))


(defn -main
  [& args]
  (println "Starting tie app  ")
  (s/init-state)
  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        }))


(comment


  (im/run http-handler {:port 3000
                        ;:host "0.0.0.0"
                        })

  )