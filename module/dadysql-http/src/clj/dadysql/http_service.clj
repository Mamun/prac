(ns dadysql.http-service
  (:require [clojure.tools.logging :as log]
            [ring.middleware.params :as p]
            [ring.middleware.multipart-params :as mp]
            [ring.middleware.keyword-params :as kp]
            [ring.middleware.format-params :as fp]
            [ring.middleware.format-response :as fr]
            [dady.fail :as f]
            [dadysql.util :as u]
            [dadysql.http-request :as req]
            [dadysql.http-response :as res]))


(defn- http-response [v]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    v})


(defn ok-response [v]
  (-> [v nil]
      (http-response)))


(defn error-response [e]
  (-> [nil e]
      (http-response)))


(defn response
  [m]
  (if (f/failed? m)
    (error-response (into {} m))
    (ok-response m)))


(defn endpoint-type
  [{:keys [request-method content-type]}]
  (if (and
        (= request-method :post)
        (or (clojure.string/includes? content-type "transit")
            (clojure.string/includes? content-type "json")))
    u/api-endpoint
    u/url-endpoint))


(defn warp-pull [handler]
  (fn [ring-request]
    (let [type (endpoint-type ring-request)
          req (req/as-request ring-request type)]
      (response
        (f/try->> req
                  (handler)
                  (res/as-response type req))))))

(defn warp-push [handler]
  (fn [ring-request]
    (let [type (endpoint-type ring-request)
          req (req/as-request ring-request type)]
      (response
        (f/try->> req
                  (handler))))))



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