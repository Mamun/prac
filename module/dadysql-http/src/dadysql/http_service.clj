(ns dadysql.http-service
  (:require [clojure.tools.logging :as log]
            [dady.common :as c]
            [dady.fail :as f]
            [dady.walk :as w]
            [dadysql.util :as u]
            [dadysql.jdbc :as tj]
            [clojure.tools.reader.edn :as edn]))


(defn http-response
  [m]
  (let [w
        (if (f/failed? m)
          [nil (into {} m)]
          [m nil])]
    {:status  200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body    w}))


(defn filter-nil-value
  [m]
  (->> m
       (filter (comp not nil? val))
       (into {})))


(defn as-keyword-value
  [m]
  (into {}
        (for [[k v] m]
          [(keyword k) (keyword v)])))



(defn read-params-string
  [params]
  (->> params
       (reduce (fn [acc [k v]]
                 (let [v1 (edn/read-string v)]
                   (if (symbol? v1)
                     (assoc acc k v)
                     (assoc acc k v1)))
                 ) {})))


(defn param-keywordize-keys
  [req]
  (if (= :string (:input req))
    (assoc req :params (clojure.walk/keywordize-keys (:params req)))
    req))


(defn response-stringify
  [req response]
  (if (= :string (:output req))
    (mapv (partial w/postwalk-replace-key-with w/keyword->str) response)
    response))



(defn endpoint-type
  [{:keys [request-method content-type]}]
  (if (and
        (= request-method :post)
        (or (clojure.string/includes? content-type "transit")
            (clojure.string/includes? content-type "json")))
    u/api-endpoint
    u/url-endpoint))


(defmulti request-format (fn [t _] t))


(defmethod request-format u/api-endpoint
  [_ params]
  (-> params
      (update-in [u/dadysql-name] c/as-keyword-batch)
      (filter-nil-value)))




(defmethod request-format u/url-endpoint
  [_ params]
  (let [r-params (dissoc params u/dadysql-name :rformat :pformat :gname)
        q-name (c/as-keyword-batch (u/dadysql-name params))
        other (-> params
                  (select-keys [:gname :rformat :pformat])
                  (as-keyword-value))]
    (-> other
        (assoc :name q-name)
        (assoc :params r-params)
        (filter-nil-value))))



(defmulti resposne-format (fn [t _] t))


(defmethod resposne-format u/api-endpoint
  [_ output]
  output)


(defmethod resposne-format u/url-endpoint
  [_ output]
  (->> output
       (w/postwalk-replace-value-with u/as-str)
       (w/postwalk-replace-key-with w/keyword->str)))


(defn is-params-not-nil? [params]
  (if params
    params
    (f/fail "No params is set in http request")))


(defn pull-handler
  [ds tms ring-request]
  (let [type (endpoint-type ring-request)
        req (f/try->> ring-request
                      (:params)
                      (is-params-not-nil?)
                      (request-format type)
                      (param-keywordize-keys))]
    (http-response
      (f/try->> req
                (tj/pull ds tms)
                (response-stringify req)
                (resposne-format type)))))


(defn push-handler
  [ds tms ring-request]
  (let [type (endpoint-type ring-request)]
    (http-response
      (f/try->> ring-request
                (:params)
                (is-params-not-nil?)
                (request-format type)
                (param-keywordize-keys)
                (tj/push! ds tms)))))



