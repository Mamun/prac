(ns dadysql.plugin.sql-bind-impl-test
  (:use [clojure.test]
        [dady.fail]
        [dadysql.compiler.util])
  (:require                                                 ;[dadysql.plugin.sql-bind-impl :refer :all]
    [dadysql.plugin.sql-bind-impl :refer :all]
    ;[dadysql.plugin.sql-util :refer :all]

    [dady.proto :refer :all]
    #_[dadysql.spec :refer :all]))


(comment
  (run-tests)

  )

(deftest bind-sql-params-test
  (testing "test bind-sql-params "
    (let [m {:dadysql.core/sql     ["select * from dual param = :a and param3 = :b" :a :b]
             :dadysql.core/dml-key :dadysql.core/dml-select
             :dadysql.core/param             {:a 3 :b 4}}
          expected-result {:dadysql.core/sql     ["select * from dual param = ? and param3 = ?" 3 4],
                           :dadysql.core/dml-key :dadysql.core/dml-select,
                           :dadysql.core/param                {:a 3, :b 4}}


          actual-result (sql-bind  m)]
      (is (= expected-result
             actual-result
             )))))

;{:dadysql.core/sql ["select * from dual param = ? and param3 = ?" 3 4], :dadysql.core/dml-key :select, :input {:a 3, :b 4}}
;{:dadysql.core/sql ["select * from dual param = ? and param3 = ?" 3 4], :dadysql.core/dml-key :select, :input {:a 3, :b 4}}

;(bind-sql-params-test)

(deftest sql-str-emit-test
  (testing "test sql-emission-single"
    (let [w "select * from dual param = :A and param3 = :b"
          actual-result (sql-str-emit w)
          expected-result ["select * from dual param = :a and param3 = :b" :a :b]]
      ;     (println actual-result)
      (is (= expected-result
             actual-result))))
  (testing "test sql-emission-single"
    (let [w "select * from dual param = :a-b and param3 = :b-c"
          actual-result (sql-str-emit w)
          expected-result ["select * from dual param = :a-b and param3 = :b-c" :a-b :b-c]]
      ;     (println actual-result)
      (is (= expected-result
             actual-result))))
  (testing "test sql-emission-single"
    (let [w "select * from dual param# = :param# and param3 = :b-c"
          actual-result (sql-str-emit w)
          expected-result ["select * from dual param# = :param# and param3 = :b-c" :param# :b-c]]
      ;     (println actual-result)
      (is (= expected-result
             actual-result)))))

;(sql-str-emit-test)


;(re-seq sql-param-regex "param#=:param# and :b-c and :b_c :b|c")


#_(deftest sql-emit-test
    (testing "test sql-emission"
      (let [w "select * from dual param = :a and param3 = :b"
            expected-result [{:dadysql.core/sql     ["select * from dual param = :a and param3 = :b" :a :b],
                              :dadysql.core/dml-key :dadysql.core/dml-select,
                              :dadysql.core/index   0}]
            actual-result (sql-emit w)]
        (is (= expected-result actual-result))))
    (testing "test sql-emission "
      (let [w "select * from dual param = :ID and param3 = :b"
            expected-result [{:dadysql.core/sql     ["select * from dual param = :id and param3 = :b" :id :b],
                              :dadysql.core/dml-key :dadysql.core/dml-select,
                              :dadysql.core/index   0}]
            actual-result (sql-emit w)]
        (is (= expected-result actual-result))))

    (testing "test sql-emission "
      (let [w "insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name);"
            expected-result [{:dadysql.core/sql     ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)" :id :transaction_id :dept_name],
                              :dadysql.core/dml-key :dadysql.core/dml-insert,
                              :dadysql.core/index   0}]
            actual-result (sql-emit w)]
        (is (= expected-result actual-result))))

    )


;(sql-emit-test)



(deftest bind-sql-params-test
  (testing "test bind-sql-params with dml select "
    (let [m {:dadysql.core/sql     ["select * from dual param = :a and param3 = :b" :a :b]
             :dadysql.core/dml-key :dadysql.core/dml-select
             :dadysql.core/param             {:a 3 :b 4}}
          expected-result {:dadysql.core/sql     ["select * from dual param = ? and param3 = ?" 3 4],
                           :dadysql.core/dml-key :dadysql.core/dml-select,
                           :dadysql.core/param                {:a 3, :b 4}}

          actual-result (default-proc m)]
      (is (= expected-result actual-result))))

  (testing "test bind-sql-params with dml insert  "
    (let [m {:dadysql.core/sql     ["insert into  dual values (:a :b)" :a :b]
             :dadysql.core/dml-key :dadysql.core/dml-insert
             :dadysql.core/param             [{:a 3 :b 4}
                                    {:a 5 :b 6}]}
          expected-result {:dadysql.core/sql     ["insert into  dual values (? ?)" [3 4] [5 6]],
                           :dadysql.core/dml-key :dadysql.core/dml-insert,
                           :dadysql.core/param                [{:a 3, :b 4} {:a 5, :b 6}]}
          actual-result (insert-proc m)]
      (is (= expected-result actual-result))))

  (testing "test bind-sql-params with dml update  "
    (let [m {:dadysql.core/sql     ["update dual set :a= :a1, b1 = :b where id=:id" :a1 :b :id]
             :dadysql.core/dml-key :dadysql.core/dml-insert
             :dadysql.core/param             {:a1 3 :b 4 :id 4}}
          expected-result {:dadysql.core/sql     ["update dual set :a= ?, b1 = ? where id=?" [3 4 4]],
                           :dadysql.core/dml-key :dadysql.core/dml-insert,
                           :dadysql.core/param                {:a1 3, :b 4, :id 4}}
          actual-result (insert-proc m)]
      (is (= expected-result actual-result)))))



;(bind-sql-params-test)

;(run-tests)

(comment
  (run-tests)

  )


#_(deftest split-sql-params-test
    (testing "test split-sql-params "
      (let [sql "select * from where a = :a;select * from t where p=:p"
            expect-result [{:dadysql.core/sql ["select * from where a = :a" :a] :dadysql.core/dml-key :dadysql.core/dml-select :dadysql.core/index 0}
                           {:dadysql.core/sql ["select * from t where p=:p" :p] :dadysql.core/dml-key :dadysql.core/dml-select :dadysql.core/index 1}]
            actual-result (sql-emit sql)]
        (is (= actual-result
               expect-result)))))


;(split-sql-params-test)


(deftest dml-type-test
  (testing "test dml-type "
    (let [t "select * from p where "
          actual-result (dml-type t)
          expected-result :dadysql.core/dml-select]
      (is (= actual-result
             expected-result)))))




;(dml-type-test)

(deftest validate-input-type-test
  (testing "test validate-input-type"
    (let [w {:dadysql.core/sql     ["insert into employee_detail (employee_id, street,   city,  state,  country )                     values (:employee_id, :street, :city, :state, :country)"
                                    :employee_id
                                    :street
                                    :city
                                    :state
                                    :country]
             :dadysql.core/dml-key :dadysql.core/dml-insert
             :dadysql.core/param                [{:street      "Schwan",
                                     :city        "Munich",
                                     :state       "Bayern",
                                     :country     "Germany",
                                     :id          126,
                                     :employee_id 125}]}
          r (validate-input-type! w)]
      (is (= r w)))))


;(validate-input-type-test)