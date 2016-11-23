(ns dadymodel.join.core-test
  (:use [clojure.test])
  (:require [dadymodel.join.core :refer :all]
            [dadymodel.core :as dc]
            [clojure.spec :as s]
            [clojure.spec.gen :as g]
            ))


(comment

  (run-tests)


  (dc/defmodel app {:dept {:req {:id   int?
                                 :name string?}
                           :opt {:note string?}}
                 :student {:req {:name string?
                                 :id   int?}}}
               :dadymodel.core/join [[:dept :id :dadymodel.core/rel-1-n :student :dept-id]]
               :dadymodel.core/gen-type #{:dadymodel.core/qualified
                                      :dadymodel.core/un-qualified
                                      :dadymodel.core/ex})


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (g/sample (s/gen :entity.unq-app/dept) 1)
      ))




  (let [j [[:dept :id :dadymodel.core/rel-1-n :student :dept-id]]
        ;j (rename-joi-key j)

        data {:dept
              {:id   0,
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
    (let [join [[:tab :id :dadymodel.core/rel-1-1 :tab1 :tab-id]
                [:tab :tab4-id :dadymodel.core/rel-n-1 :tab4 :id]
                [:tab :id :dadymodel.core/rel-n-n :tab2 :id [:tab-tab1 :tab-id :tab2-id]]]

          data {:tab      {:id 100 :tab4-id 1}
                :tab1     {:tab-id 100}
                :tab4     {:id 1}
                :tab-tab1 [{:tab2-id 102 :tab-id 100}
                           {:tab2-id 103 :tab-id 100}]}
          expected-result {:tab
                           {:id        100
                            :tab4-id   1
                            :tab1      {:tab-id 100}
                            :tab4      {:id 1}
                            :tab2-list [{:tab2-id 102 :tab-id 100}
                                        {:tab2-id 103 :tab-id 100}]}}
          actual-result (do-join data join)]
      (is (= actual-result
             expected-result)))))



(deftest do-disjoin-test
  (testing "test do-disjoin with :dadymodel.core/rel-1-n relationship "
    (let [join [[:tab :id :dadymodel.core/rel-1-n :tab1 :tab-id]]
          data {:tab {:id   100
                      :tab1-list [{:tab-id 100 :name "name1"}
                                  {:tab-id 100 :name "name2"}]}}
          expected-result {:tab  {:id 100}
                           :tab1 [{:tab-id 100 :name "name1"}
                                  {:tab-id 100 :name "name2"}]}
          actual-result (do-disjoin data join)]
      (is (= actual-result
             expected-result))))
  (testing "test do-disjoin with :n-n relationship "
    (let [join [[:tab :id :dadymodel.core/rel-n-n :tab1 :tab-id [:ntab :tab-id :tab1-id]]]
          data {:tab {:id   100
                      :tab1-list [{:tab-id 100}
                                  {:tab-id 101}]}}
          expected-result {:tab {:id 100},
                           :ntab [{:tab1-id 100, :tab-id 100}
                                  {:tab1-id 100, :tab-id 101}],
                           :tab1 [{:tab-id 100} {:tab-id 100}]}


          actual-result (do-disjoin data join)
          ]

      ;(assoc-join-key data join)
      (is (= actual-result
             expected-result)))
    )
  (testing "test do-join"
    (let [j [[:employee :id :dadymodel.core/rel-1-1 :employee-detail :employee_id]]
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
          actual-result (do-disjoin data j)]
      (is (= expected-result actual-result)))))



;(do-join-test)



#_(let [join [[:tab :id :dadymodel.core/rel-1-1 :tab1 :tab-id]
              [:tab :id :dadymodel.core/rel-1-n :tab2 :tab-id]
              [:tab :tab4-id :dadymodel.core/rel-n-1 :tab4 :id]
              [:tab :id :dadymodel.core/rel-n-n :tab4 :id [:tab-tab1 :tab-id :tab2-id]]]

        data {:tab      {:id 100 :tab4-id 1}
              :tab1     {:tab-id 100}
              :tab2     [{:tab-id 100}]
              :tab4     {:id 1}
              :tab-tab1 [{:tab2-id 102 :tab-id 100}
                         {:tab2-id 103 :tab-id 100}]}]

    (do-join data join)
    )




;(dest-rel-data-test)



;(ndest-rel-data-test)


#_(let [join [[:tab :id :dadymodel.core/rel-1-n :tab1 :tab-id]]
        data {:tab {:id   100
                    :tab1 [{:name "name1"}
                           {:name "name2"}]}}

        ]
    (clojure.pprint/pprint
      (assoc-join-key data join))

    )

(comment

  (run-tests)
  )


