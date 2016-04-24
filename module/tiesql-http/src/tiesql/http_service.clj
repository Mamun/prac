(ns tiesql.http-service
  (:require [clojure.tools.logging :as log]
            [tiesql.common :as c]
            [tiesql.util :as u]
            [clojure.tools.reader.edn :as edn]))



(defn filter-nil-value
  [m]
  (->> m
       (filter (comp not nil? val))
       (into {})))


(defn read-params-string
  [params]
  (->> params
       (reduce (fn [acc [k v]]
                 (log/info "type --" (type v))
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
    (mapv (partial u/postwalk-replace-key-with u/keyword->str) response)
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


(defmethod request-format u/url-endpoint
  [_ params]
  (log/info " url endpoint " params)
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
        (assoc :params r-params #_(read-params-string r-params))
        (filter-nil-value))))



(defmulti resposne-format (fn [t _] t))


(defmethod resposne-format u/api-endpoint
  [_ output]
  output)


(defmethod resposne-format u/url-endpoint
  [_ output]
  (->> output
       (u/postwalk-replace-value-with u/as-str)
       (u/postwalk-replace-key-with u/keyword->str)))


(defn is-params-not-nil? [params]
  (if params
    params
    (c/fail "No params is set in http request")))


(defn pull [pull-handler ring-request]
  (let [type (endpoint-type ring-request)
        req (c/try->> ring-request
                      (:params)
                      (is-params-not-nil?)
                      (request-format type)
                      (param-keywordize-keys))]
    (u/response-format
      (c/try->> req
                (pull-handler)
                (response-stringify req)
                (resposne-format type)))))


(defn push [push-handler ring-request]
  (let [type (endpoint-type ring-request)]
    (u/response-format
      (c/try->> ring-request
                (:params)
                (is-params-not-nil?)
                (request-format type)
                (param-keywordize-keys)
                (push-handler)))))



(defn warp-pull
  [handler]
  (fn [ring-request]
    (pull handler ring-request) ))


(defn warp-push!
  [handler]
  (fn [ring-request]
    (push handler ring-request) ))


(defn warp-tiesql [handler t]
  (fn [ring-request]
    (if (= t "/push")
      ((warp-push! handler) ring-request)
      ((warp-pull handler) ring-request)
      )))