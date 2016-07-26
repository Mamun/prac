(ns dadysql.jdbc
  (:import [java.util.Date]
           [java.util.concurrent.TimeUnit])
  (:require
    [clojure.tools.logging :as log]
    [clojure.java.jdbc :as jdbc]
    [dadysql.core :as tc]
    [dadysql.core2 :as tie]
    [dadysql.compiler.core :as fr]
    [dadysql.plugin.factory :as imp]
    [dady.proto :as c]
    [dady.fail :as f]
    [dadysql.plugin.sql.sql-executor :as ce]
    [dadysql.plugin.params.core :as p]))


(defn read-file
  ([file-name] (read-file file-name (imp/new-root-node)))
  ([file-name pc]
   (-> (fr/read-file file-name)
       (assoc-in [tc/global-key :dadysql.core/file-name] file-name)
       (assoc-in [tc/global-key tc/process-context-key] pc))))



(defmulti default-request (fn [t _] t))


(defmethod default-request :pull
  [_ {:keys [name gname] :as request-m}]
  (let [dfmat (if (or gname
                      (sequential? name))
                {:pformat tc/map-format :rformat tc/nested-join-format}
                {:pformat tc/map-format :rformat :one})
        request-m (merge dfmat request-m)
        request-m (if gname
                    (assoc request-m :rformat tc/nested-join-format)
                    request-m)]
    request-m))


(defmethod default-request :push
  [_ {:keys [gname name] :as request-m}]
  (let [d (if (or gname
                  (sequential? name))
            {:pformat tc/nested-map-format :rformat tc/nested-map-format}
            {:pformat tc/map-format :rformat :one})
        request-m (merge d request-m)
        request-m (if gname
                    (-> request-m
                        (assoc :pformat tc/nested-map-format)
                        (assoc :rformat tc/nested-map-format))
                    request-m)]
    request-m))


(defmethod default-request :db-seq
  [_ request-m]
  (-> request-m
      (assoc :pformat tc/map-format)
      (assoc :rformat tc/value-format)))


(defn- filter-processor
  [process {:keys [out-format]}]
  (if (= out-format tc/value-format)
    (c/remove-type process :output)
    process))


(defn select-pull-node [ds tms request-m]
  (f/try-> tms
           (get-in [tc/global-key tc/process-context-key] [])
           (filter-processor request-m)
           (c/add-child-one (ce/sql-executor-node ds tms ce/Parallel))))


(defn pull
  "Read or query value from database. It will return as model map
   ds: datasource
   "
  [ds tms request-m]
  (f/try->> request-m
            (tc/validate-input!)
            (default-request :pull)
            (tie/do-run (select-pull-node ds tms request-m) tms)))


#_(defn debug [v]
    (println "----")
    (clojure.pprint/pprint v)
    (println "---")
    v
    )

(defn select-push-node [ds tms]
  (f/try-> tms
           (get-in [tc/global-key tc/process-context-key] [])
           (c/remove-type :output)
           (c/add-child-one (ce/sql-executor-node ds tms ce/Transaction))
           (p/assoc-param-ref-gen (fn [& {:as m}]
                                    (->> (default-request :db-seq m)
                                         (pull ds tms))))))


(defn push!
  "Create, update or delete value in database. DB O/P will be run within transaction. "
  [ds tms request-m]
  (f/try->> request-m
            (tc/validate-input!)
            (default-request :push)
            (tie/do-run (select-push-node ds tms) tms)))



(defn db-do [ds name-coll tms]
  (when name-coll
    (try
      (let [tm-coll (vals (tie/select-name tms name-coll))]
        (doseq [m tm-coll]
          (when-let [sql (get-in m [:dadysql.core/sql])]
            (log/info "db do with " sql)
            (jdbc/db-do-commands ds sql))))
      (catch Exception e
        (do
          (log/error e)
          (f/fail {:detail e}))
        ;(log/error e)
        ;(log/error (.getMessage e))
        )))
  tms)



(defn has-dml-type? [m-map]
  (let [dml (:dadysql.core/dml-key m-map)]
    (or
      (= :dadysql.core/dml-update dml)
      (= :dadysql.core/dml-call dml)
      (= :dadysql.core/dml-insert dml)
      (= :dadysql.core/dml-delete dml)
      (= :dadysql.core/dml-select dml))))


(defn get-dml
  [tms]
  (let [p (comp (filter has-dml-type?)
                (map :dadysql.core/sql)
                (filter (fn [v] (if (< 1 (count v))
                                  true false)))
                (map first)
                (map #(clojure.string/replace % #":\w+" "?")))]
    (into [] p (vals tms))))


(defn validate-dml! [ds tms]
  (let [str-coll (get-dml tms)]
    (jdbc/with-db-connection
      [conn ds]
      (doseq [str str-coll]
        (jdbc/prepare-statement (:connection conn) str)))
    (log/info (format "checking %d dml statement is done " (count str-coll)))

    ;(validate-dml! ds (get-dml tms))
    tms))


(defn start-tracking
  [name callback]
  (ce/start-tracking name callback))


(defn stop-tracking
  [name]
  (ce/stop-tracking name))


(defn- as-date [milliseconds]
  (if milliseconds
    (java.util.Date. milliseconds)))


(defn- execution-log
  [tm-coll]
  (let [v (mapv #(select-keys % [:dadysql.core/sql :dadysql.core/exec-total-time :dadysql.core/exec-start-time]) tm-coll)
        w (mapv (fn [t]
                  (update-in t [:dadysql.core/exec-start-time] (fn [o] (str (as-date o))))
                  ) v)]
    (log/info w)))


(defn start-sql-execution-log
  "Start sql execution log with sql statement, total duration and time"
  []
  (start-tracking :_sql-execution_ execution-log))


(defn stop-sql-execution-log
  "Stop sql execution log "
  []
  (stop-tracking :_sql-execution_))
