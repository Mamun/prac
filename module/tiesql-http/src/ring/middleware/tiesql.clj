(ns ring.middleware.tiesql
  (:require [clojure.tools.logging :as log]
            [tiesql.common :as c]
            [tiesql.util :as u]
            [tiesql.http-service :as h]
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


(defn tiesql-handler [ds tms t req]
  ;(log/info "--------------" tms )
  (if (c/failed? tms)
    (do
      (-> tms
          ;(response-stringify req)
          (u/response-format)
          (response)))
    (if (= "/pull" t)
      (response (h/pull ds tms req))
      (response (h/push! ds tms req)))))


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
               (tj/validate-dml! ds)
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
  [handler & {:keys [pull-path push-path log? tms ds encoding]
              :or   {pull-path "/pull"
                     encoding  "UTF-8"
                     push-path "/push"
                     log?      false}}]
  (let [p-set #{pull-path push-path}]
    (fn [req]
      (let [ds (or (:ds req) @ds)
            tms (or (:tms req)
                    (try! reload-tms tms ds))
            request-path (or (:path-info req)
                             (:uri req))]
        (if (contains? p-set request-path)
          ((-> (partial tiesql-handler ds tms request-path) ;; Data service here
               (kp/wrap-keyword-params)
               (p/wrap-params :encoding encoding)
               (mp/wrap-multipart-params)
               (fp/wrap-restful-params)
               (fr/wrap-restful-response)
               (log-request log?)) req)
          (handler req))))))



(defn get-sql-file-value [tms-atom]
  (->> @tms-atom
       (vals)
       (mapv (fn [w] (select-keys w [c/name-key c/model-key c/sql-key])))))




