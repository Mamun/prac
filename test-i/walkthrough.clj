(ns walkthrough
  (:require [clojure.java.jdbc :as jdbc]
            [dadysql.jdbc :as t]
            ;[dadysql.spec :refer :all]
            [test-data :as td]
            [clojure.spec :as s]
            ))



;(integer? 3)
;(get-in u/tie-system-v [:tms])

(comment









  #_(s/valid? (s/or :name string?
                  :id   integer?) :keyowrd)

  ;; Create database table and init data
  (do
    (td/get-ds)
    (-> (td/get-ds)
        (t/db-do  [:create-ddl :init-data] (t/read-file "tie.edn.sql"))))


  (-> @td/ds
      (t/db-do  [:drop-ddl] (t/read-file "tie.edn.sql")))

  ;; Validate all sql statment with database
  (-> (t/validate-dml! @td/ds (t/get-dml (t/read-file "tie.edn.sql")))
      (clojure.pprint/pprint))


  ;; jdbc query example
  (-> @td/ds
      (jdbc/query ["select * from department "]))


  ;; Simple pull
  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:name :get-dept-list}) )


  ;;With model name when name as vector
  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:name [:get-dept-list]})
      (clojure.pprint/pprint)
      )



  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:name [:get-dept-list :get-employee-list]})
      (clojure.pprint/pprint))





  ;; with params
  (-> @td/ds
      (t/pull (t/read-file "tie.edn2.sql")
              {:name   [:get-dept-by-id]
               :params {:id "sdf"}}
              ;:dadysql.core/rformat :map
              )
      (clojure.pprint/pprint))


  ;(jdbc/query @td/ds "select * from department where id = 1 ")
  ;(jdbc/execute! @td/ds ["insert into department (id, transaction_id, dept_name) values (9, 0, 'Business' )"])


  ;; for sequence of params

  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:name   [:get-dept-by-ids]
               :params {:id (list 1 2 3)}})
      (clojure.pprint/pprint))



  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:name   [:get-dept-by-ids]
               :params {:id [1 2 112]}})
      (clojure.pprint/pprint))


  ;; for join data
  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              :name [:get-dept-by-id :get-dept-employee]
              :params {:id 1})
      (clojure.pprint/pprint))


  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              :name [:get-employee-by-id :get-employee-dept :get-employee-detail]
              :params {:id 1}
              )
      (clojure.pprint/pprint))


  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:name   [:get-employee-detail]
               :params {:id 1}}
              ;:dadysql.core/rformat :nested-join
              )
      (clojure.pprint/pprint))



  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              :gname :load-employee
              :params {:id 1}
              ;:dadysql.core/rformat :nested-join
              )
      (clojure.pprint/pprint))



  ;; Write single
  (let [v {:dept_name "Call Center Munich"}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 {:name   :create-dept
                  :params v}
                 ;:dadysql.core/rformat :as-sequence
                 )
        (clojure.pprint/pprint)))




  ;; Wirite with batch!
  (let [d {:department {:dept_name "Call Center Munich 2"}}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 {:name   [:create-dept]
                  :params d}

                 )
        (clojure.pprint/pprint)))




  ;(:insert-dept (t/read-file "tie.edn.sql"))

  (let [d {:dept_name "Call Center Munich 2"}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 :name :insert-dept
                 :params d)
        (clojure.pprint/pprint)))


  ;; Write with batch!
  (let [d {:department [{:dept_name "Software dept "}
                        {:dept_name "Hardware dept"}]}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 :name [:insert-dept]
                 :params d)
        (clojure.pprint/pprint)))



  ;; Check all depts
  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              {:name [:get-dept-list]})
      (clojure.pprint/pprint))







  ;; for single result
  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              :name [:gen-dept :gen-empl]
              ;:dadysql.core/rformat :map
              )
      (clojure.pprint/pprint))


  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              :name :gen-dept
              :dadysql.core/rformat value-format)
      (clojure.pprint/pprint))

  ;; Update department name
  (let [d {:department {:dept_name "Call Center Munich 1" :transaction_id 0 :id 1}}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 :name [:update-dept]
                 :params d)
        (clojure.pprint/pprint)))

  (let [d {:dept_name "Call Center Munich 1" :transaction_id 0 :id 2}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 {
                  :name   :update-dept
                  :params d})
        (clojure.pprint/pprint)))


  ;; Delete department
  (let [input {:department {:id [101 102]}}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 :name [:delete-dept]
                 :params input)
        (clojure.pprint/pprint)))



  ;;;;;;;;;;;;; Employee  ###############################################################

  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              :name [:get-employee-list])
      (clojure.pprint/pprint))


  (-> @td/ds
      (t/pull (t/read-file "tie.edn.sql")
              :name :get-employee-by-id
              :params {:id 1}
              :dadysql.core/rformat :array
              )
      (clojure.pprint/pprint))


  ;; Write join data
  (let [employee {:employee {:firstname       "Schwan"
                             :lastname        "Ragg"
                             :dept_id         1
                             :employee-detail {:street  "Schwan",
                                               :city    "Munich",
                                               :state   "Bayern",
                                               :country "Germany"}}}]
    (-> @td/ds
        (t/push! (t/read-file "tie.edn.sql")
                 :name [:create-employee :create-employee-detail]
                 :params employee
                 ;:iormat as-model
                 ;:dadysql.core/rformat as-model
                 )
        (clojure.pprint/pprint)
        )
    )




  ;;;;;######################## Meeting [Employee n:n Meeting] ############


  ;; read all meeting
  (-> {:datasource @conn}
      (t/pull (t/read-file "tie.edn.sql")
              {:name [:get-meeting-list]})
      (clojure.pprint/pprint))


  ;; add new meeting
  (let [meeting {:meeting
                 {:subject "Hello Meeting for IT"
                  }}]
    (->
      @td/ds
      (t/push! (t/read-file "tie.edn.sql")
               {:name   [:create-meeting]
                :params meeting})
      (clojure.pprint/pprint)
      )
    )

  ;; join with n-n
  (let [meeting {:meeting {:subject  "Hello Meeting for Manager"
                           :employee {:id 112}}}]
    (->
      (t/read-file "tie.edn.sql")
      (t/push! @td/ds
               {:name   [:create-meeting :create-employee-meeting]
                :params meeting})
      (clojure.pprint/pprint)
      )
    )


  (let [meeting {:meeting {:subject  "Hello Meeting for IT"
                           :employee [{:current_transaction_id 1,
                                       :dept_id                2,
                                       :lastname               "Zoma",
                                       :firstname              "Abba"
                                       :id                     1}
                                      {:current_transaction_id 1,
                                       :dept_id                2,
                                       :lastname               "Zoma",
                                       :firstname              "Abba"
                                       :id                     2}]}}]
    (-> (t/read-file "tie.edn.sql")
        (t/push! @td/ds
                 :name [:create-meeting :create-employee-meeting]
                 :params meeting)
        (clojure.pprint/pprint)))



  ;;;; Check sql tracking

  (t/start-tracking :hello
                    (fn [v]
                      (clojure.pprint/pprint v)))

  (t/stop-tracking :hello)

  (t/start-sql-execution)






  )
