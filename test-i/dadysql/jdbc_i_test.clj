(ns dadysql.jdbc-i-test
  (:use [clojure.test])
  (:require [dady.common :refer :all]
            [dadysql.jdbc :refer :all]
            [test-data :as td]))

(comment

  (run-tests)
  )


(use-fixtures :once (fn [f]
                      (td/get-ds)
                      (td/get-tms)
                      nil
                      (f)))


(deftest pull-test
  (testing "test pull for sequence "
    (let [v (pull (td/get-ds) (td/get-tms) {:dadysql.core/name [:gen-dept]})]
      (is (not (nil? v)))))

  (testing "testing pull "

    (is (= (pull (td/get-ds) (td/get-tms)
                 {:dadysql.core/name   :get-dept-by-id
                  :dadysql.core/input {:id 1}})
           {:id 1, :transaction_id 0, :dept_name "Business"}))

    (is (= (pull (td/get-ds) (td/get-tms)
                 {:dadysql.core/name   [:get-dept-by-id]
                  :dadysql.core/input {:id 1}})
           {:department {:id 1, :transaction_id 0, :dept_name "Business"}}))

    (is (= (pull (td/get-ds) (td/get-tms) {:dadysql.core/name [:get-dept-list]})
           {:department [[:id :transaction_id :dept_name]
                         [1 0 "Business"]
                         [2 0 "Marketing"]
                         [3 0 "HR"]]}))

    (is (= (pull (td/get-ds) (td/get-tms) {:dadysql.core/name :get-dept-list})
           [[:id :transaction_id :dept_name]
            [1 0 "Business"]
            [2 0 "Marketing"]
            [3 0 "HR"]]))

    (is (= (pull (td/get-ds) (td/get-tms)
                 {:dadysql.core/name   [:get-employee-by-id :get-employee-dept]
                  :dadysql.core/input {:id 1}})
           {:employee
            {:id             1,
             :transaction_id 0,
             :firstname      "Abba",
             :lastname       "Zoma",
             :dept_id        1,
             :department     {:id 1, :transaction_id 0, :dept_name "Business"}}}))

    (is (= (pull (td/get-ds) (td/get-tms)
                 {:dadysql.core/name   [:get-employee-by-id :get-employee-dept :get-employee-detail :get-employee-meeting]
                  :dadysql.core/input {:id 1}})
           {:employee {:id              1,
                       :transaction_id  0,
                       :firstname       "Abba",
                       :lastname        "Zoma",
                       :dept_id         1,
                       :employee-detail {:employee_id 1,
                                         :street      "Schwan",
                                         :city        "Munich",
                                         :state       "Bayern",
                                         :country     "GRE"},
                       :meeting         [[:meeting_id :subject :employee_id]
                                         [1 "Hello" 1]
                                         [2 "Hello Friday" 1]],
                       :department      {:id 1, :transaction_id 0, :dept_name "Business"}}}))
    (is (= (pull (td/get-ds) (td/get-tms)
                 {:dadysql.core/group  :load-dept
                  :dadysql.core/input {:id 1}})
           {:department
            {:id             1,
             :transaction_id 0,
             :dept_name      "Business",
             :employee       [{:id             1,
                               :transaction_id 0,
                               :firstname      "Abba",
                               :lastname       "Zoma",
                               :dept_id        1}]}}))
    (is (= (pull (td/get-ds) (td/get-tms)
                 {:dadysql.core/group  :load-employee
                  :dadysql.core/input {:id 1}})


           {:employee
            {:id              1,
             :transaction_id  0,
             :firstname       "Abba",
             :lastname        "Zoma",
             :dept_id         1,
             :employee-detail {:employee_id 1,
                               :street      "Schwan",
                               :city        "Munich",
                               :state       "Bayern",
                               :country     "GRE"},
             :meeting         [[:meeting_id :subject :employee_id]
                               [1 "Hello" 1]
                               [2 "Hello Friday" 1]],
             :department      {:id 1, :transaction_id 0, :dept_name "Business"}}}))
    ))


;(pull-test)

#_(clojure.pprint/pprint
  (:create-dept (td/get-tms)))

;(empty [ 1 2 3] )

(deftest push!-test
  (testing "testing push! "

    (is (= (push! (td/get-ds) (td/get-tms)
                  {:dadysql.core/name   :create-dept
                   :dadysql.core/input {:dept_name "Support "}})
           [1]))

    (is (= (push! (td/get-ds) (td/get-tms)
                  {:dadysql.core/name   [:create-dept]
                   :dadysql.core/input {:department {:dept_name "IT"}}})
           {:department [1]}))

    (is (= (push! (td/get-ds) (td/get-tms)
                  {:dadysql.core/name   [:create-employee :create-employee-detail]
                   :dadysql.core/input {:employee {:firstname       "Schwan"
                                       :lastname        "Ragg"
                                       :dept_id         1
                                       :employee-detail {:street  "Schwan",
                                                         :city    "Munich",
                                                         :state   "Bayern",
                                                         :country "Germany"}}}})
           {:employee [1] :employee-detail [1]}))

    (is (= (push! (td/get-ds) (td/get-tms)
                  {:dadysql.core/name   [:create-meeting :create-employee-meeting]
                   :dadysql.core/input {:meeting {:subject  "Hello Meeting for IT"
                                      :employee [{:current_transaction_id 1,
                                                  :dept_id                2,
                                                  :lastname               "Zoma",
                                                  :firstname              "Abba"
                                                  :id                     1}
                                                 {:current_transaction_id 1,
                                                  :dept_id                2,
                                                  :lastname               "Zoma",
                                                  :firstname              "Abba"
                                                  :id                     2}]}}})
           {:meeting [1], :employee-meeting [1 1]}))

    (is (= (push! (td/get-ds) (td/get-tms)
                  {:dadysql.core/name   [:create-meeting :create-employee-meeting]
                   :dadysql.core/input {:meeting {:subject  "Hello Meeting for Manager"
                                      :employee {:id 112}}}})
           {:meeting [1], :employee-meeting [1]}))



    (is (= (push! (td/get-ds) (td/get-tms)
                  {:dadysql.core/name   :update-dept
                   :dadysql.core/input {:dept_name "Call Center Munich 1" :transaction_id 0 :id 1}})
           [1]))

    (is (= (push! (td/get-ds) (td/get-tms)
                  {:dadysql.core/name   [:update-dept]
                   :dadysql.core/input {:department {:dept_name "Call Center Munich 1" :transaction_id 0 :id 1}}})
           {:department [1]}))
    (is (= (push! (td/get-ds) (td/get-tms)
                  {:dadysql.core/name   [:delete-dept]
                   :dadysql.core/input {:department {:id 1 }}})
           {:department [1]}))
    ))


(comment
  (push!-test)

  )

;@td/tms
;(run-tests)

(deftest validate-dml!-test
  (testing "test validate-dml!"
    (do
      (->> (validate-dml! @td/ds @td/tms))
      (is true))))


;(validate-dml!-test)
;(run-tests)


(comment

  (run-tests)

  (:delete-dept @td/tms)

  (pull @td/ds @td/tms
        {:gname  :load-dept
         :params {:id 1}})

  ;@td/tms

  )
