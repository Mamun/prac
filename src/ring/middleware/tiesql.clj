(ns ring.middleware.tiesql
  (:require [clojure.tools.logging :as log]
            [tiesql.common :refer :all]
            [ring.middleware.tiesql-util :as u]
            [clojure.tools.reader.edn :as edn]
            [tiesql.jdbc :as tj]))




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
    (mapv stringify-keys2 response)
    response))



(defn endpoint-type
  [{:keys [request-method content-type]}]
  ;(log/info request-method)
  ;(log/info content-type)
  #_(log/info (and
              (= request-method :post)
              (or (re-find #"application/transit+json" content-type )
                  (re-find #"application/json" content-type ))))
  (if (and
        (= request-method :post)
        (or (clojure.string/includes? content-type "application/transit+json" )
            (clojure.string/includes? content-type "application/json" )))
    u/api-endpoint
    u/url-endpoint))


(defmulti parse-request (fn [t _] t))


(defmethod parse-request u/api-endpoint
  [_ params]
  (log/info "api end point ")
  (-> params
      (update-in [u/tiesql-name] (fn [w] (if w
                                           (if (sequential? w)
                                             (mapv as-keyword (remove nil? w))
                                             (keyword w)))))
      (filter-nil-value)))


(defn as-keyword-value
  [m]
  (into {}
        (for [[k v] m]
          [(keyword k) (keyword v)])))


(defmethod parse-request u/url-endpoint
  [_ params]
  (log/info "url endpoint ")
  (let [r-params (dissoc params u/tiesql-name :rformat :pformat :gname)
        q-name (when-let [w (u/tiesql-name params)]
                 (if (sequential? w)
                   (mapv as-keyword (remove nil? w))
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
    (fail "No params is set in http request ")))




(defn read-init-validate-file
  ([file-name ds] (read-init-validate-file file-name ds nil))
  ([file-name ds init-name]
   (let [v (tj/read-file file-name)]
     (when init-name
       (tj/db-do ds v init-name))
     (tj/validate-dml! ds (tj/get-dml v))
     v)))


(defn reload-validate-file
  ([tie-atom ds-atom]
   (when (get-in @tie-atom [global-key file-reload-key])
     (let [f-name (get-in @tie-atom [global-key file-name-key])
           new-tms (read-init-validate-file f-name @ds-atom)]
       (log/info "file is reloading ---" f-name)
       (reset! tie-atom new-tms)))
   tie-atom))


(defn get-sql-file-value [tms-atom]
  (->> @tms-atom
       (vals)
       (mapv (fn [w] (select-keys w [name-key model-key sql-key])))))


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
        res (try-> req
                   (apply-op tj/pull ds tms))]
    (-> res
        (response-stringify req)
        (u/response-format)
        (response))))


(defn push!
  [ds tms ring-request]
  (let [res (try-> (tiesql-request ring-request)
                   (apply-op tj/push! ds tms))]
    (-> res
        (u/response-format)
        (response))))


(defn warp-tiesql
  "Warper that tries to do with tiesql. It should use next to the ring-handler. If path-in is matched with
   pull-path or push-path then it will API and return result.

   handler: Ring handler
   ds-atom: Clojure datasource as atom
   tms-atom: tiesql file as atom
   pull-path and push path string

  "
  [handler ds-atom tms-atom & {:keys [pull-path push-path]
                               :or   {pull-path "/pull"
                                      push-path "/push"}}]
  (fn [req]
    (condp = (or (:path-info req)
                 (:uri req))
      pull-path (let [new-tms-atom (reload-validate-file tms-atom ds-atom)]
                  (pull @ds-atom @new-tms-atom req))
      push-path (let [new-tms-atom (reload-validate-file tms-atom ds-atom)]
                  (push! @ds-atom @new-tms-atom req))
      (handler req))))
