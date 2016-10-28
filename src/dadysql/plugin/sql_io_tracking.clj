(ns dadysql.plugin.sql-io-tracking
  (:import [java.util.Date]
           [java.util.concurrent.TimeUnit])
  (:require [clojure.set]
            [clojure.core.async :refer [<! >! <!! chan alt! go go-loop onto-chan sliding-buffer]]
            [clojure.tools.logging :as log]))


(defonce tracking-fns (atom {}))


(defn start-tracking
  [name callback]
  {:pre [(keyword? name)
         (fn? callback)]}
  (swap! tracking-fns assoc-in [name] callback)
  nil)


(defn stop-tracking
  [name]
  {:pre [(keyword? name)]}
  (swap! tracking-fns #(dissoc % name))
  nil)


(defn stop-all-tracking
  []
  (reset! tracking-fns {})
  nil)


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


(defn notify-async-tracking
  [tm-coll]
  (do
    (go
      (let [t-fns (vals @tracking-fns)]
        (when (< 0 (count t-fns))
          (doseq [f t-fns]
            (f tm-coll)))))
    tm-coll))


