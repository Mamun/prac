(ns dadysql.plugin.sql-io-impl
  (:require [clojure.set]
            [clojure.core.async :as async :refer [<! >! <!! chan alt! go go-loop onto-chan sliding-buffer]]
            [clojure.java.jdbc :as jdbc]
            [dady.common :as cc]
            [dady.fail :as f]
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


(defn warp-tracking [handler]
  (fn [m]
    (do
      (notify-async-tracking
        (handler m)))))



(defn is-rollback?
  [commit-type read-only result-coll]
  (cond
    (= true read-only) true
    (= commit-type :dadysql.core/none) true
    (and (= commit-type :dadysql.core/all)
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
            (filter #(not= :dadysql.core/dml-select (:dadysql.core/dml-key %)))
            (map #(:dadysql.core/commit %))
            (map #(or % :dadysql.core/all)))
        commits (into [] p tm-coll)]
    ;(println commits)
    (if (empty? commits)
      :dadysql.core/none
      (or (some #{:dadysql.core/none} commits)
          (some #{:dadysql.core/all} commits)
          (cc/contain-all? commits :dadysql.core/any)
          :dadysql.core/none))))


(defn read-only?
  "Is it read only"
  ([tx-map]
   (if (contains? tx-map :read-only?)
     (:read-only? tx-map)
     true)))



(defn jdbc-handler
  [ds tm]
  (let [dml-type (:dadysql.core/dml-key tm)
        sql (:dadysql.core/sql tm)
        result (:dadysql.core/result tm)]
    (condp = dml-type
      :dadysql.core/dml-select
      (if (contains? result :dadysql.core/array)
        (jdbc/query ds sql :as-arrays? true :identifiers clojure.string/lower-case)
        (jdbc/query ds sql :as-arrays? false :identifiers clojure.string/lower-case))
      :dadysql.core/dml-insert
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
          (assoc m :dadysql.core/output r
                   :dadysql.core/exec-total-time total
                   :dadysql.core/exec-start-time stm)))
      (catch Exception e
        (log/error e (:dadysql.core/sql m))
        (-> (f/fail {:dadysql.core/query-exception (.getMessage e)})
            (merge m))))))


(defn warp-async-go
  [handler]
  (fn [m]
    (async/go
      (let [t-v (or (:dadysql.core/timeout m) 2000)
            exec-ch (async/thread (handler m))
            [v rch] (async/alts! [exec-ch (async/timeout t-v)])]
        (if (= rch exec-ch)
          v
          ;; Need to assoc exception here as it returns from here
          (-> {:dadysql.core/query-exception "SQL Execution time out"
               :dadysql.core/timeout         t-v}
              (f/fail)
              (merge m)))))))



(defmulti sql-execute (fn [_ _ & {:keys [type]}] type))


(defmethod sql-execute
  :default
  [ds m-coll & _]
  (let [handler (-> (partial jdbc-handler ds)
                    (warp-map-output)
                    (warp-tracking)
                    )]
    (-> (map handler)
        (transduce conj m-coll))))


(defmethod sql-execute
  :dadysql.plugin.sql.jdbc-io/parallel
  [ds m-coll & _]
  (let [handler (-> (partial jdbc-handler ds)
                    (warp-map-output)
                    (warp-tracking)
                    (warp-async-go))]
    (->> m-coll
         (map #(handler %))
         (doall)
         (async/merge)
         (async/take (count m-coll))
         (async/into [])
         (async/<!!))))


(defmethod sql-execute
  :dadysql.plugin.sql.jdbc-io/serial-until-failed
  [ds m-coll & _]
  (let [handler (-> (partial jdbc-handler ds)
                    (warp-map-output)
                    (warp-tracking))]
    (-> (map handler)
        (f/comp-xf-until)
        (transduce conj m-coll))))



(defn execute-type [commit-type]
  (if (= :dadysql.core/all commit-type)
    :dadysql.plugin.sql.jdbc-io/serial-until-failed
    :dadysql.plugin.sql.jdbc-io/serial))


(defmethod sql-execute
  :dadysql.plugin.sql.jdbc-io/transaction
  [ds m-coll & {:keys [tms]}]
  (let [tx-prop (apply hash-map (get-in tms [:_global_ :dadysql.core/tx-prop]))
        isolation (or (:isolation tx-prop) :serializable)
        read-only? (read-only? tx-prop)
        commit-type (commit-type m-coll)
        exec-type (execute-type commit-type)]
    (if (has-transaction? ds)
      (sql-execute ds m-coll :type exec-type)
      (jdbc/with-db-transaction
        [t-conn ds
         :isolation isolation
         :read-only? read-only?]
        (let [result-coll (sql-execute t-conn m-coll :type exec-type)]
          (when (is-rollback? commit-type read-only? result-coll)
            (jdbc/db-set-rollback-only! t-conn))
          result-coll)))))



(defn warp-sql-execute [ds tms type]
  (fn [tm-coll]
    (sql-execute ds tm-coll :type type :tms tms)))





#_(defn sql-executor-node
    [ds tms type]
    (let [f (fn [m-coll]
              (sql-execute ds m-coll :type type :tms tms))]
      (fn-as-node-processor f :dadysql.core/name :sql-executor)))


(comment

  (require '[dadysql.jdbc :as t])
  (require '[test-data :as td])


  (with-redefs [jdbc-handler (fn [_ _]
                               (list 1)
                               )]
    (let [meeting [{:dadysql.core/sql
                                          ["insert into meeting (meeting_id, subject) values (?, ?)"
                                           [109 "Hello Meeting for IT"]],
                    :dadysql.core/timeout 1000,
                    :dadysql.core/commit  :dadysql.core/all,
                    :dadysql.core/dml-key :dadysql.core/dml-insert,
                    :dadysql.core/join    [],
                    :dadysql.core/group   :create-meeting,
                    :dadysql.core/model   :meeting,
                    :dadysql.core/param   [[:meeting_id :dadysql.core/ref-gen :gen-meet]],
                    :dadysql.core/index   0,
                    :dadysql.core/input
                                          {:subject "Hello Meeting for IT", :meeting_id 109},
                    :dadysql.core/name    :create-meeting}]]
      (->> (sql-execute (td/get-ds) @td/ds meeting :tms (t/read-file "tie.edn.sql")
                        :type :dadysql.plugin.sql.jdbc-io/transaction)
           (clojure.pprint/pprint)))

    )

  )