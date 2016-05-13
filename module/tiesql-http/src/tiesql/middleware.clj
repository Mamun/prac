(ns tiesql.middleware
  (:require [clojure.tools.logging :as log]
            [ring.middleware.params :as p]
            [ring.middleware.multipart-params :as mp]
            [ring.middleware.keyword-params :as kp]
            [ring.middleware.format-params :as fp]
            [ring.middleware.format-response :as fr]))


(defn ok-response [v]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    [v nil]})


(defn error-response [e]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    [nil e]})


(defn warp-log-request
  [handler log?]
  (fn [req]
    (when log?
      (log/info "After warp-log-request  ---------------" req))
    (handler req)))


(defn warp-default
  [handler & {:keys [encoding log?]
              :or   {encoding "UTF-8"
                     log?     false}}]
  (-> handler
      (kp/wrap-keyword-params)
      (p/wrap-params :encoding encoding)
      (mp/wrap-multipart-params)
      (fp/wrap-restful-params)
      (fr/wrap-restful-response)
      (warp-log-request log?)))

