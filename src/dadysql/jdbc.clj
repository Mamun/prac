(ns dadysql.jdbc
  (:require
    [spec-model.core :as sm]
    [dadysql.workflow-exec :as tie]
    [dadysql.compiler.core :as cc]
    [dadysql.clj.fail :as f]
    [dadysql.spec :as ds]
    [dadysql.impl.sql-io-impl :as ce]
    [dadysql.impl.param-spec-impl :as ps]
    [dadysql.selector :as sel]
    [dadysql.jdbc-io :as io]
    [dadysql.file-reader :as fr]
    [clojure.tools.logging :as log]))



(defn read-file
  ([file-name] (read-file file-name nil #_(imp/new-root-node)))
  ([file-name pc]
   (-> (fr/read-file file-name)
       (cc/do-compile file-name)

       ;;file name is needed to gen spec
       )))


(defn select-name [tms req-m]
  (let [name (:dadysql.core/name req-m)
        group (:dadysql.core/group req-m)
        name (if group
               (sel/select-name-for-groups tms group name)
               name)]
    (sel/select-name-by-name-coll tms name)))


(defn get-sql-statement
  [tms]
  (let [p (comp (filter #(contains? ds/dml (:dadysql.core/dml %)))
                (map :dadysql.core/sql)
                (filter (fn [v] (if (< 1 (count v))
                                  true false)))
                (map first)
                (map #(clojure.string/replace % #":\w+" "?")))]
    (into [] p (vals tms))))



(defn select-spec [tms req-m]
  (->> (select-name tms req-m)
       (map :dadysql.core/spec)
       (ps/as-merge-spec)))


(defn- warp-sql-io [ds tms type]
  (fn [tm-coll]
    (let [m (ce/global-info-m tms tm-coll)
          sql-io (fn [v]
                   (if (= type :dadysql.impl.sql.jdbc-io/batch)
                     (io/jdbc-handler-batch ds v m)
                     (io/jdbc-handler ds v m)))]
      (ce/apply-sql-io sql-io tm-coll type))))



(defn pull
  "Read or query value from database. It will return as model map
   ds: datasource
   "
  [ds tms req-m]
  ;(println "pull" req-m)
  (if-let [r (f/failed? (tie/validate-input! req-m))]
    r
    (let [op-m {:dadysql.core/op :dadysql.core/op-pull}
          sql-exec (warp-sql-io ds tms :dadysql.impl.sql.jdbc-io/parallel)]
      (-> op-m
          (merge req-m)
          (assoc :dadysql.core/pull identity)
          (assoc :dadysql.core/sql-exec sql-exec)
          (tie/do-execute (select-name tms req-m))))))



(defn push!
  "Create, update or delete value in database. DB O/P will be run within transaction. "
  [ds tms req-m]
  (if-let [r (f/failed? (tie/validate-input! req-m))]
    r
    (let [sql-exec (warp-sql-io ds tms :dadysql.impl.sql.jdbc-io/batch)]
      (-> req-m
          (assoc :dadysql.core/op :dadysql.core/op-push!)
          (assoc :dadysql.core/pull (partial pull ds tms))
          (assoc :dadysql.core/sql-exec sql-exec)
          (tie/do-execute (select-name tms req-m))))))


(defn default-param [ds tms req-m]
  (if-let [r (f/failed? (tie/validate-input! req-m))]
    r
    (let [sql-exec (warp-sql-io ds tms :dadysql.impl.sql.jdbc-io/parallel)]
      (-> req-m
          (assoc :dadysql.core/pull (partial pull ds tms))
          (assoc :dadysql.core/sql-exec sql-exec)
          (tie/process-input (select-name tms req-m) :disjoin false)))))


(defn get-spec
  [tms ]
  (ps/gen-spec tms))


(defn get-spec-str
  ([tms ns-name]
   (let [ns-identifier (ps/filename-as-keyword
                         (or (get-in tms [:_global_ :dadysql.core/file-name]) "nofound.clj"))]
     (->> (ps/gen-spec tms)
          (sm/as-file-str (or ns-name ns-identifier ) ))))
  ([tms]
   (get-spec-str tms nil)))




