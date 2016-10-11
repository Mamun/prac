(ns dadysql.plugin.sql-bind-impl
  (:require [dady.fail :as f]
            [dady.common :as cc]))


(defn validate-input-not-empty!
  [m]
  (if (and (not-empty (rest (:dadysql.core/sql m)))
           (empty? (:dadysql.core/param m)))
    (f/fail (format "Input is missing for %s " (:dadysql.core/name m)))
    m))


(defn validate-input-type!
  [m]
  (let [dml-type (:dadysql.core/dml m)
        input (:dadysql.core/param m)
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
     :dadysql.core/dml :dadysql.core/dml-insert,
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
  (let [input (cc/as-sequential (:dadysql.core/param m))
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
        input (:dadysql.core/param tm)
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
    (->> (:dadysql.core/param tm)
         (cc/as-sequential)
         (mapv #(cc/select-values %1 (rest sql)))
         (reduce conj [sql-str])
         (assoc tm :dadysql.core/sql))))


(defmulti sql-bind (fn [tm] (:dadysql.core/dml tm)))


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


(defn sql-bind-batch [tm-coll]
  (transduce (map sql-bind) conj tm-coll))



