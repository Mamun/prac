(ns tiesql.http-service
  (:require [clojure.tools.logging :as log]
            [tiesql.common :as c]
    ;    [tiesql.util :as ]
            [tiesql.util :as u]
            [clojure.tools.reader.edn :as edn]
            [tiesql.jdbc :as tj]))





(defn filter-nil-value
  [m]
  (->> m
       (filter (comp not nil? val))
       (into {})))


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
  [response req]
  (if (= :string (:output req))
    (mapv u/postwalk-stringify-keys response)
    response))



(defn endpoint-type
  [{:keys [request-method content-type]}]
  ;(log/info request-method)
  ;(log/info content-type)
  #_(log/info (and
                (= request-method :post)
                (or (re-find #"application/transit+json" content-type)
                    (re-find #"application/json" content-type))))
  (if (and
        (= request-method :post)
        (or (clojure.string/includes? content-type "application/transit+json")
            (clojure.string/includes? content-type "application/json")))
    u/api-endpoint
    u/url-endpoint))


(defmulti parse-request (fn [t _] t))


(defmethod parse-request u/api-endpoint
  [_ params]
  (log/info "api end point ")
  (-> params
      (update-in [u/tiesql-name] (fn [w] (if w
                                           (if (sequential? w)
                                             (mapv c/as-keyword (remove nil? w))
                                             (keyword w)))))
      (filter-nil-value)))


(defn as-keyword-value
  [m]
  (into {}
        (for [[k v] m]
          [(keyword k) (keyword v)])))


(defmethod parse-request u/url-endpoint
  [_ params]
  (log/info " url endpoint ")
  (let [r-params (dissoc params u/tiesql-name :rformat :pformat :gname)
        q-name (when-let [w (u/tiesql-name params)]
                 (if (sequential? w)
                   (mapv c/as-keyword (remove nil? w))
                   (keyword w)))
        other (-> params
                  (select-keys [:gname :rformat :pformat])
                  (as-keyword-value))]
    (-> other
        (assoc :name q-name)
        (assoc :params (read-params-string r-params))
        (filter-nil-value))))


(defn tiesql-request
  [{:keys [params] :as req}]
  (if params
    (let [type (endpoint-type req)]
      (-> (parse-request type params)
          (param-keywordize-keys)))
    (c/fail "No params is set in http request ")))



(defn- apply-op
  [request-m handler ds tms]
  (->> (seq request-m)
       (apply concat)
       (cons tms)
       (cons ds)
       (apply handler)))


(defn pull
  [ds tms ring-request]
  (let [req (tiesql-request ring-request)
        res (c/try-> req
                     (apply-op tj/pull ds tms))]
    (-> res
        (response-stringify req)
        (u/response-format))))


(defn push!
  [ds tms ring-request]
  (let [res (c/try-> (tiesql-request ring-request)
                     (apply-op tj/push! ds tms))]
    (-> res
        (u/response-format))))
