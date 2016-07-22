(ns dadysql.core-util-test
  (:use [clojure.test]
        [dadysql.core-util])
  (:require [dadysql.core :refer :all]
            [dady.common :refer :all]
            [dady.fail :refer :all]
            ))

(deftest get-path-test
  (testing "test get-path "
    (let [d {:a {:b [{:b 4}]}}
          expected-result [[:a]]
          acutal-result (get-path d :a)]
      (is (= acutal-result expected-result))))
  (testing "test get-path "
    (let [d {:a [{:b 4}]}
          expected-result [[:a 0 :b]]
          acutal-result (get-path d [[:a 0]] :b)]
      (is (= expected-result acutal-result))))
  (testing "test get-path "
    (let [d {:a [{:b 4}
                 {:c 8}]}
          expected-result [[:a 0] [:a 1]]
          acutal-result (get-path d :a)]
      (is (= expected-result acutal-result)))))




(deftest validate-name!-test
  (testing "test validate-name! with success "
    (let [data {:get-dual  {sql-key "select * from dual "}
                :get-dual2 {sql-key "select * from dual "}}
          actual-result (validate-name! data [:get-dual :get-dual2])]
      (is (not (failed? actual-result)))))
  (testing "test validate-name! with fail "
    (let [data {:get-dual  {:sql "select * from dual "}
                :get-dual2 {:sql "select * from dual "}}
          actual-result (validate-name! data [:get-dual :get-dual4])]
      (is (failed? actual-result)))))


;(validate-name!-test)


(deftest validate-model!-test
  (testing "test validate-model! with success "
    (let [data [{sql-key   "select * from dual "
                 model-key :DUEL}
                {sql-key   "select * from dual "
                 model-key :DUEL2}]
          acutal-result (validate-model! data)]
      (is (not (failed? acutal-result)))))
  (testing "test validate-model! with success "
    (let [data [{sql-key   "select * from dual "
                 model-key :DUEL}
                {sql-key   "select * from dual "
                 model-key :DUEL}]
          acutal-result (validate-model! data)]
      (is (failed? acutal-result)))))


;(validate-model!-test)


(deftest filter-join-key-test
  (testing "test filter-join-key "
    (let [data [{sql-key   "select * from dual "
                 model-key :dual
                 join-key  [[:dual :id :1-n :dual2 :tab-id]
                            [:dual :id :1-n :tab3 :tab-id]]}
                {sql-key   "select * from dual "
                 model-key :dual2
                 join-key  [[:dual :id :1-n :tab2 :tab-id]
                            [:dual :id :1-n :tab3 :tab-id]]}]
          expected-result [{:sql   "select * from dual ",
                            :model :dual,
                            :join  [[:dual :id :1-n :dual2 :tab-id]]}
                           {:sql "select * from dual ", :model :dual2, :join []}]
          actual-result (filter-join-key data)]
      (is (= expected-result
             actual-result)))))


;(filter-join-key-test)


(deftest is-reserve?-test
  (testing "test is-reserve? "
    (let [data {global-key {reserve-name-key #{:a :b}}}]
      (is (is-reserve? data [:a]))))
  (testing "test is-reserve?  "
    (let [data {global-key {reserve-name-key #{:a :b}}}]
      (is (not (is-reserve? data [:c]))))))

;(is-reserve?-test)




;(has-dml-type?-test)




#_(deftest commit?-test
    (testing "test commit?"
      (is (commit? commit-all-key false [{:a 4}]))
      (is (not (commit? commit-all-key false [(cc/fail "failed ")])))))

;(commit?-test)

;(run-tests)