(ns dadysql.http-service
  (:require [dady.fail :as f]
            [dadysql.util :as u]
            [dadysql.jdbc :as tj]
            [dadysql.http-middleware :as m]
            [dadysql.http-request :as req]
            [dadysql.http-response :as res]))



(defn http-response [v]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    v})


(defn ok-response [v]
  (-> (vector v nil)
      (http-response)))


(defn error-response [e]
  (-> (vector nil e)
      (http-response)))


(defn as-response
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


(defn- warp-pull-handler
  [ds tms]
  (fn [ring-request]
    (let [type (endpoint-type ring-request)
          req (req/as-request ring-request type)]
      (as-response
        (f/try->> req
                  (tj/pull ds tms)
                  (res/as-response type req))))))


(defn- warp-push-handler
  [ds tms]
  (fn [ring-request]
    (let [type (endpoint-type ring-request)
          req (req/as-request ring-request type)]
      (as-response
        (f/try->> req
                  (tj/push! ds tms))))))


(def warp-default-middleware m/warp-default)


(defn pull [ds tms ring-request]
  (let [handler (m/warp-default (warp-pull-handler ds tms))]
    (handler ring-request)))


(defn push [ds tms ring-request]
  (let [handler (m/warp-default (warp-push-handler ds tms))]
    (handler ring-request)))

