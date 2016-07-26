(ns dadysql.plugin.sql.bind-impl
  (:use [dady.proto])
  (:require [dadysql.core :refer :all]
            [dady.fail :as f]
            [dady.common :as cc]
    #_[schema.core :as s]))


(defn validate-input-not-empty!
  [m]
  (if (and (not-empty (rest (:dadysql.core/sql m)))
           (empty? (input-key m)))
    (f/fail (format "Input is missing for %s " (:dadysql.core/name m)))
    m))


(defn validate-input-type!
  [m]
  (let [dml-type (:dadysql.core/dml-key m)
        input (input-key m)
        sql (:dadysql.core/sql m)]
    (if (and (not= dml-type :dadysql.core/dml-insert)
             (not-empty (rest sql))
             (not (map? input)))
      (f/fail (format "Input Params for %s will be map format but %s is not map format " sql input))
      m)))

(comment

  ;(contains? #{1 2 3}  5)

  (validate-input-type!
    {
     :dadysql.core/sql
                           ["insert into employee_detail (employee_id, street,   city,  state,  country )                     values (:employee_id, :street, :city, :state, :country)"
                            :employee_id
                            :street
                            :city
                            :state
                            :country],

     :dadysql.core/dml-key :dadysql.core/dml-insert,

     :input
                           [{:street      "Schwan",
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
  (let [input (cc/as-sequential (input-key m))
        r (-> (f/comp-xf-until (map #(validate-required-params*! (rest (:dadysql.core/sql m)) %)))
              (transduce conj input))]
    (if (f/failed? r)
      r
      m)))


#_(defn get-vali-type
    [coll id]
    (->> coll
         (filter #(and
                   (= id (first %))
                   (= validation-type-key (second %))))
         (map #(nth % 2))
         (first)))



(defn get-place-holder
  [type v]
  ;(println "----" type)
  ;(println "----" v)
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
        input (input-key tm)
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
    (->> (input-key tm)
         (cc/as-sequential)
         (mapv #(cc/select-values %1 (rest sql)))
         (reduce conj [sql-str])
         (assoc tm :dadysql.core/sql))))



(defn do-default-proc
  [tm]
  (f/try-> tm
           validate-input-not-empty!
           validate-input-type!
           validate-required-params!
           default-proc))


(defn do-insert-proc
  [tm]
  (f/try-> tm
           validate-input-not-empty!
           validate-input-type!
           validate-required-params!
           insert-proc))



#_(defn dml-type
    [v]
    (println v)
    (let [w (-> v
                (first)
                (clojure.string/trim)
                (clojure.string/lower-case)
                (clojure.string/split #"\s+")
                (first)
                (keyword))]

      (condp = w
        :select :dadysql.core/dml-select
        :update :dadysql.core/dml-update
        :insert :dadysql.core/dml-insert
        :delete :dadysql.core/dml-delete
        :call :dadysql.core/dml-call
        (throw (ex-info "Undefined dml op" {:for v})))
      ))

;insert
;(dml-type ["select * from p where "])

;(def sql-param-regex #"\w*:[\w|\-|#]+")


#_(defn sql-str-emit
    [sql-str]
    (->> (re-seq sql-param-regex sql-str)
         (transduce (comp (map read-string)) conj)
         (reduce (fn [acc v]
                   (let [w (cc/as-lower-case-keyword v)
                         sql-str-w (-> (first acc)
                                       (clojure.string/replace-first (re-pattern (cc/as-string v)) (cc/as-string w)))]
                     (-> (assoc-in acc [0] sql-str-w)
                         (conj w)))
                   ) [sql-str])))


#_(defn sql-emit
    [sql-str]
    (let [p (comp (filter not-empty)
                  (map sql-str-emit)
                  (map (fn [v] {:dadysql.core/sql     v
                                :dadysql.core/dml-key (dml-type v)})))
          sql (clojure.string/split (clojure.string/trim sql-str) #";")]
      (->> (transduce p conj [] sql)
           (mapv (fn [i m]
                   (assoc m :dadysql.core/index i)
                   ) (range)))))


(defbranch SqlKey [cname ccoll corder])
(defleaf InsertSqlKey [cname corder])
(defleaf UpdateSqlKey [cname corder])
(defleaf DeleteSqlKey [cname corder])
(defleaf SelectSqlKey [cname corder])
(defleaf CallSqlKey [cname corder])


(defn new-sql-key [order coll]
  (SqlKey. :dadysql.core/sql coll order))


(defn new-childs-key []
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

(defn batch-process [childs m]

  ;(clojure.pprint/pprint m)

  (let [p (-> (group-by-node-name childs)
              ;           (debug)
              (get (:dadysql.core/dml-key m)))]
    (-process p m)))


(extend-protocol INodeProcessor
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




#_(defn- not-blank? [^String v]
    (not (clojure.string/blank? v)))


#_(extend-protocol INodeCompiler
    SqlKey
    (-spec [this]
      `{(schema.core/required-key ~(:cname this))
        (schema.core/both schema.core/Str
                          (schema.core/pred (fn [v#]
                                              (not (clojure.string/blank? v#))
                                              ) 'not-blank?))})
    #_(-spec-valid? [this v] (s/validate (-spec this) v))
    (-emit [_ w]
      (sql-emit w)))
