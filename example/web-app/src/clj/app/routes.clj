(ns app.routes
  (:use [compojure.route :as route]
        [compojure.core])
  (:require [ring.middleware.webjars :refer [wrap-webjars]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.middleware.dadysql :as hs]
            [clojure.tools.logging :as log]
    ;  [dadysql.middleware :as m]
            [dadysql.http-service :as h]
            [app.view :as v]
            [app.service :as api]
            [app.state :as s]))

(defn select-header [request]
  (select-keys request [:character-encoding :params :request-method
                        :content-length :uri :query-params :query-string
                        :context :remote-addr :path-info :server-name :session :server-port
                        :form-params :scheme :headers]))

(defroutes
  view-routes
  (GET "/" _ (v/index) )
  (GET "/index" _ (v/index))
  (GET "/contact" _ (v/contact)))


(defroutes
  admin-view-routes
  (GET "/" _ (v/admin-index) ))


(defroutes
  api-routes
  (GET "/postcode" request  (h/ok-response (select-header request)) )
  (GET "/deals" _           (h/response (api/load-deals) ) ))


(defroutes
  app-routes
  view-routes
  (context "/admin" _ admin-view-routes)
  (context "/api" _ (-> api-routes
                        (h/warp-default)))
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
      (hs/warp-dadysql-handler :tms s/tms-atom :ds s/ds-atom)
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      ;  (warp-log)
      (wrap-webjars)
      ;  wrap-with-logger
      ;wrap-gzip
      ))
