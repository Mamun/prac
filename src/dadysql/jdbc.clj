(ns dadysql.jdbc
  (:require
    [clojure.tools.logging :as log]
    [dadysql.selector :as dc]
    [dadysql.core :as tie]
    [dadysql.compiler.core :as cc]
    [dady.fail :as f]
    [dadysql.spec :as ds]
    [dadysql.sql-io-impl :as ce]
    [dadysql.file-reader :as fr]))



(defn read-file
  ([file-name] (read-file file-name nil #_(imp/new-root-node)))
  ([file-name pc]
   (-> (fr/read-file file-name)
       (cc/do-compile file-name)
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


(defn get-all-sql
  [tms]
  (let [p (comp (filter #(contains? ds/dml (:dadysql.core/dml %)))
                (map :dadysql.core/sql)
                (filter (fn [v] (if (< 1 (count v))
                                  true false)))
                (map first)
                (map #(clojure.string/replace % #":\w+" "?")))]
    (into [] p (vals tms))))


(defn select-name [tms req-m]
  (dc/select-name-by-name-coll tms (:dadysql.core/name req-m) ))

