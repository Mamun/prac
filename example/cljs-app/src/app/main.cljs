(ns ^:figwheel-always app.main
  (:require [tools.client :as tiesql]
            [devcards.core]
            [tiesql.common :as v]
            [cljs.core.async :refer [<! >! timeout chan]])
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [devcards.core :as dc :refer [defcard deftest]]
    [cljs.test :refer [is testing async]]
    [tools.macro :refer [defcard-tiesql]]))


(defn fig-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  ;        (query "http://localhost:3000/tie" [:get-dept-by-id] {:id 1} handler)
  )

#_(defcard Hello
         "Hello"
         {:a 3})


#_(defcard Hello
         "Hello from dev card "
         ;(failed? (fail "Hello"))
         (v/failed?
           ;{:1 2}
           (v/fail "Hello")))


#_(deftest Checktest
         (testing "sfsdf"
           (is (= 1 1))))

;(devcards.core/start-devcard-ui!)

(defcard-tiesql get-dept-by-id
                "**With name keyword**"
                tiesql/pull "/"
                :name :get-dept-by-id
                :params {:id 1})



(defcard-tiesql employee-by-id
                "**Join example**"
                tiesql/pull "/"
                :name [:get-employee-by-id :get-employee-dept]
                :params {:id 1})




#_(defcard-tiesql load-dept
                "**Load Department 2**  "
                tiesql/pull "/"
                :gname :load-dept
                :params {:id 1})


#_(defcard-tiesql load-employee
                "**Load Employee**  "
                tiesql/pull "/"
                :gname :load-employee
                :params {:id 1})



#_(defcard-tiesql dept-list
                "Load dept list as array  "
                tiesql/pull "/"
                :name [:get-dept-list])



#_(defcard-tiesql insert-dept
                "Create department  "
                tiesql/push! "/"
                :name [:create-dept]
                :params {:department {:dept_name "Call Center 9"}})






#_(defcard-tiesql create-employee
                "Create employee  "
                tiesql/push! "/"
                :name [:create-employee :create-employee-detail]
                :params {:employee {:firstname       "Schwan"
                                    :lastname        "Ragg"
                                    :dept_id         1
                                    :employee-detail {:street  "Schwan",
                                                      :city    "Munich",
                                                      :state   "Bayern",
                                                      :country "Germany"}}})



#_(go
    (print
      (<! (tiesql/pull "/"
                       :name :get-dept-by-id
                       :params {:id 1}
                       ))))


;(set! devcards.core/test-timeout 5000)



#_(defcard pull-test
           (dc/tests
             "## pull testing  "
             (testing "testing pull with :get-dept-by-id "
               (async done
                 (go
                   (<! (timeout 100))
                   (is (= (<! (tiesql/pull "/"
                                           :name :get-dept-by-id
                                           :params {:id 1}
                                           ))

                          ))
                   (is (= (<! (tiesql/pull "/"
                                           :name [:get-dept-by-id]
                                           :params {:id 1}))
                          [{:department {:id 1, :transaction_id 0, :dept_name "Business"}}
                           nil]
                          ))
                   (is (= (<! (tiesql/pull "/"
                                           :name [:get-employee-by-id :get-employee-dept]
                                           :params {:id 1}))
                          [{:employee
                            {:id             1,
                             :transaction_id 0,
                             :firstname      "Abba",
                             :lastname       "Zoma",
                             :dept_id        1,
                             :department     {:id 1, :transaction_id 0, :dept_name "Business"}}}
                           nil]))
                   (done))))
             (testing "testing pull1 for join "
               (async fjoin
                 (go
                   (is (= (<! (tiesql/pull "/"
                                           :gname :load-dept
                                           :params {:id 1}
                                           ))
                          [{:department
                            {:id             1,
                             :transaction_id 0,
                             :dept_name      "Business",
                             :employee
                                             [{:id             1,
                                               :transaction_id 0,
                                               :firstname      "Abba",
                                               :lastname       "Zoma",
                                               :dept_id        1}]}}
                           nil]))
                   (is (= (<! (tiesql/pull "/"
                                           :gname :load-employee
                                           :params {:id 1}
                                           ))
                          [{:employee
                            {:id             1,
                             :transaction_id 0,
                             :firstname      "Abba",
                             :lastname       "Zoma",
                             :dept_id        1,
                             :employee-detail
                                             {:employee_id 1,
                                              :street      "Schwan",
                                              :city        "Munich",
                                              :state       "Bayern",
                                              :country     "GRE"},
                             :meeting
                                             [[:meeting_id :subject :employee_id]
                                              [1 "Hello" 1]
                                              [2 "Hello Friday" 1]],
                             :department     {:id 1, :transaction_id 0, :dept_name "Business"}}}
                           nil]
                          ))
                   (fjoin)
                   )))
             (testing "testing pull with fails"
               (async fpull
                 (go
                   (is (= (<! (tiesql/pull "/"
                                           :name [:get-dept-by-id]
                                           ))
                          [{:department {:id 1, :transaction_id 0, :dept_name "Business"}}
                           nil]))

                   (fpull))))))



#_(defcard pull-test2
           (dc/tests
             "## pull testing  "
             (testing "testing pull with :get-dept-by-id "
               (async done1
                 (go
                   (<! (timeout 100))
                   (is (= (<! (tiesql/pull "/"
                                           :name [:get-dept-list]
                                           ))
                          [{:department
                            [[:id :transaction_id :dept_name]
                             [1 0 "Business"]
                             [2 0 "Marketing"]
                             [3 0 "HR"]]}
                           nil]))
                   (is (= (<! (tiesql/pull "/"
                                           :name :get-dept-list
                                           :rformat :array
                                           ))
                          [[[:id :transaction_id :dept_name]
                            [1 0 "Business"]
                            [2 0 "Marketing"]
                            [3 0 "HR"]]
                           nil]))
                   (done1))))))



#_(defcard push!-test
           (dc/tests
             "## push! testing  "
             (testing "testing pull with :get-dept-by-id "
               (async tpush
                 (go
                   (<! (timeout 100))
                   (is (= (<! (tiesql/push! "/"
                                            :name :create-dept
                                            :params {:dept_name "Call Center 9"}
                                            ))
                          [[1] nil]))
                   (is (= (<! (tiesql/push! "/"
                                            :name [:create-dept]
                                            :params {:department {:dept_name "Call Center 9"}}
                                            ))
                          [{:department [1]} nil]))
                   (is (= (<! (tiesql/push! "/"
                                            :name [:create-employee :create-employee-detail]
                                            :params {:employee {:firstname       "Schwan"
                                                                :lastname        "Ragg"
                                                                :dept_id         1
                                                                :employee-detail {:street  "Schwan",
                                                                                  :city    "Munich",
                                                                                  :state   "Bayern",
                                                                                  :country "Germany"}}}
                                            ))
                          [{:employee [1], :employee-detail [1]} nil]))


                   (tpush))))))







#_(tiesql/pull :url "/"
               :name [:get-depts2]
               :callback #(defcard get-dept2
                                   "# pull-batch!
                                   It will return array as format [v e] "
                                   %))

#_(def dept2 (atom nil))

#_(defcard get-depts2
           "# pull get-dept2
           ```
           (tiesql/pull :url \"/\"
           :name :get-depts2
           :callback println )

           ```
           "
           dept2)


#_(tiesql/pull "/"
               :name [:get-depts2]
               :callback (fn [w]
                           (reset! dept2 w)))



#_(tiesql/push! :url "/"
                :name [:insert-dept]
                :params {:department {:dept_name "Call Center 9"}}
                :callback print)




(defn log [v]
  (.log js/console (str (js->clj v))))







(comment

  (enable-console-print!)

  (tiesql/pull "/" #js ["get-depts"] #js {"id" 1} tiesql/h-options print)

  (tiesql/pull "/" :name [:get-depts]
               :params {:id 1}
               ;   :options tiesql/accept-html
               :callback log

               )


  (tiesql/pull "/" :name ["get-depts"]
               :params {"id" 1}
               :options tiesql/h-options
               :callback log

               )

  (tiesql/pull "/" #js ["get-depts2"] #js {"id" 1} print)

  (tiesql/push "/" #js ["get-depts2"] #js {"id" 1} (fn [v] (println v)))

  )
















