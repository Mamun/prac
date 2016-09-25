(ns dadysql.plugin.sql.jdbc-io
  (:require [clojure.set]
            [clojure.core.async :as async :refer [<! >! <!! chan alt! go go-loop onto-chan sliding-buffer]]
            [clojure.java.jdbc :as jdbc]
            [dady.common :as cc]
            [dady.fail :as f]
            [dady.proto :refer :all]
            [dadysql.spec :refer :all]
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
    (= commit-type :dadysql.spec/none) true
    (and (= commit-type :dadysql.spec/all)
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
            (filter #(not= :dadysql.spec/dml-select (:dadysql.spec/dml-key %)))
            (map #(:dadysql.spec/commit %))
            (map #(or % :dadysql.spec/all)))
        commits (into [] p tm-coll)]
    ;(println commits)
    (if (empty? commits)
      :dadysql.spec/none
      (or (some #{:dadysql.spec/none} commits)
          (some #{:dadysql.spec/all} commits)
          (cc/contain-all? commits :dadysql.spec/any)
          :dadysql.spec/none))))


(defn read-only?
  "Is it read only"
  ([tx-map]
   (if (contains? tx-map :read-only?)
     (:read-only? tx-map)
     true)))



(defn jdbc-handler
  [ds tm]
  (let [dml-type (:dadysql.spec/dml-key tm)
        sql (:dadysql.spec/sql tm)
        result (:dadysql.spec/result tm)]
    (condp = dml-type
      :dadysql.spec/dml-select
      (if (contains? result :dadysql.spec/array)
        (jdbc/query ds sql :as-arrays? true :identifiers clojure.string/lower-case)
        (jdbc/query ds sql :as-arrays? false :identifiers clojure.string/lower-case))
      :dadysql.spec/dml-insert
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
                   :dadysql.spec/exec-total-time total
                   :dadysql.spec/exec-start-time stm)))
      (catch Exception e
        (log/error e (:dadysql.spec/sql m))
        (-> (f/fail {:dadysql.spec/query-exception (.getMessage e)})
            (merge m))))))


(defn warp-async-go
  [handler]
  (fn [m]
    (async/go
      (let [t-v (or (:dadysql.spec/timeout m) 2000)
            exec-ch (async/thread (handler m))
            [v rch] (async/alts! [exec-ch (async/timeout t-v)])]
        (if (= rch exec-ch)
          v
          ;; Need to assoc exception here as it returns from here
          (-> {:dadysql.spec/query-exception "SQL Execution time out"
               :dadysql.spec/timeout         t-v}
              (f/fail)
              (merge m)))))))



(defmulti execute (fn [_ _ & {:keys [type]}] type))


(defmethod execute
  :default
  [ds m-coll _]
  (let [handler (-> (partial jdbc-handler ds)
                    (warp-map-output))]
    (-> (map handler)
        (transduce conj m-coll))))


(defmethod execute
  :dadysql.plugin.sql.jdbc-io/parallel
  [ds m-coll _]
  (let [handler (-> (partial jdbc-handler ds)
                    (warp-map-output)
                    (warp-async-go))]
    (->> m-coll
         (map #(handler %))
         (doall)
         (async/merge)
         (async/take (count m-coll))
         (async/into [])
         (async/<!!))))


(defmethod execute
  :dadysql.plugin.sql.jdbc-io/serial-until-failed
  [ds m-coll _]
  (let [handler (-> (partial jdbc-handler ds)
                    (warp-map-output))]
    (-> (map (handler ds))
        (f/comp-xf-until)
        (transduce conj m-coll))))



(defn execute-type [commit-type]
  (if (= :dadysql.spec/all commit-type)
    :dadysql.plugin.sql.jdbc-io/serial-until-failed
    :dadysql.plugin.sql.jdbc-io/serial))


(defmethod execute
  :dadysql.plugin.sql.jdbc-io/transaction
  [ds m-coll & {:keys [tx-map]}]
  (let [isolation (or (:isolation tx-map) :serializable)
        read-only? (read-only? tx-map)
        commit-type (commit-type m-coll)
        exec-type (execute-type commit-type)]
    (if (has-transaction? ds)
      (execute ds m-coll :type exec-type)
      (jdbc/with-db-transaction
        [t-conn ds
         :isolation isolation
         :read-only? read-only?]
        (let [result-coll (execute t-conn m-coll :type exec-type)]
          (when (is-rollback? commit-type read-only? result-coll)
            (jdbc/db-set-rollback-only! t-conn))
          result-coll)))))


(defn sql-executor-node
  [ds tms type]
  (let [tx (apply hash-map (get-in tms [global-key :dadysql.spec/tx-prop]))
        f (fn [m-coll]
            (-> (execute ds m-coll :type type :tx-map tx)
                (notify-async-tracking)))]
    (fn-as-node-processor f :name :sql-executor)))

