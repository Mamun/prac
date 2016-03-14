(ns tiesql.jdbc
  (:import [java.util.Date]
           [java.util.concurrent.TimeUnit])
  (:require
    [clojure.tools.logging :as log]
    [clojure.java.jdbc :as jdbc]
    [tiesql.common :as cc]
    ;[tiesql.common :refer :all]
    [tiesql.core :as tie]
    [tiesql.compiler.file-reader :as fr]
    [tiesql.plugin.base-impl :as imp]
    [tiesql.proto :as c]
    [tiesql.plugin.sql-executor :as ce]
    [tiesql.plugin.param-impl :as p]))


(defn read-file
  ([file-name] (read-file file-name (imp/new-root-node)))
  ([file-name pc]
   (-> (fr/read-file file-name pc)
       (assoc-in [cc/global-key cc/file-name-key] file-name)
       (assoc-in [cc/global-key cc/process-context-key]
                 (imp/select-module-node-processor pc)))))



(defn has-dml-type? [m-map]
  (let [dml (cc/dml-key m-map)]
    (or
      (= cc/dml-update-key dml)
      (= cc/dml-call-key dml)
      (= cc/dml-insert-key dml)
      (= cc/dml-delete-key dml)
      (= cc/dml-select-key dml))))


(defn get-dml
  [tms]
  (let [p (comp (filter has-dml-type?)
                (map cc/sql-key)
                (filter (fn [v] (if (< 1 (count v))
                                  true false)))
                (map first)
                (map #(clojure.string/replace % #":\w+" "?")))]
    (into [] p (vals tms))))


(defn- exec-node
  [ds tms type]
  (let [tx (apply hash-map (get-in tms [cc/global-key cc/tx-prop]))
        f (fn [m-coll] (ce/db-execute m-coll type ds tx))]
    (c/fn-as-node-processor f :name :sql-executor)))


(defn node-processor
  [tms]
  (if-let [w (get-in tms [cc/global-key cc/process-context-key])]
    w
    (cc/fail "There is no node processorprocess to do process ")))





(defn pull-default-request!
  [{:keys [name gname] :as request-m}]
  (let [dfmat (if (or gname
                      (sequential? name))
                {:pformat cc/map-format :rformat cc/nested-join-format}
                {:pformat cc/map-format :rformat cc/map-format})
        request-m (merge dfmat request-m)
        request-m (if gname
                    (assoc request-m :rformat cc/nested-join-format)
                    request-m)]
    request-m))


(defn pull-sequence-format!
  [request-m]
  (merge request-m {:pformat cc/map-format :rformat cc/value-format}))


(defn push-default-request!
  [{:keys [gname name] :as request-m}]
  (let [d (if (or gname
                  (sequential? name))
            {:pformat cc/nested-map-format :rformat cc/nested-map-format}
            {:pformat cc/map-format :rformat cc/map-format})
        request-m (merge d request-m)
        request-m (if gname
                    (-> request-m
                        (assoc :pformat cc/nested-map-format)
                        (assoc :rformat cc/nested-map-format))
                    request-m)]
    request-m))


(defn- filter-processor
  [process {:keys [out-format]}]
  (if (= out-format cc/value-format)
    (c/remove-type process :output)
    process))



(defn pull
  "Read or query value from database. It will return as model map "
  [ds tms & {:as request-m}]
  (if-let [w (cc/failed? (cc/validate-input! request-m))]
    w
    (let [request-m (pull-default-request! request-m)]
      (cc/try-> tms
                (node-processor)
                (filter-processor request-m)
                (c/add-child-one (exec-node ds tms ce/Parallel))
                (tie/do-run tms request-m)))))


(defn push!
  "Create, update or delete value in database. DB O/P will be run within transaction. "
  [ds tms & {:as request-m}]
  (if-let [w (cc/failed? (cc/validate-input! request-m))]
    w
    (let [request-m (push-default-request! request-m)]
      (cc/try-> tms
                (node-processor)
                (c/remove-type :output)
                (c/add-child-one (exec-node ds tms ce/Transaction))
                (p/assoc-param-ref-gen (fn [& {:as m}]
                                         (->> (pull-sequence-format! m)
                                              (seq)
                                              (apply concat)
                                              (cons tms)
                                              (cons ds)
                                              (apply pull))))
                (tie/do-run tms request-m)))))



(defn validate-dml!
  [ds str-coll]
  (jdbc/with-db-connection
    [conn ds]
    (doseq [str str-coll]
      (jdbc/prepare-statement (:connection conn) str)))
  (log/info (format "checking %d dml statement is done " (count str-coll))))



(defn warp-db-do [ds name-coll tms]
  (when name-coll
    (try
      (let [tm-coll (vals (tie/select-name tms name-coll))]
        (doseq [m tm-coll]
          (when-let [sql (get-in m [cc/sql-key])]
            (log/info "db do with " sql)
            (jdbc/db-do-commands ds false sql))))
      (catch Exception e
        (log/error e)
        (log/error (.getMessage e)))))
  tms)



(defn warp-validate-dml! [ds tms]
  (do
    (validate-dml! ds (get-dml tms))
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
  (let [v (mapv #(select-keys % [cc/sql-key cc/exec-time-total-key cc/exec-time-start-key]) tm-coll)
        w (mapv (fn [t]
                  (update-in t [cc/exec-time-start-key] (fn [o] (str (as-date o))))
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
