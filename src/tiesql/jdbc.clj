(ns tiesql.jdbc
  (:import [java.util.Date]
           [java.util.concurrent.TimeUnit])
  (:require
    [clojure.tools.logging :as log]
    [clojure.java.jdbc :as jdbc]
    [dady.common :as cc]
    [tiesql.common :as tc]
    [tiesql.core :as tie]
    [tiesql.compiler.file-reader :as fr]
    [tiesql.plugin.factory :as imp]
    [dady.node-proto :as c]
    [dady.fail :as f]
    [tiesql.plugin.sql-executor :as ce]
    [tiesql.plugin.param-impl :as p]))


(defn read-file
  ([file-name] (read-file file-name (imp/new-root-node)))
  ([file-name pc]
   (-> (fr/read-file file-name pc)
       (assoc-in [tc/global-key tc/file-name-key] file-name)
       (assoc-in [tc/global-key tc/process-context-key]
                 (imp/select-module-node-processor pc)))))


(defmulti default-request (fn [t _] t))


(defmethod default-request :pull
  [_ {:keys [name gname] :as request-m}]
  (let [dfmat (if (or gname
                      (sequential? name))
                {:pformat tc/map-format :rformat tc/nested-join-format}
                {:pformat tc/map-format :rformat :one })
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


(defn select-push-node [ds tms]
  (f/try-> tms
            (get-in [tc/global-key tc/process-context-key] [])
            (c/remove-type :output)
            (c/add-child-one (ce/sql-executor-node ds tms ce/Transaction))
            (p/assoc-param-ref-gen (fn [& {:as m}]
                                     (->> (default-request :db-seq m)
                                          ;(seq)
                                          ;(apply concat)
                                          ;(cons tms)
                                          ;(cons ds)
                                          (pull ds tms))))))


(defn push!
  "Create, update or delete value in database. DB O/P will be run within transaction. "
  [ds tms request-m ]
  (f/try->> request-m
             (tc/validate-input!)
             (default-request :push)
             (tie/do-run (select-push-node ds tms) tms)))



(defn db-do [ds name-coll tms]
  (when name-coll
    (try
      (let [tm-coll (vals (tie/select-name tms name-coll))]
        (doseq [m tm-coll]
          (when-let [sql (get-in m [tc/sql-key])]
                   (log/info "db do with " sql)
            (jdbc/db-do-commands ds  sql))))
      (catch Exception e
        (do
          (log/error e)
          (f/fail {:detail e}))
        ;(log/error e)
        ;(log/error (.getMessage e))
        )))
  tms)



(defn has-dml-type? [m-map]
  (let [dml (tc/dml-key m-map)]
    (or
      (= tc/dml-update-key dml)
      (= tc/dml-call-key dml)
      (= tc/dml-insert-key dml)
      (= tc/dml-delete-key dml)
      (= tc/dml-select-key dml))))


(defn get-dml
  [tms]
  (let [p (comp (filter has-dml-type?)
                (map tc/sql-key)
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
  (let [v (mapv #(select-keys % [tc/sql-key tc/exec-time-total-key tc/exec-time-start-key]) tm-coll)
        w (mapv (fn [t]
                  (update-in t [tc/exec-time-start-key] (fn [o] (str (as-date o))))
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
