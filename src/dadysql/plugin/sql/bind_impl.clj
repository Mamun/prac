(ns dadysql.plugin.sql.bind-impl

  (:require [dady.fail :as f]
            [dady.common :as cc]))


(defn validate-input-not-empty!
  [m]
  (if (and (not-empty (rest (:dadysql.core/sql m)))
           (empty? (:dadysql.core/input m)))
    (f/fail (format "Input is missing for %s " (:dadysql.core/name m)))
    m))


(defn validate-input-type!
  [m]
  (let [dml-type (:dadysql.core/dml-key m)
        input (:dadysql.core/input m)
        sql (:dadysql.core/sql m)]
    (if (and (not= dml-type :dadysql.core/dml-insert)
             (not-empty (rest sql))
             (not (map? input)))
      (f/fail (format "Input Params for %s will be map format but %s is not map format " sql input))
      m)))

(comment

  ;(contains? #{1 2 3}  5)

  (validate-input-type!
    {:dadysql.core/sql     ["insert into employee_detail (employee_id, street,   city,  state,  country )
                           values (:employee_id, :street, :city, :state, :country)"
                            :employee_id
                            :street
                            :city
                            :state
                            :country],
     :dadysql.core/dml-key :dadysql.core/dml-insert,
     :input                [{:street      "Schwan",
                             :city        "Munich",
                             :state       "Bayern",
                             :country     "Germany",
                             :id          126,
                             :employee_id 125}],
     })

  )





(defn- validate-required-params*!
  [p-set input]
  (let [p-set (into #{} p-set)
        i-set (into #{} (keys input))
        diff-keys (clojure.set/difference p-set i-set)]
    (if-not (empty? diff-keys)
      (f/fail (format "Missing required parameter %s" (pr-str diff-keys)))
      input)))


(defn validate-required-params!
  [m]
  (let [input (cc/as-sequential (:dadysql.core/input m))
        r (-> (f/comp-xf-until (map #(validate-required-params*! (rest (:dadysql.core/sql m)) %)))
              (transduce conj input))]
    (if (f/failed? r)
      r
      m)))


(defn get-place-holder
  [type v]
  (if (and (sequential? v)
           (= #'clojure.core/vector? type))
    (clojure.string/join ", " (repeat (count v) "?"))
    "?"))


(defn update-sql-str
  [v sql-str k]
  (clojure.string/replace-first sql-str (re-pattern (str k)) v))


(defn default-proc
  [tm]
  (let [[sql-str & sql-params] (:dadysql.core/sql tm)
        input (:dadysql.core/input tm)
        ;todo Need to find type using sql str
        validation nil                                      ; (validation-key tm)
        rf (fn [sql-coll p-key]
             (let [p-value (cc/as-sequential (p-key input))
                   w (-> nil
                         (get-place-holder p-value)
                         (update-sql-str (first sql-coll) p-key))
                   q-str (assoc sql-coll 0 w)]
               (reduce conj q-str p-value)))]
    (->> (reduce rf [sql-str] sql-params)
         (assoc tm :dadysql.core/sql))))


(defn insert-proc
  [tm]
  (let [sql (:dadysql.core/sql tm)
        sql-str (reduce (partial update-sql-str "?") sql)]
    (->> (:dadysql.core/input tm)
         (cc/as-sequential)
         (mapv #(cc/select-values %1 (rest sql)))
         (reduce conj [sql-str])
         (assoc tm :dadysql.core/sql))))


(defmulti sql-bind (fn [tm] (:dadysql.core/dml-key tm)))


(defmethod sql-bind
  :default
  [tm]
  (f/try-> tm
           validate-input-not-empty!
           validate-input-type!
           validate-required-params!
           default-proc))


(defmethod sql-bind
  :dadysql.core/dml-insert
  [tm]
  (f/try-> tm
           validate-input-not-empty!
           validate-input-type!
           validate-required-params!
           insert-proc))



#_(defn sql-bind-impl [tm-coll]
  (transduce (map sql-bind) conj tm-coll))






;(defbranch SqlKey [cname ccoll corder])
;(defleaf InsertSqlKey [cname corder])
;(defleaf UpdateSqlKey [cname corder])
;(defleaf DeleteSqlKey [cname corder])
;(defleaf SelectSqlKey [cname corder])
;(defleaf CallSqlKey [cname corder])


#_(defn new-sql-key [order coll]
    (SqlKey. :dadysql.core/sql coll order))


#_(defn new-childs-key []
    (vector
      (InsertSqlKey. :dadysql.core/dml-insert 0)
      (UpdateSqlKey. :dadysql.core/dml-update 1)
      (DeleteSqlKey. :dadysql.core/dml-delete 2)
      (SelectSqlKey. :dadysql.core/dml-select 3)
      (CallSqlKey. :dadysql.core/dml-call 4)))


#_(defn debug [v]
    (println "---")
    (clojure.pprint/pprint v)
    (println "---")
    v
    )

#_(defn batch-process [childs m]

    ;(clojure.pprint/pprint m)

    (let [p (-> (group-by-node-name childs)
                ;           (debug)
                (get (:dadysql.core/dml-key m)))]
      (-process p m)))


#_(extend-protocol INodeProcessor
    SqlKey
    (-lorder [this] (:corder this))
    (-process-type [_] :input)
    (-process [this m]
      (batch-process (:ccoll this) m))
    InsertSqlKey
    (-lorder [this] (:corder this))
    (-process-type [_] :input)
    (-process? [_ m] (= :dadysql.core/dml-insert (:dadysql.core/dml-key m)))
    (-process [_ m]
      (do-insert-proc m))
    UpdateSqlKey
    (-lorder [this] (:corder this))
    (-process-type [_] :input)
    (-process? [_ m] (= :dadysql.core/dml-update (:dadysql.core/dml-key m)))
    (-process [_ m]
      (do-default-proc m))
    SelectSqlKey
    (-lorder [this] (:corder this))
    (-process-type [_] :input)
    (-process? [_ m] (do
                       (= :dadysql.core/dml-select (:dadysql.core/dml-key m))))
    (-process [_ m]
      (do-default-proc m))
    DeleteSqlKey
    (-lorder [this] (:corder this))
    (-process-type [_] :input)
    (-process? [_ m] (= :dadysql.core/dml-delete (:dadysql.core/dml-key m)))
    (-process [_ m]
      (do-default-proc m))
    CallSqlKey
    (-lorder [this] (:corder this))
    (-process-type [_] :input)
    (-process? [_ m] (= :dadysql.core/dml-call (:dadysql.core/dml-key m)))
    (-process [_ m]
      (do-default-proc m)))




