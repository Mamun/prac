(ns ring.middleware.tiesql
  (:require [clojure.tools.logging :as log]
            [tiesql.common :as c]
            [ring.middleware.tiesql-util :as u]
            [clojure.tools.reader.edn :as edn]
            [tiesql.jdbc :as tj]
            [ring.middleware.params :as p]
            [ring.middleware.multipart-params :as mp]
            [ring.middleware.keyword-params :as kp]
            [ring.middleware.format-params :as fp]
            [ring.middleware.format-response :as fr]))


(defn response
  [body]
  {:status  200
   :headers {}
   :body    body})


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
    (mapv c/stringify-keys2 response)
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
        (u/response-format)
        (response))))


(defn push!
  [ds tms ring-request]
  (let [res (c/try-> (tiesql-request ring-request)
                   (apply-op tj/push! ds tms))]
    (-> res
        (u/response-format)
        (response))))


(defn tiesql-handler [ds tms t req]
  ;(log/info "--------------" tms )
  (if (c/failed? tms)
    (do
    ;  (log/info "--------------" (u/response-format tms) )
      (-> tms
          ;(response-stringify req)
          (u/response-format)
          (response)))
    (if (= "/pull" t)
      (pull ds tms req)
      (push! ds tms req))))


(defn log-request
  [handler log?]
  (fn [req]
    (if log?
      (let [res (handler req)]
        (log/info "tiesql reqest ---------------" res)
        res)
      (handler req))))



(defn- reload-tms
  ([tms-atom ds]
   (when (get-in @tms-atom [c/global-key c/file-reload-key])
     (c/try->> (get-in @tms-atom [c/global-key c/file-name-key])
             (tj/read-file)
             (tj/warp-validate-dml! ds)
             (reset! tms-atom)))
   @tms-atom))


(defn try!
  [form & v]
  (try
    (apply form v)
    (catch Exception e
      (log/error e)
      (c/fail {:msg "Error in server "}))))


;ds-atom tms-atom
(defn warp-tiesql-handler
  "Warper that tries to do with tiesql. It should use next to the ring-handler. If path-in is matched with
   pull-path or push-path then it will API and return result.

   handler: Ring handler
   ds-atom: Clojure datasource as atom
   tms-atom: tiesql file as atom
   pull-path and push path string

  "
  [handler & {:keys [pull-path push-path log? tms ds]
              :or   {pull-path "/pull"
                     push-path "/push"
                     log?      false}}]
  #_{:pre [(instance? clojure.lang.Ref )]}
  (let [p-set #{pull-path push-path}]
    (fn [req]
      ;(log/info "----" @tms )
      (let [ds (or (:ds req) @ds)
            tms (or (:tms req)
                    (try! reload-tms tms ds))
            request-path (or (:path-info req)
                             (:uri req))]
        (if (contains? p-set request-path)
          ((-> (partial tiesql-handler ds tms request-path) ;; Data service here
               (kp/wrap-keyword-params)
               (p/wrap-params)
               (mp/wrap-multipart-params)
               (fp/wrap-restful-params)
               (fr/wrap-restful-response)
               (log-request log?)) req)
          (handler req))))))



(defn get-sql-file-value [tms-atom]
  (->> @tms-atom
       (vals)
       (mapv (fn [w] (select-keys w [c/name-key c/model-key c/sql-key])))))




