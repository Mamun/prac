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
    (let [data {:get-dual  {:dadysql.core/sql "select * from dual "}
                :get-dual2 {:dadysql.core/sql "select * from dual "}}
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
    (let [data [{:dadysql.core/sql   "select * from dual "
                 :dadysql.core/model :DUEL}
                {:dadysql.core/sql   "select * from dual "
                 :dadysql.core/model :DUEL2}]
          acutal-result (validate-model! data)]
      (is (not (failed? acutal-result)))))
  (testing "test validate-model! with success "
    (let [data [{:dadysql.core/sql   "select * from dual "
                 :dadysql.core/model :DUEL}
                {:dadysql.core/sql   "select * from dual "
                 :dadysql.core/model :DUEL}]
          acutal-result (validate-model! data)]
      (is (failed? acutal-result)))))


;(validate-model!-test)


(deftest filter-join-key-test
  (testing "test filter-join-key "
    (let [data [{:dadysql.core/sql   "select * from dual "
                 :dadysql.core/model :dual
                 :dadysql.core/join  [[:dual :id :1-n :dual2 :tab-id]
                            [:dual :id :1-n :tab3 :tab-id]]}
                {:dadysql.core/sql   "select * from dual "
                 :dadysql.core/model :dual2
                 :dadysql.core/join  [[:dual :id :1-n :tab2 :tab-id]
                            [:dual :id :1-n :tab3 :tab-id]]}]
          expected-result [{:dadysql.core/sql   "select * from dual ",
                            :dadysql.core/model :dual,
                            :dadysql.core/join  [[:dual :id :1-n :dual2 :tab-id]]}
                           {:dadysql.core/sql "select * from dual ",
                            :dadysql.core/model :dual2, :dadysql.core/join []}]
          actual-result (filter-join-key data)]
      (is (= expected-result
             actual-result)))))


;(filter-join-key-test)


(deftest is-reserve?-test
  (testing "test is-reserve? "
    (let [data {global-key {:dadysql.core/reserve-name #{:a :b}}}]
      (is (is-reserve? data [:a]))))
  (testing "test is-reserve?  "
    (let [data {global-key {:dadysql.core/reserve-name #{:a :b}}}]
      (is (not (is-reserve? data [:c]))))))

;(is-reserve?-test)




;(has-dml-type?-test)




#_(deftest commit?-test
    (testing "test commit?"
      (is (commit? commit-all-key false [{:a 4}]))
      (is (not (commit? commit-all-key false [(cc/fail "failed ")])))))

;(commit?-test)

(comment

  (run-tests)
  )
;