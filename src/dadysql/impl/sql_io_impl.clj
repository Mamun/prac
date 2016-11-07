(ns dadysql.impl.sql-io-impl
  (:require [clojure.set]
            [clojure.core.async :as async :refer [<! >! <!! chan alt! go go-loop onto-chan sliding-buffer]]
            [dady.common :as cc]
            [dady.fail :as f]
            [dadysql.impl.sql-io-tracking :as dt]
            [clojure.tools.logging :as log]))


(defn warp-io-execption [handler]
  (fn [m]
    (try
      (handler m)
      (catch Exception e
        (log/error e (:dadysql.core/sql m))
        (f/fail {:dadysql.core/sql-io-exception (.getMessage e)
                 :for                           m})))))



(defn warp-map-output
  [handler]
  (fn [m]
    (let [st (System/nanoTime)
          stm (System/currentTimeMillis)
          r (handler m)
          total (/ (clojure.core/double
                     (- (System/nanoTime)
                        st)) 1000000.0)                     ;;in msecs
          r (if (seq? r) (into [] r) r)]
      (if (f/failed? r)
        r
        (assoc m :dadysql.core/output r
                 :dadysql.core/exec-total-time total
                 :dadysql.core/exec-start-time stm)))))


(defn warp-map-output-batch
  [batch-handler]
  (fn [tm-coll]
    (let [st (System/nanoTime)
          stm (System/currentTimeMillis)
          r (batch-handler tm-coll)
          total (/ (clojure.core/double
                     (- (System/nanoTime)
                        st)) 1000000.0)]
      (if (f/failed? r)
        r
        (mapv (fn [m r]
                (assoc m :dadysql.core/output r
                         :dadysql.core/exec-total-time total
                         :dadysql.core/exec-start-time stm))
              tm-coll
              r)))))


(defn warp-tracking [handler]
  (fn [m]
    (let [w (handler m)]
      (dt/notify-async-tracking w)
      w)))



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



(defmulti apply-sql-io (fn [_ _ type] type))


(defmethod apply-sql-io
  :default
  [h m-coll _]
  (let [handler (-> h
                    (warp-io-execption)
                    (warp-map-output)
                    (warp-tracking))]
    (-> (map handler)
        (transduce conj m-coll))))


(defmethod apply-sql-io
  :dadysql.impl.sql.jdbc-io/parallel
  [h m-coll _]
  (let [handler (-> h
                    (warp-io-execption)
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


(defmethod apply-sql-io
  :dadysql.impl.sql.jdbc-io/batch
  [h tm-coll _]
  (let [handler (-> h
                    (warp-io-execption)
                    (warp-map-output-batch)
                    (warp-tracking))]
    (handler tm-coll)))



(defn is-rollback?
  [commit-type read-only]
  (cond
    (= true read-only) true
    (= commit-type :dadysql.core/commit-none) true
    :else false))


(defn commit-type
  "Return commit type if not found return commit-none-key  "
  [tm-coll]
  (let [p (comp
            (filter #(not= :dadysql.core/dml-select (:dadysql.core/dml %)))
            (map #(:dadysql.core/commit %))
            (map #(or % :dadysql.core/commit-all)))
        commits (into [] p tm-coll)]
    ;(println commits)
    (if (empty? commits)
      :dadysql.core/commit-none
      (or (some #{:dadysql.core/commit-none} commits)
          (some #{:dadysql.core/commit-all} commits)
          (cc/contain-all? commits :dadysql.core/commit-any)
          :dadysql.core/commit-none))))


(defn read-only?
  "Is it read only"
  ([tx-map]
   (if (contains? tx-map :read-only?)
     (:read-only? tx-map)
     true)))


(defn global-info-m [tms m-coll]
  (let [tx-prop (apply hash-map (get-in tms [:_global_ :dadysql.core/tx-prop]))
        isolation (or (:isolation tx-prop) :serializable)
        read-only? (read-only? tx-prop)
        commit-type (is-rollback? (commit-type m-coll) read-only?)]
    {:isolation  isolation
     :read-only? read-only?
     :rollback?  commit-type}))











(comment

  (require '[dadysql.jdbc :as t])
  (require '[test-data :as td])


  (with-redefs [jdbc-handler (fn [_ _]
                               (list 1)
                               )]
    (let [meeting [{:dadysql.core/sql
                                             ["insert into meeting (meeting_id, subject) values (?, ?)"
                                              [109 "Hello Meeting for IT"]],
                    :dadysql.core/timeout    1000,
                    :dadysql.core/commit     :dadysql.core/commit-all,
                    :dadysql.core/dml        :dadysql.core/dml-insert,
                    :dadysql.core/join       [],
                    :dadysql.core/group      :create-meeting,
                    :dadysql.core/model      :meeting,
                    :dadysql.core/default-param [[:meeting_id :dadysql.core/param-ref-gen :gen-meet]],
                    :dadysql.core/index      0,
                    :dadysql.core/param
                                             {:subject "Hello Meeting for IT", :meeting_id 109},
                    :dadysql.core/name       :create-meeting}]]
      (->> (apply-sql-io (td/get-ds) @td/ds meeting :tms (t/read-file "tie.edn.sql")
                         :type :dadysql.impl.sql.jdbc-io/transaction)
           (clojure.pprint/pprint)))

    )



  #_(defn start-tracking
      [name callback]
      (ce/start-tracking name callback))


  #_(defn stop-tracking
      [name]
      (ce/stop-tracking name))

  )