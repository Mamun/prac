(ns dadyspec.join.join-path-finder-test
  (:use [clojure.test])
  (:require [dadyspec.join.join-path-finder :refer :all]))


#_(deftest get-source-relational-key-value-test
  (testing "test get-relational-key-value"
    (let [join [[:tab :id :dadyspec.core/rel-1-n :tab2 :tab-id]
                [:tab :id :dadyspec.core/rel-1-n :tab3 :tab-id]]
          data-m {:tab [{:id 3 :name "yourname"}
                        {:id 4 :name "yourname"}]}
          expected-result {:tab-id (list 3 4)}
          actual-result (get-source-relational-key-value join data-m)]
      (is (= expected-result
             actual-result)))))


(deftest group-by-target-entity-key-batch-test
  (testing "test group-by-target-entity-key-batch"
    (let [join [[:tab :id :dadyspec.core/rel-1-n :tab2 :tab-id]
                [:tab :id :dadyspec.core/rel-1-n :tab3 :tab-id]]
          data-m {:tab  [{:id 1 :desc "tab "}
                         {:id 2 :desc "tab "}]
                  :tab2 [{:id 3 :tab-id 1 :desc "tab2 "}
                         {:id 4 :tab-id 2 :desc "tab2 "}]
                  :tab3 {:id 1 :tab-id 1 :desc "tab3 "}}
          expected-result {:tab2 {:tab-id {1 [{:id 3, :tab-id 1, :desc "tab2 "}]
                                           2 [{:id 4 :tab-id 2 :desc "tab2 "}]}},
                           :tab3 {:tab-id {1 {:id 1, :tab-id 1, :desc "tab3 "}}}}
          actual-result (group-by-target-entity-key-batch join data-m)]
      (is (= expected-result
             actual-result))
      )))


(deftest group-by-target-entity-key-batch-test
  (testing "test group-by-target-entity-key-batch "
    (let [join [[:tab :id :dadyspec.core/rel-1-n :tab2 :tab-id]
                [:tab :id :dadyspec.core/rel-1-n :tab3 :tab-id]]
          data {:tab2 [[:tab-id :id] [100 1] [100 2] [1 3]]
                :tab  [{:id 100}]}
          expected-result {:tab2 {:tab-id
                                  {100 [[:tab-id :id]
                                        [100 1]
                                        [100 2]],
                                   1   [[:tab-id :id]
                                        [1 3]]}},
                           :tab3 {:tab-id nil}}
          actual-result (group-by-target-entity-key-batch join data)]
      (is (= actual-result
             expected-result)))))


(deftest assoc-to-source-entity-batch-test
  (testing "test get-target-relational-key-value"
    (let [data-m {:tab  [{:id 1 :desc "tab "}
                         {:id 2 :desc "tab "}]
                  :tab2 [{:id 3 :tab-id 1 :desc "tab2 "}
                         {:id 4 :tab-id 2 :desc "tab2 "}]
                  :tab3 {:id 1 :tab-id 1 :desc "tab3 "}}

          join (list [[:tab 0] :id :dadyspec.core/rel-1-n :tab2 :tab-id]
                     [[:tab 1] :id :dadyspec.core/rel-1-n :tab2 :tab-id]
                     [[:tab 0] :id :dadyspec.core/rel-1-n :tab3 :tab-id]
                     [[:tab 1] :id :dadyspec.core/rel-1-n :tab3 :tab-id])

          target-g-m {:tab2 {:tab-id {1 [{:id 3, :tab-id 1, :desc "tab2 "}]
                                      2 [{:id 4 :tab-id 2 :desc "tab2 "}]}},
                      :tab3 {:tab-id {1 {:id 1, :tab-id 1, :desc "tab3 "}}}}
          expected-result {:tab [{:id   1,
                                  :desc "tab ",
                                  :tab2 [{:id 3, :tab-id 1, :desc "tab2 "}],
                                  :tab3 {:id 1, :tab-id 1, :desc "tab3 "}}
                                 {:id   2,
                                  :desc "tab ",
                                  :tab2 [{:id 4, :tab-id 2, :desc "tab2 "}],
                                  :tab3 nil}]}
          actual-result (assoc-to-source-entity-batch target-g-m data-m join)]
      (is (= expected-result (select-keys actual-result [:tab]))))))


;(assoc-to-source-entity-batch-test)


(deftest replace-source-entity-path-test
  (testing "test replace-source-entity-path "
    (let [join [[:tab :id :dadyspec.core/rel-1-n :tab2 :tab-id]
                [:tab :id :dadyspec.core/rel-1-n :tab3 :tab-id]]
          data-m {:tab2 [[:tab-id :id] [100 1] [100 2] [1 3]]
                  :tab  [{:id 100 :desc "one "}
                         {:id 100 :desc "two "}]}
          expected-result (list [[:tab 0] :id :dadyspec.core/rel-1-n :tab2 :tab-id]
                                [[:tab 1] :id :dadyspec.core/rel-1-n :tab2 :tab-id]
                                [[:tab 0] :id :dadyspec.core/rel-1-n :tab3 :tab-id]
                                [[:tab 1] :id :dadyspec.core/rel-1-n :tab3 :tab-id])
          actual-result (replace-source-entity-path join data-m)]
      (is (= actual-result
             expected-result)))))

;(replace-source-entity-path-test)


(deftest replace-target-entity-path-test
  (testing "test replace-target-entity-path "
    (let [join [[[:tab] :id :dadyspec.core/rel-1-n :tab2 :tab-id]
                [[:tab] :id :dadyspec.core/rel-1-n :tab3 :tab-id]]
          join-data {:tab {:id   100,
                           :tab2 [{:m "Munich"}]}}
          expected-result (list [[:tab] :id :dadyspec.core/rel-1-n [:tab :tab2 0] :tab-id])
          actual-result (replace-target-entity-path join join-data)]
      (is (= actual-result
             expected-result))))
  (testing "test replace-target-entity-path "
    (let [join [[[:tab] :id :dadyspec.core/rel-1-1 :tab2 :tab-id]]
          join-data {:tab {:id   100,
                           :tab2 {:m "Munich"}}}
          expected-result (list [[:tab] :id :dadyspec.core/rel-1-1 [:tab :tab2] :tab-id])
          actual-result (replace-target-entity-path join join-data)]
      (is (= actual-result
             expected-result)))))


;(assoc-target-entity-key-test)

(deftest group-by-target-entity-one-test
  (testing "test dest-rel-data "
    (let [data {:tab {:id   100
                      :tab1 [{:tab-id 100} {:tab-id 100}]}}
          j [[:tab] :id :dadyspec.core/rel-1-n :tab1 :tab-id]
          expected-result {:tab1 [{:tab-id 100} {:tab-id 100}]}
          actual-result (group-by-target-entity-one data j)]
      (is (= actual-result
             expected-result))))
  (testing "test dest-rel-data with single join "
    (let [data {:tab [{:id   100
                       :tab1 [{:tab-id 100}
                              {:tab-id 100}]}
                      {:id   102
                       :tab1 {:tab-id 138}}]}
          j [[:tab 0] :id :dadyspec.core/rel-n-n [:tab 0 :tab1 0] :tab-id [:ntab :tab-id :tab1-id]]
          expected-result {:ntab [{:tab-id 100, :tab1-id 100}]}
          actual-result (group-by-target-entity-one data j)]
      (is (= actual-result
             expected-result)))))
