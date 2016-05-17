(ns tiesql.plugin.sql-executor
  (:require [clojure.set]
            [clojure.core.async :as async :refer [<! >! <!! chan alt! go go-loop onto-chan sliding-buffer]]
            [clojure.java.jdbc :as jdbc]
            [dady.common :as cc]
            [dady.fail :as f]
            [dady.node-proto :refer :all]
            [tiesql.common :refer :all]
            [clojure.tools.logging :as log]))


;@todo will be private
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


(defn notify-async-tracking
  [tm-coll]
  (do
    (go
      (let [t-fns (vals @tracking-fns)]
        (when (< 0 (count t-fns))
          (doseq [f t-fns]
            (f tm-coll)))))
    tm-coll))



(defn is-rollback?
  [commit-type read-only result-coll]
  (cond
    (= true read-only) true
    (= commit-type commit-none-key) true
    (and (= commit-type commit-all-key)
         (f/failed? result-coll)) true
    :else false))


(defn has-transaction?
  [ds]
  (and (jdbc/db-find-connection ds)
       (< 0 (jdbc/get-level ds))))


(defn commit-type
  "Return commit type if not found return commit-none-key  "
  [tm-coll]
  (let [p (comp
            (filter #(not= dml-select-key (dml-key %)))
            (map #(commit-key %))
            (map #(or % commit-all-key)))
        commits (into [] p tm-coll)]
    ;(println commits)
    (if (empty? commits)
      commit-none-key
      (or (some #{commit-none-key} commits)
          (some #{commit-all-key} commits)
          (cc/contain-all? commits commit-any-key)
          commit-none-key))))


(defn read-only?
  "Is it read only"
  ([tx-map]
   (if (contains? tx-map :read-only?)
     (:read-only? tx-map)
     true)))


(defn apply-handler-one
  [handler m]
  (-> (handler m)
      (async/<!!)))


(defn apply-handler-parallel
  [handler m-coll]
  (->> m-coll
       (map #(handler %))
       (doall)
       (async/merge)
       (async/take (count m-coll))
       (async/into [])
       (async/<!!)))


(defn jdbc-handler-single
  [ds tm]
  (let [dml-type (dml-key tm)
        sql (sql-key tm)
        result (result-key tm)]
    ;todo Need to move this log from here
    (condp = dml-type
      dml-select-key
      (if (contains? result result-array-key)
        (jdbc/query ds sql :as-arrays? true :identifiers clojure.string/lower-case)
        (jdbc/query ds sql :as-arrays? false :identifiers clojure.string/lower-case))
      dml-insert-key
      (jdbc/execute! ds sql :multi? true)
      (jdbc/execute! ds sql))))


(defn warp-map-output
  [handler]
  (fn [m]
    (try
      (let [st (System/nanoTime)
            stm (System/currentTimeMillis)
            r (handler m)
            total (/ (clojure.core/double
                       (- (System/nanoTime)
                          st)) 1000000.0)                   ;;in msecs
            r (if (seq? r) (into [] r) r)]
        (if (f/failed? r)
          r
          (assoc m output-key r
                   exec-time-total-key total
                   exec-time-start-key stm)))
      (catch Exception e
        (log/error e (sql-key m))
        (-> (f/fail {query-exception-key (.getMessage e)})
            (merge m))))))


(defn warp-async-go
  [handler]
  (fn [m]
    (async/go
      (let [t-v (or (timeout-key m) 2000)
            exec-ch (async/thread (handler m))
            [v rch] (async/alts! [exec-ch (async/timeout t-v)])]
        (if (= rch exec-ch)
          v
          ;; Need to assoc exception here as it returns from here
          (-> {query-exception-key "SQL Execution time out"
               timeout-key         t-v}
              (f/fail)
              (merge m)))))))



(defn jdbc-handler->chan [ds]
  (-> (partial jdbc-handler-single ds)
      (warp-map-output)
      (warp-async-go)))



(def Parallel :parallel)
(def Transaction :transaction)
(def Serial :serial)
(def SerialNotFailed :serial-not-failed)


(defn do-execute [type ds m-coll]
  (condp = type
    Parallel
    (-> (jdbc-handler->chan ds)
        (apply-handler-parallel m-coll))
    Serial
    (-> (map #(apply-handler-one (jdbc-handler->chan ds) %))
        (transduce conj m-coll))
    SerialNotFailed
    (-> (map #(apply-handler-one (jdbc-handler->chan ds) %))
        (f/comp-xf-until)
        (transduce conj m-coll))
    commit-all-key
    (do-execute SerialNotFailed ds m-coll)
    ;; default serial
    (do-execute Serial ds m-coll)))


(defn do-transaction-execute
  [ds tx-map m-coll]
  (let [isolation (or (:isolation tx-map) :serializable)
        read-only? (read-only? tx-map)
        commit-type (commit-type m-coll)]
    (if (has-transaction? ds)
      (do-execute commit-type ds m-coll)
      (jdbc/with-db-transaction
        [t-conn ds
         :isolation isolation
         :read-only? read-only?]
        (let [result-coll (do-execute commit-type t-conn m-coll)]
          (when (is-rollback? commit-type read-only? result-coll)
            (jdbc/db-set-rollback-only! t-conn))
          result-coll)))))


(defn db-execute
  [m-coll type ds tx]
  (if (= Transaction type)
    (-> (do-transaction-execute ds tx m-coll)
        (notify-async-tracking))
    (-> (do-execute type ds m-coll)
        (notify-async-tracking))))


(defn sql-executor-node
  [ds tms type]
  (let [tx (apply hash-map (get-in tms [global-key tx-prop]))
        f (fn [m-coll] (db-execute m-coll type ds tx))]
    (fn-as-node-processor f :name :sql-executor)))

