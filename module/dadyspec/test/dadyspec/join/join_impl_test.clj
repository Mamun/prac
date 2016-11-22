(ns dadyspec.join.join-impl-test
  (:use [clojure.test])
  (:require [dadyspec.join.join-impl :refer :all]
            [dadyspec.core :as dc]
            [clojure.spec :as s]
            [clojure.spec.gen :as g]
            ))


(comment

  (run-tests)


  (dc/defentity app {:dept {:req {:id  int?
                                 :name string?}
                           :opt  {:note string?}}
                 :student  {:req {:name string?
                                 :id   int?}}}
                :dadyspec.core/join [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]]
                :dadyspec.core/gen-type #{:dadyspec.core/qualified
                                      :dadyspec.core/un-qualified
                                      :dadyspec.core/ex})


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (g/sample (s/gen :entity.unq-app/dept) 1)
      ))




  (let [j [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]]
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
    (let [join [[:tab :id :dadyspec.core/rel-1-1 :tab1 :tab-id]
                [:tab :tab4-id :dadyspec.core/rel-n-1 :tab4 :id]
                [:tab :id :dadyspec.core/rel-n-n :tab2 :id [:tab-tab1 :tab-id :tab2-id]]]

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



;(do-join-test)



#_(let [join [[:tab :id :dadyspec.core/rel-1-1 :tab1 :tab-id]
              [:tab :id :dadyspec.core/rel-1-n :tab2 :tab-id]
              [:tab :tab4-id :dadyspec.core/rel-n-1 :tab4 :id]
              [:tab :id :dadyspec.core/rel-n-n :tab4 :id [:tab-tab1 :tab-id :tab2-id]]]

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


#_(let [join [[:tab :id :dadyspec.core/rel-1-n :tab1 :tab-id]]
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


