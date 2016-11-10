(ns dadysql.http-service
  (:require [clojure.tools.logging :as log]
            [clojure.walk :as w]
            [ring.middleware.params :as p]
            [ring.middleware.multipart-params :as mp]
            [ring.middleware.keyword-params :as kp]
            [ring.middleware.format-params :as fp]
            [ring.middleware.format-response :as fr]
            [dadysql.clj.fail :as f]
            [dadysql.clj.walk :as dw]
            [dadysql.clj.common :as c]
            [dadysql.http-util :as u]))


(defn debug [v]
  (do
    (log/info "############################# debug " v)
    v))


(defn- http-response [v]
  {:status  200
   :headers {"Content-Type" "text/html; charset=utf-8"}
   :body    v})


(defn ok-response [v]
  (http-response [v nil]))


(defn error-response [e]
  (http-response [nil e]))


(defn response
  [m]
  (if (f/failed? m)
    (do
      (log/info "failed response " (into {} m))
      (error-response (str (into {} m))))
    (ok-response m)))


(defn endpoint-type
  [{:keys [request-method content-type]}]
  (if (and
        (= request-method :post)
        (or (clojure.string/includes? content-type "transit")
            (clojure.string/includes? content-type "json")))
    :dadysql/api-endpoint
    :dadysql/url-endpoint))


(defn as-namespace-keyword [m]
  (let [v {:name          :dadysql.core/name
           :group         :dadysql.core/group
           :output-format :dadysql.core/output-format
           :param-format  :dadysql.core/param-format}]
    (w/postwalk (fn [x]
                  (if-let [v (get v x)]
                    v
                    x)) m)))



(defmulti request-format (fn [t _] (endpoint-type t)))


(defmethod request-format :dadysql/api-endpoint
  [_ params]
  (-> params
      (update-in [:name] c/as-keyword-batch)
      (u/filter-nil-value)))

;;Strill need to case to type
(defmethod request-format :dadysql/url-endpoint
  [_ params]
  (let [params (clojure.walk/keywordize-keys params)
        r-params (dissoc params
                         :dadysql.core/name
                         :dadysql.core/group
                         :dadysql.core/output-format
                         :dadysql.core/param-format)
        q-name (c/as-keyword-batch (:dadysql.core/name params))
        other (-> params
                  (select-keys [:dadysql.core/group :dadysql.core/output-format :dadysql.core/param-format])
                  (u/as-keyword-value))]
    (-> other
        (assoc :dadysql.core/name q-name)
        (assoc :dadysql.core/param r-params)
        (u/filter-nil-value))))



(defmulti resposne-format (fn [t _] (endpoint-type t)))


(defmethod resposne-format :dadysql/api-endpoint
  [_ output]
  output)


(defmethod resposne-format :dadysql/url-endpoint
  [_ output]
  (->> output
       (dw/postwalk-replace-value-with u/as-str)
       (dw/postwalk-replace-key-with dw/keyword->str)))



(defn is-valid?
  [params]
  (if params
    params
    (f/fail "No params is set in http request")))



;(def {:dadysql} )


(defn pull-handler [api-handler ring-request]
  (f/try->> ring-request
            (:params)
            (is-valid?)
            (as-namespace-keyword)
            (request-format ring-request)
            (api-handler)
            (resposne-format ring-request)))


(defn push-handler [api-handler ring-request]
  (f/try->> ring-request
            (:params)
            (is-valid?)
            (as-namespace-keyword)
            (request-format ring-request)
            (api-handler)))




(defn warp-pull [handler]
  (fn [ring-request]
    (response (pull-handler handler ring-request))))


(defn warp-push [handler]
  (fn [ring-request]
    (response (push-handler handler ring-request))))


(defn warp-log-request
  [handler log?]
  (fn [req]
    (when log?
        (log/info "After warp-log-request  ---------------" (:params req)))
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