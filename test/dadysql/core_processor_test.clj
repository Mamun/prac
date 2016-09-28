(ns dadysql.core-processor-test
  (:use [clojure.test])
  (:require [dadysql.core-selector :refer :all]
            [dady.common :refer :all]
            [dady.fail :refer :all]
            [dadysql.compiler.core :as fr]
            [dady.fail :as f]))



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
                           {:dadysql.core/sql   "select * from dual ",
                            :dadysql.core/model :dual2, :dadysql.core/join []}]
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
    (let [j [[:d-tab :d-id :dadysql.core/one-one :s-tab :s-id]]
          model [:s-tab]
          actual-result (filter-join-key-coll j model)
          expected-result [[:d-tab :d-id :dadysql.core/one-one :s-tab :s-id]]]
      (is (= actual-result
             expected-result))))
  (testing "test filter-join-key "
    (let [j [[:d-tab :d-id :dadysql.core/one-one :s-tab :s-id]]
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



(deftest select-name-test
  (testing "test select-name"
    (let [m {:name :get-dept-by-id}
          r (select-name (fr/read-file "tie.edn.sql") m)]
      (is (= 1 (count r)))
      (is (not (failed? r))))))



(deftest default-request-test
  (testing "test default-request"
    (are [a e]
      (= a e)
      (assoc-format :pull {:name :get-dept-by-id}) {:dadysql.core/input-format :dadysql.core/format-map, :dadysql.core/output-format :one, :name :get-dept-by-id}
      (assoc-format :pull {:name [:get-dept-by-id]}) {:dadysql.core/input-format :dadysql.core/format-map, :dadysql.core/output-format :dadysql.core/format-nested-join, :name [:get-dept-by-id]}
      (assoc-format :push {:name :get-dept-by-id}) {:dadysql.core/input-format :dadysql.core/format-map, :dadysql.core/output-format :one, :name :get-dept-by-id}
      (assoc-format :push {:name [:get-dept-by-id]}) {:dadysql.core/input-format :dadysql.core/format-nested, :dadysql.core/output-format :dadysql.core/format-nested, :name [:get-dept-by-id]}
      (assoc-format :db-seq {:name :get-dept-by-id}) {:dadysql.core/input-format :dadysql.core/format-map, :dadysql.core/output-format :dadysql.core/format-value, :name :get-dept-by-id})))


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