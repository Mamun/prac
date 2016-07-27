(ns dadysql.core-node
  (:require
    [dadysql.spec :as tc]
    [dadysql.core :as dc]
    [dady.proto :as c]
    [dady.fail :as f]
    [dadysql.plugin.sql.sql-executor :as ce]
    [dadysql.plugin.params.core :as p]))


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



(defn select-push-node [pull ds tms]
  (f/try-> tms
           (get-in [tc/global-key tc/process-context-key] [])
           (c/remove-type :output)
           (c/add-child-one (ce/sql-executor-node ds tms ce/Transaction))
           (p/assoc-param-ref-gen (fn [& {:as m}]
                                    (->> (dc/default-request :db-seq m)
                                         (pull ds tms))))))
