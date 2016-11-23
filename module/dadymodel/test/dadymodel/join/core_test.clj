(ns dadymodel.join.core-test
  (:use [clojure.test])
  (:require [dadymodel.join.core :refer :all]
            [dadymodel.core :as dc]
            [clojure.spec :as s]
            [clojure.spec.gen :as g]
            ))


(comment

  (run-tests)

  (dc/defmodel app {:dept    {:req {:id   int?
                                    :name string?}
                              :opt {:note string?}}
                    :student {:req {:name string?
                                    :id   int?}}}
               :dadymodel.core/join [[:dept :id :dadymodel.core/rel-1-n :student :dept-id]])


  (clojure.pprint/pprint
    (binding [s/*recursion-limit* 0]
      (let [j [[:dept :id :dadymodel.core/rel-1-n :student :dept-id]]
            w (first (g/sample (s/gen :entity.unq-app/dept) 1))
            a (do-join-impl (do-disjoin-impl w j) j)]
        (clojure.pprint/pprint w)
        (clojure.pprint/pprint a)
        (is (= a w))
        )))

  )







;(group-by-join-coll-test)

(deftest do-join-test
  (testing "test do-join "
    (let [join [[:tab :id :dadymodel.core/rel-1-1 :tab1 :tab-id]
                [:tab :tab4-id :dadymodel.core/rel-n-1 :tab4 :id]
                ]

          data {:tab  {:id 100 :tab4-id 1}
                :tab1 {:tab-id 100}
                :tab4 {:id 1}
                }
          expected-result {:tab
                           {:id      100
                            :tab4-id 1
                            :tab1    {:tab-id 100}
                            :tab4    {:id 1}
                            }}
          actual-result (do-join-impl data join)]

      (is (= actual-result
             expected-result)))))



(deftest do-disjoin-test
  (testing "test do-disjoin with :dadymodel.core/rel-1-n relationship "
    (let [join [[:tab :id :dadymodel.core/rel-1-n :tab1 :tab-id]]
          data {:tab {:id        100
                      :tab1-list [{:tab-id 100 :name "name1"}
                                  {:tab-id 100 :name "name2"}]}}
          expected-result {:tab  {:id 100}
                           :tab1 [{:tab-id 100 :name "name1"}
                                  {:tab-id 100 :name "name2"}]}
          actual-result (do-disjoin-impl data join)]
      (is (= actual-result
             expected-result))))
  (testing "test do-disjoin with :n-n relationship "
    (let [join [[:tab :id :dadymodel.core/rel-n-n :tab1 :tab-id [:ntab :tab-id :tab1-id]]]
          data {:tab {:id        100
                      :tab1-list [{:tab-id 100}
                                  {:tab-id 101}]}}
          expected-result {:tab  {:id 100},
                           :tab1 [{:tab-id 100} {:tab-id 101}]}
          actual-result (do-disjoin-impl data join)]
      (is (= actual-result
             expected-result))))
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
                           {:street  "Schwan",
                            :city    "Munich",
                            :state   "Bayern",
                            :country "Germany"}}
          actual-result (do-disjoin-impl data j)]
      (is (= expected-result actual-result)))))




(comment

  (run-tests)
  )


