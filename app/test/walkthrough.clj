(ns walkthrough
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

  (t/read-file "app.sql")

  (->> {:dadysql.core/name [:init-db :init-data]}
       (t/select-name (t/read-file "app.sql"))
       (io/db-do @ds))

  ;; Pull API 

  ;; Load one table
  (->> {:dadysql.core/name [:get-dept-list]}
       (t/pull @ds (t/read-file "app.sql")))

  ;; Load two table
  (->> {:dadysql.core/name [:get-dept-list :get-employee-list]}
       (t/pull @ds (t/read-file "app.sql")));; sql in


  ;;; Sql in operation 
  (->> {:dadysql.core/name  [:get-dept-by-ids]
        :dadysql.core/param {:id [1 2 3 5 6]}}
       (t/pull @ds (t/read-file "app.sql")));; Clojure spec conform string as correct type



  ;; With string conformation 
  (->> {:dadysql.core/name  [:get-dept-by-ids]
        :dadysql.core/param {:id [1 2 "324"]}}
       (t/pull @ds (t/read-file "app.sql")))   ;; Load sequence


  ;; call sequence 
  (->> {:dadysql.core/name [:gen-dept :gen-empl :gen-meet]}
       (t/pull @ds (t/read-file "app.sql")))  ;; relational data

  ;; call multi with parameter 
  (->> {:dadysql.core/name  [:get-employee-by-id :get-employee-dept :get-employee-detail]
        :dadysql.core/param {:id 1}}
       (t/pull @ds (t/read-file "app.sql")))

  ;; call for grou p
  (->> {:dadysql.core/group :load-employee
        :dadysql.core/param {:id 1}}
       (t/pull @ds (t/read-file "app.sql")))



  ;;;;;;;;;;;;;;;;;;;, Push API 


  ;; As entity type 
  (->> {:dadysql.core/name  [:create-dept]
        :dadysql.core/param {:department {:dept_name "Call Center Munich 2"}}}
       (t/push! @ds (t/read-file "app.sql"))
       )

  ;; Write join data


  ;; Complex entity type 
  (let [employee {:employee {:firstname       "Schwan"
                             :lastname        "Ragg"
                             :dept_id         1
                             :employee-detail {:street  "Schwan",
                                               :city    "Munich",
                                               :state   "Bayern",
                                               :country "Germany"}}}]
    (->> {:dadysql.core/name  [:create-employee :create-employee-detail]
          :dadysql.core/param employee}
         (t/push! @ds (t/read-file "app.sql"))))


  ;; Entity as list 
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
         (t/push! @ds (t/read-file "app.sql")) ))
  


  ;; Update data

  ;;  Optimistic concurrency


  (->> {:dadysql.core/name [:get-dept-list :get-employee-list]}
       (t/pull @ds (t/read-file "app.sql"))) ;; sql in



  (->> {:dadysql.core/name  [:update-employee-dept]
        :dadysql.core/param {:employee {:dept_id 3 :transaction_id 0 :id 2 }}}
       (t/push! @ds (t/read-file "app.sql")))










;;; Load default params


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
                          :dadysql.core/param meeting}) ))



  (let [d {:department [{:dept_name "Software dept "}
                        {:dept_name "Hardware dept"}]}]
    (-> @ds
        (t/default-param (t/read-file "app.sql")
          {:dadysql.core/name [:create-dept]
           :dadysql.core/param d}) ))



  )
