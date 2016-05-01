(ns tiesql.middleware
  (:require [clojure.tools.logging :as log]
            [tiesql.common :as c]
            [ring.middleware.params :as p]
            [ring.middleware.multipart-params :as mp]
            [ring.middleware.keyword-params :as kp]
            [ring.middleware.format-params :as fp]
            [ring.middleware.format-response :as fr]))


(defn ok-response [v]
  [v nil])

(defn error-response [e]
  [nil e])


(defn warp-http-response
  [handler]
  (fn [req]
    (let [body (handler req)]
      {:status  200
       :headers {}
       :body    body})))


(defn warp-log-request
  [handler log?]
  (fn [req]
    (when log?
      (log/info "http request  ---------------" req))
    (handler req)))


(defn warp-default
  [handler & {:keys [encoding log?]
              :or   {encoding "UTF-8"
                     log?     false}}]
  (-> handler
      (warp-log-request log?)
      (warp-http-response)
      (kp/wrap-keyword-params)
      (p/wrap-params :encoding encoding)
      (mp/wrap-multipart-params)
      (fp/wrap-restful-params)
      (fr/wrap-restful-response)
      ))

