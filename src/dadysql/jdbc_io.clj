(ns dadysql.jdbc-io
  (:require [clojure.java.jdbc :as jdbc]
            [dady.fail :as f]
            [clojure.tools.logging :as log]))


(defn jdbc-handler
  [ds tm _]
  ;(println "jdbc hanbdler ")
  ;(clojure.pprint/pprint tm )
  (let [dml-type (:dadysql.core/dml tm)
        sql (:dadysql.core/sql tm)
        result (:dadysql.core/result tm)]
    (condp = dml-type
      :dadysql.core/dml-select
      (if (contains? result :dadysql.core/result-array)
        (jdbc/query ds sql :as-arrays? true :identifiers clojure.string/lower-case)
        (jdbc/query ds sql :as-arrays? false :identifiers clojure.string/lower-case))
      :dadysql.core/dml-insert
      (jdbc/execute! ds sql :multi? true)
      (jdbc/execute! ds sql))))



(defn jdbc-handler-batch
  [ds tm-coll m]
  (let [{:keys [isolation read-only? rollback?]} m]
    (jdbc/with-db-transaction
      [t-conn ds
       :isolation isolation
       :read-only? read-only?]
      (let [result (mapv #(jdbc-handler t-conn % m) tm-coll)]
        (when rollback?
          (jdbc/db-set-rollback-only! t-conn))
        result))))


(defn validate-dml! [ds sql-str-coll]
  (jdbc/with-db-connection
    [conn ds]
    (doseq [str sql-str-coll]
      (jdbc/prepare-statement (:connection conn) str)))
  (log/info "Validation done for " sql-str-coll))


(defn db-do [ds sql-str-coll]
  (try
    (doseq [m sql-str-coll]
      (when-let [sql (get-in m [:dadysql.core/sql])]
        (log/info "db do with " sql)
        (jdbc/db-do-commands ds sql)))
    (catch Exception e
      (do
        (log/error e)
        (f/fail {:detail e})))))
