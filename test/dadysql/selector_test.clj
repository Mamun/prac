(ns dadysql.selector-test
  (:use [clojure.test])
  (:require [dadysql.selector :refer :all]
            [dadysql.clj.common :refer :all]
            [dadysql.clj.fail :refer :all]
            [dadysql.compiler.core :as fr]
            [dadysql.clj.fail :as f]))



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
                 :spec-model.core/join  [[:dual :id :1-n :dual2 :tab-id]
                                      [:dual :id :1-n :tab3 :tab-id]]}
                {:dadysql.core/sql   "select * from dual "
                 :dadysql.core/model :dual2
                 :spec-model.core/join  [[:dual :id :1-n :tab2 :tab-id]
                                      [:dual :id :1-n :tab3 :tab-id]]}]
          expected-result [{:dadysql.core/sql   "select * from dual ",
                            :dadysql.core/model :dual,
                            :spec-model.core/join  [[:dual :id :1-n :dual2 :tab-id]]}
                           {:dadysql.core/sql   "select * from dual ",
                            :dadysql.core/model :dual2, :spec-model.core/join []}]
          actual-result (filter-join-key data)]
      (is (= expected-result
             actual-result)))))


;(filter-join-key-test)


(deftest is-reserve?-test
  (testing "test is-reserve? "
    (let [data {:_global_ {:dadysql.core/reserve-name #{:a :b}}}]
      (is (is-reserve? data [:a]))))
  (testing "test is-reserve?  "
    (let [data {:_global_ {:dadysql.core/reserve-name #{:a :b}}}]
      (is (not (is-reserve? data [:c]))))))

;(is-reserve?-test)

(deftest filter-join-key-coll-test
  (testing "test filter-join-key-coll"
    (let [j [[:d-tab :d-id :spec-model.core/rel-1-1 :s-tab :s-id]]
          model [:s-tab]
          actual-result (filter-join-key-coll j model)
          expected-result [[:d-tab :d-id :spec-model.core/rel-1-1 :s-tab :s-id]]]
      (is (= actual-result
             expected-result))))
  (testing "test filter-join-key "
    (let [j [[:d-tab :d-id :spec-model.core/rel-1-1 :s-tab :s-id]]
          model [:s-tab2]
          actual-result (filter-join-key-coll j model)
          expected-result []]
      (is (= actual-result
             expected-result)))))


;(has-dml-type?-test)


#_(deftest validate-input!-test
  (testing "test validate-input! "
    (let [w {:name :get-dept-by-id}]
      (is (= (validate-input! w) w))))
  (testing "test validate-input! "
    (let [w {:name [:get-dept-by-id]}]
      (is (= (validate-input! w) w))))
  (testing "test validate-input! "
    (let [w {:name  [:get-dept-by-id]
             :group :load-dept}]
      (is (= (validate-input! w) w))))
  (testing "test validate-input! "
    (let [w {:name  [:get-dept-by-id]
             :group [:load-dept]}
          _ (failed? (validate-input! w))
          ]


      )))








#_(deftest commit?-test
    (testing "test commit?"
      (is (commit? commit-all-key false [{:a 4}]))
      (is (not (commit? commit-all-key false [(cc/fail "failed ")])))))

;(commit?-test)

(comment

  (default-request-test)

  (validate-input!-test)

  (select-name-test)

  (run-tests)
  )
;