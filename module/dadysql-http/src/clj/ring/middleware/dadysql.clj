(ns ring.middleware.dadysql
  (:require [clojure.tools.logging :as log]
            [dady.fail :as f]
            [dadysql.http-service :as h]
            [dadysql.jdbc-io :as io]
            [dadysql.jdbc :as tj]))


#_{:ds        ds-atom
   :init-name []
   :file-name "tie.edn.sql"}


(defn load-file-one
  [{:keys [ds file-name init-name]}]
  (let [v (tj/read-file file-name)]
    (do
      (when init-name
        (io/db-do @ds (tj/select-name v {:dadysql.core/name init-name})))
      (io/validate-dml! @ds (tj/get-sql-statement @ds))
      {:ds  ds
       :url (str "/" (first (clojure.string/split file-name #"\.")))
       :tms (atom v) })))



(defn- reload-tms
  ([tms-atom ds]
   (when (get-in @tms-atom [:_global_ :dadysql.core/file-reload])
     (let [n-tms (tj/read-file (get-in @tms-atom [:_global_ :dadysql.core/file-name]))]
       (io/validate-dml! ds (tj/get-sql-statement n-tms))
       (reset! tms-atom n-tms)))
   @tms-atom))



(defn try!
  [form & v]
  (try
    (apply form v)
    (catch Exception e
      (log/error e)
      (f/fail {:msg "Error in server "}))))

;(clojure.string/join "/" (clojure.string/split "/tu/e" #"/") )



;ds-atom tms-atom
(defn warp-dadysql-handler
  "Warper that tries to do with dadysql. It should use next to the ring-handler. If path-in is matched with
   pull-path or push-path then it will API and return result.

   handler: Ring handler
   ds-atom: Clojure datasource as atom
   tms-atom: dadysql file as atom
   pull-path and push path string

  "
  [handler config-atom & {:keys [pull-path push-path log? encoding]
                :or   {pull-path "/pull"
                       push-path "/push"}}]
  (let []
    (fn [req]
      (let [m (into {} (mapv (fn [m] {(:url m) m}) @config-atom))
            request-path (or (:path-info req)
                             (:uri req))
            r (clojure.string/split request-path #"/")
            req-url (clojure.string/join "/" (butlast r))
            config (get m req-url)]
        (if (nil? config)
          (handler req)
          (let [ds (:ds config)
                tms (:tms config)]
            (condp = (str "/" (last r))
              pull-path
              (let [ds (or (:ds req) @ds)
                    tms (or (:tms req)
                            (try! reload-tms tms ds))
                    handler (-> (partial tj/pull ds tms)
                                (h/warp-pull)
                                (h/warp-default))]
                (handler req))
              push-path
              (let [ds (or (:ds req) @ds)
                    tms (or (:tms req)
                            (try! reload-tms tms ds))
                    handler (-> (partial tj/push! ds tms)
                                (h/warp-push)
                                (h/warp-default))]
                (handler req))
              (do
                (handler req)))))))))



(defn get-sql-file-value [tms-atom]
  (->> @tms-atom
       (vals)
       (mapv (fn [w] (select-keys w [:dadysql.core/name :dadysql.core/model :dadysql.core/sql])))))








