(ns dadysql.jdbc
  (:require
    [clojure.tools.logging :as log]
    [clojure.java.jdbc :as jdbc]
    [dadysql.selector :as dc]
    [dadysql.core :as tie]
    [dadysql.compiler.core :as fr]
    [dady.fail :as f]
    [dadysql.spec :as ds]
    [dadysql.plugin.sql-io-impl :as ce]))



(defn read-file
  ([file-name] (read-file file-name nil #_(imp/new-root-node)))
  ([file-name pc]
   (-> (fr/read-file file-name)
       (assoc-in [:_global_ :dadysql.core/file-name] file-name))))



(defn pull
  "Read or query value from database. It will return as model map
   ds: datasource
   "
  [ds tms req-m]
  (if-let [r (f/failed? (tie/validate-input! req-m))]
    r
    (let [op-m {:dadysql.core/op :dadysql.core/op-pull}
          sql-exec (ce/warp-sql-execute ds tms :dadysql.plugin.sql.jdbc-io/parallel)
          gen (fn [_] 1)]
      (-> op-m
          (merge req-m)
          (assoc :dadysql.core/pull gen)
          (assoc :dadysql.core/sql-exec sql-exec)
          (tie/do-execute tms)))))




(defn push!
  "Create, update or delete value in database. DB O/P will be run within transaction. "
  [ds tms req-m]
  (if-let [r (f/failed? (tie/validate-input! req-m))]
    r
    (let [sql-exec (ce/warp-sql-execute ds tms :dadysql.plugin.sql.jdbc-io/transaction)
          gen (fn [m]
                (->> (assoc m :dadysql.core/op :dadysql.core/op-db-seq)
                     (pull ds tms)))]
      (-> req-m
          (assoc :dadysql.core/op :dadysql.core/op-push!)
          (assoc :dadysql.core/pull gen)
          (assoc :dadysql.core/sql-exec sql-exec)
          (tie/do-execute tms)))))



(defn db-do [ds name-coll tms]
  (when name-coll
    (try
      (let [tm-coll (vals (dc/select-name-by-name-coll tms name-coll))]
        (doseq [m tm-coll]
          (when-let [sql (get-in m [:dadysql.core/sql])]
            (log/info "db do with " sql)
            (jdbc/db-do-commands ds sql))))
      (catch Exception e
        (do
          (log/error e)
          (f/fail {:detail e})))))
  tms)



(defn get-dml
  [tms]
  (let [p (comp (filter #(contains? ds/dml (:dadysql.core/dml %) ) )
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
    tms))


