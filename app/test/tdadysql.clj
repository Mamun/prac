(ns tdadysql
  (:require [clojure.java.jdbc :as jdbc]
            [dadysql.jdbc :as t]
            [dadysql.jdbc-io :as io])
  (:import [com.mchange.v2.c3p0 ComboPooledDataSource]))



(defonce ds (atom nil))


(defn get-ds
  []
  (when (nil? @ds)
    (reset! ds {:datasource (ComboPooledDataSource.)})
    (println "Init datasource connection  "))
  @ds)






(comment

  (get-ds)

  (-> (t/read-file "app.sql")
      (keys)
      (clojure.pprint/pprint))


  (let [w (t/read-file "app.sql")]
    (->> (t/select-name w {:dadysql.core/name [:init-db :init-data]})
         (io/db-do (get-ds))))

  ;; Load one table
  (->> {:dadysql.core/name [:get-dept-list]}
       (t/pull @ds (t/read-file "app.sql"))
       #_(clojure.pprint/pprint))

  ;; Load two table
  (->> {:dadysql.core/name [:get-dept-list :get-employee-list]}
       (t/pull @ds (t/read-file "app.sql"))
       #_(clojure.pprint/pprint))


  ;; sql in

  (->> {:dadysql.core/name  [:get-dept-by-ids]
        :dadysql.core/param {:id [1 2 3]}}
       (t/pull @ds (t/read-file "app.sql"))
       (clojure.pprint/pprint))

  ;; Clojure spec conform string as correct type

  (->> {:dadysql.core/name  [:get-dept-by-ids]
        :dadysql.core/param {:id [1 2 "3"]}}
       (t/pull @ds (t/read-file "app.sql"))
       )



  ;; Load sequence
  (->> {:dadysql.core/name [:gen-dept :gen-empl :gen-meet]}
       (t/pull @ds (t/read-file "app.sql"))
       )


  ;; relational data
  (->> {:dadysql.core/name  [:get-employee-by-id :get-employee-dept :get-employee-detail]
        :dadysql.core/param {:id 1}}
       (t/pull @ds (t/read-file "app.sql"))
       (clojure.pprint/pprint))



  (->> {:dadysql.core/group :load-employee
        :dadysql.core/param {:id 1}
        ;:dadysql.core/output-format :nested-join
        }

       (t/pull @ds (t/read-file "tie.edn.sql"))
       (clojure.pprint/pprint))



  ;;database insert op

  (->> {:dadysql.core/name  [:create-dept]
        :dadysql.core/param {:department {:dept_name "Call Center Munich 2"}}}
       (t/push! @ds (t/read-file "app.sql"))
       (clojure.pprint/pprint))


  ;; Write join data
  (let [employee {:employee {:firstname       "Schwan"
                             :lastname        "Ragg"
                             :dept_id         1
                             :employee-detail {:street  "Schwan",
                                               :city    "Munich",
                                               :state   "Bayern",
                                               :country "Germany"}}}]
    (->> {:dadysql.core/name  [:create-employee :create-employee-detail]
          :dadysql.core/param employee}
         (t/push! @ds (t/read-file "app.sql"))
         (clojure.pprint/pprint)))



  (let [meeting {:meeting {:subject  "Hello Meeting for IT"
                           :employee-list [{:current_transaction_id 1,
                                            :dept_id                2,
                                            :lastname               "Zoma",
                                            :firstname              "Abba"
                                            :id                     1}
                                           {:current_transaction_id 1,
                                            :dept_id                2,
                                            :lastname               "Zoma",
                                            :firstname              "Abba"
                                            :id                     2}]}}]
    (->> {:dadysql.core/name [:create-meeting :create-employee-meeting]
          :dadysql.core/param meeting}
         (t/push! @ds (t/read-file "app.sql"))
         (clojure.pprint/pprint)))



  (let [meeting {:meeting {:subject  "Hello Meeting for IT"
                           :employee-list [{:current_transaction_id 1,
                                            :dept_id                2,
                                            :lastname               "Zoma",
                                            :firstname              "Abba"
                                            :id                     1}
                                           {:current_transaction_id 1,
                                            :dept_id                2,
                                            :lastname               "Zoma",
                                            :firstname              "Abba"
                                            :id                     2}]}}]
    (-> @ds
        (t/default-param (t/read-file "app.sql")
                         {:dadysql.core/name [:create-meeting :create-employee-meeting]
                          :dadysql.core/op :dadysql.core/op-push!
                          :dadysql.core/param meeting}  )
        (clojure.pprint/pprint)))


  ;;; Load default params


  (let [d {:department [{:dept_name "Software dept "}
                        {:dept_name "Hardware dept"}]}]
    (-> @ds
        (t/default-param (t/read-file "app.sql")
                         {:dadysql.core/name [:create-dept]
                          :dadysql.core/param d
                          :dadysql.core/op :dadysql.core/op-push!
                          }  )
        (clojure.pprint/pprint)))



  )
