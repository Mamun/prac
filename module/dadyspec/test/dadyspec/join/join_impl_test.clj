(ns dadyspec.join.join-core-test
  (:use [clojure.test])
  (:require [dadyspec.join.join-impl :refer :all]
            [dadyspec.core :as dc]
            [clojure.spec :as s]
            [clojure.spec.gen :as g]
            ))


(comment

  (run-tests)



  (dc/defsp app {:dept    {:req {:id   int?
                              :name string?}
                        :opt {:note string?}}
              :student {:req {:name string?
                              :id   int?}}}
         :dadyspec.core/join [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]]
         :dadyspec.core/gen-type #{:dadyspec.core/qualified
                                   :dadyspec.core/unqualified
                                   :dadyspec.core/ex})


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (g/sample (s/gen :entity.un-app/dept) 1)
      ))




  (let [j [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]]
        ;j (rename-joi-key j)

        data {:dept
              {:id 0,
               :name "",
               :student-list
               [{:name "", :id 0}
                {:name "", :id -1}]
               :note ""}}]
    (assoc-join-key data j)
    #_(do-disjoin (assoc-join-key data j) j)

    )


  )







;(group-by-join-coll-test)

(deftest do-join-test
  (testing "test do-join "
    (let [join [[:tab :id :dadyspec.core/rel-1-n :tab2 :tab-id]
                [:tab :id :dadyspec.core/rel-1-n :tab3 :tab-id]]

          data {:tab2 [[:tab-id :id] [100 1] [100 2] [1 3]]
                :tab  [{:id 100}]}

          expected-result {:tab
                           [{:id   100
                             :tab2 [[:tab-id :id] [100 1] [100 2]]
                             :tab3 nil}]}
          actual-result (do-join (assoc-join-key data join) join)]
      (is (= expected-result actual-result))))
  (testing "test do-join "
    (let [join [[:tab :id :dadyspec.core/rel-1-1 :tab1 :tab-id]
                [:tab :tab4-id :dadyspec.core/rel-n-1 :tab4 :id]
                [:tab :id :dadyspec.core/rel-n-n :tab2 :id [:tab-tab1 :tab-id :tab2-id]]]

          data {:tab      {:id 100 :tab4-id 1}
                :tab1     {:tab-id 100}
                :tab4     {:id 1}
                :tab-tab1 [{:tab2-id 102 :tab-id 100}
                           {:tab2-id 103 :tab-id 100}]}
          expected-result {:tab
                           {:id      100
                            :tab4-id 1
                            :tab1    {:tab-id 100}
                            :tab4    {:id 1}
                            :tab2    [{:tab2-id 102 :tab-id 100}
                                      {:tab2-id 103 :tab-id 100}]}}
          actual-result (do-join (assoc-join-key data join) join)]
      (is (= actual-result
             expected-result)))))



;(do-join-test)


(deftest assoc-target-entity-key-test
  (testing "test acc-ref-key "
    (let [r [[:tab] :id :dadyspec.core/rel-1-n [:tab :tab1 0] :tab-id]
          data {:tab {:id 100, :tab1 [{:tab-id 101}]}}
          expected-result {:tab {:id 100, :tab1 [{:tab-id 100}]}}
          atual-result (assoc-target-entity-key data r)]
      (is (= atual-result
             expected-result))))
  (testing "test acc-ref-key "
    (let [r [[:tab] :id :dadyspec.core/rel-1-n [:tab :tab1 0] :tab-id]
          data {:tab {:id 100}}
          expected-result {:tab {:id 100}}
          atual-result (assoc-target-entity-key data r)]
      (is (= atual-result
             expected-result))))
  (testing "test acc-ref-key with worng target path "
    (let [r [[:tab] :id :dadyspec.core/rel-1-n [:tab :tab1] :tab-id]
          data {:tab {:id 100, :tab1 [{:tab-id 101}]}}
          expected-result {:tab {:id   100
                                 :tab1 [{:tab-id 101}]}}
          atual-result (assoc-target-entity-key data r)]
      (is (= atual-result
             expected-result))))
  (testing "test acc-ref-key for map target "
    (let [r [[:tab] :id :dadyspec.core/rel-1-n [:tab :tab1] :tab-id]
          data {:tab {:id 100, :tab1 {:tab-id 101}}}
          expected-result {:tab {:id   100
                                 :tab1 {:tab-id 100}}}
          atual-result (assoc-target-entity-key data r)]
      (is (= atual-result
             expected-result))))
  (testing "test assoc-target-entity-key "
    (let [r [[:employee] :id :dadyspec.core/rel-1-1 [:employee :employee-detail] :employee_id]
          data {:employee {:firstname       "Schwan",
                           :lastname        "Ragg",
                           :dept_id         1,
                           :transaction_id  0,
                           :id              109
                           :employee-detail {:street  "Schwan",
                                             :city    "Munich",
                                             :state   "Bayern",
                                             :country "Germany"}}}
          atual-result (assoc-target-entity-key data r)
          ]
      (is (= (get-in data [:employee :id])
             (get-in atual-result [:employee :employee-detail :employee_id])
             ))

      ))
  )




;(dest-rel-data-test)



;(ndest-rel-data-test)


#_(let [join [[:tab :id :dadyspec.core/rel-1-n :tab1 :tab-id]]
      data {:tab {:id   100
                  :tab1 [{ :name "name1"}
                         { :name "name2"}]}}

      ]
  (clojure.pprint/pprint
    (assoc-join-key data join))

  )

(deftest do-disjoin-test
  (testing "test do-disjoin with :dadyspec.core/rel-1-n relationship "
    (let [join [[:tab :id :dadyspec.core/rel-1-n :tab1 :tab-id]]
          data {:tab {:id   100
                      :tab1 [{:tab-id 100 :name "name1"}
                             {:tab-id 100 :name "name2"}]}}
          expected-result {:tab  {:id 100}
                           :tab1 [{:tab-id 100 :name "name1"}
                                  {:tab-id 100 :name "name2"}]}
          actual-result (do-disjoin (assoc-join-key data join) join)]
      (is (= actual-result
             expected-result))))
  (testing "test do-disjoin with :dadyspec.core/rel-1-n relationship "
    (let [join [[:tab :id :dadyspec.core/rel-1-n :tab1 :tab-id]]
          data {:tab [{:id   100
                       :tab1 [{:tab-id 100 :name "name1"}
                              {:tab-id 100 :name "name2"}]}
                      {:id   100
                       :tab1 {:tab-id 138 :name "name3"}}]}
          expected-result {:tab  [{:id 100} {:id 100}]
                           :tab1 (list {:tab-id 100 :name "name1"}
                                       {:tab-id 100 :name "name2"}
                                       {:tab-id 100 :name "name3"})}
          actual-result (do-disjoin (assoc-join-key data join) join)]
      (is (= actual-result
             expected-result))))
  (testing "test do-disjoin with :n-n relationship "
    (let [join [[:tab :id :dadyspec.core/rel-n-n :tab1 :tab-id [:ntab :tab-id :tab1-id]]]
          data {:tab [{:id   100
                       :tab1 [{:tab-id 100}
                              {:tab-id 101}]}
                      {:id   102
                       :tab1 {:tab-id 138}}]}
          expected-result {:tab  [{:id 100} {:id 102}],
                           :tab1 [{:tab-id 100} {:tab-id 102}]
                           :ntab [{:tab-id 100, :tab1-id 100}
                                  {:tab-id 100, :tab1-id 101}
                                  {:tab-id 102, :tab1-id 138}]}
          actual-result (do-disjoin (assoc-join-key data join) join)]
      (is (= actual-result
             expected-result)))
    )
  (testing "test do-join"
    (let [j [[:employee :id :dadyspec.core/rel-1-1 :employee-detail :employee_id]]
          data {:employee {:firstname       "Schwan",
                           :lastname        "Ragg",
                           :dept_id         1,
                           :transaction_id  0,
                           :id              109
                           :employee-detail {:street  "Schwan",
                                             :city    "Munich",
                                             :state   "Bayern",
                                             :country "Germany"}}}
          expected-result {:employee
                           {:firstname      "Schwan",
                            :lastname       "Ragg",
                            :dept_id        1,
                            :transaction_id 0,
                            :id             109},
                           :employee-detail
                           {:street      "Schwan",
                            :city        "Munich",
                            :state       "Bayern",
                            :country     "Germany",
                            :employee_id 109}

                           }
          actual-result (do-disjoin (assoc-join-key data j) j)]
      (is (= expected-result actual-result)))))

(comment

  (run-tests)
  )


;(do-disjoin-test)


;(do-disjoin-test)

;(run-tests)
;(do-disjoin-test-1)

#_(deftest group-by-join-src-test
  (testing "test group-by-join-src "
    (let [v [[:tab :id :dadyspec.core/rel-1-n :tab1 :tab-id]]
          expected-result {:tab {:dadysql.core/join [[:tab :id :dadyspec.core/rel-1-n :tab1 :tab-id]]}}
          actual-result (group-by-join-src v)]
      (is (= actual-result
             expected-result)))))

;(group-by-join-src-test)


;(filter-join-key-coll-test)

#_(deftest join-emission-batch-test
  (testing "test join-emission-batch "
    (let [join [[:tab :ID :dadyspec.core/rel-1-n :tab2 :tab-id]
                [:tab :id :dadyspec.core/rel-1-n :tab3 :tab-ID]
                [:tab :id :dadyspec.core/rel-n-n :tab1 :tab-id [:ntab :tab-ID :tab1-id]]]
          expected-result [[:tab :id :dadyspec.core/rel-1-n :tab2 :tab-id]
                           [:tab :id :dadyspec.core/rel-1-n :tab3 :tab-id]
                           [:tab :id :dadyspec.core/rel-n-n :tab1 :tab-id [:ntab :tab-id :tab1-id]]]
          actual-result (join-emission-batch join)]
      (is (= expected-result actual-result)))))

;(join-emission-batch-test)