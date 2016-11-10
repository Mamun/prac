(ns dadysql.impl.sql-bind-impl-test
  (:use [clojure.test]
        [dadysql.clj.fail]
        [dadysql.compiler.validation])
  (:require                                                 ;[dadysql.impl.sql-bind-impl :refer :all]
    [dadysql.impl.sql-bind-impl :refer :all]
    ))


(comment
  (run-tests)

  )

(deftest bind-sql-params-test
  (testing "test bind-sql-params "
    (let [m {:dadysql.core/sql   ["select * from dual param = :a and param3 = :b" :a :b]
             :dadysql.core/dml   :dadysql.core/dml-select
             :dadysql.core/param {:a 3 :b 4}}
          expected-result {:dadysql.core/sql   ["select * from dual param = ? and param3 = ?" 3 4],
                           :dadysql.core/dml   :dadysql.core/dml-select,
                           :dadysql.core/param {:a 3, :b 4}}


          actual-result (sql-bind m)]
      (is (= expected-result
             actual-result
             )))))

;{:dadysql.core/sql ["select * from dual param = ? and param3 = ?" 3 4], :dadysql.core/dml :select, :input {:a 3, :b 4}}
;{:dadysql.core/sql ["select * from dual param = ? and param3 = ?" 3 4], :dadysql.core/dml :select, :input {:a 3, :b 4}}

;(bind-sql-params-test)


;(sql-str-emit-test)


;(re-seq sql-param-regex "param#=:param# and :b-c and :b_c :b|c")


#_(deftest sql-emit-test
    (testing "test sql-emission"
      (let [w "select * from dual param = :a and param3 = :b"
            expected-result [{:dadysql.core/sql   ["select * from dual param = :a and param3 = :b" :a :b],
                              :dadysql.core/dml   :dadysql.core/dml-select,
                              :dadysql.core/index 0}]
            actual-result (sql-emit w)]
        (is (= expected-result actual-result))))
    (testing "test sql-emission "
      (let [w "select * from dual param = :ID and param3 = :b"
            expected-result [{:dadysql.core/sql   ["select * from dual param = :id and param3 = :b" :id :b],
                              :dadysql.core/dml   :dadysql.core/dml-select,
                              :dadysql.core/index 0}]
            actual-result (sql-emit w)]
        (is (= expected-result actual-result))))

    (testing "test sql-emission "
      (let [w "insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name);"
            expected-result [{:dadysql.core/sql   ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)" :id :transaction_id :dept_name],
                              :dadysql.core/dml   :dadysql.core/dml-insert,
                              :dadysql.core/index 0}]
            actual-result (sql-emit w)]
        (is (= expected-result actual-result))))

    )


;(sql-emit-test)



(deftest bind-sql-params-test
  (testing "test bind-sql-params with dml select "
    (let [m {:dadysql.core/sql   ["select * from dual param = :a and param3 = :b" :a :b]
             :dadysql.core/dml   :dadysql.core/dml-select
             :dadysql.core/param {:a 3 :b 4}}
          expected-result {:dadysql.core/sql   ["select * from dual param = ? and param3 = ?" 3 4],
                           :dadysql.core/dml   :dadysql.core/dml-select,
                           :dadysql.core/param {:a 3, :b 4}}

          actual-result (default-proc m)]
      (is (= expected-result actual-result))))

  (testing "test bind-sql-params with dml insert  "
    (let [m {:dadysql.core/sql   ["insert into  dual values (:a :b)" :a :b]
             :dadysql.core/dml   :dadysql.core/dml-insert
             :dadysql.core/param [{:a 3 :b 4}
                                  {:a 5 :b 6}]}
          expected-result {:dadysql.core/sql   ["insert into  dual values (? ?)" [3 4] [5 6]],
                           :dadysql.core/dml   :dadysql.core/dml-insert,
                           :dadysql.core/param [{:a 3, :b 4} {:a 5, :b 6}]}
          actual-result (insert-proc m)]
      (is (= expected-result actual-result))))

  (testing "test bind-sql-params with dml update  "
    (let [m {:dadysql.core/sql   ["update dual set :a= :a1, b1 = :b where id=:id" :a1 :b :id]
             :dadysql.core/dml   :dadysql.core/dml-insert
             :dadysql.core/param {:a1 3 :b 4 :id 4}}
          expected-result {:dadysql.core/sql   ["update dual set :a= ?, b1 = ? where id=?" [3 4 4]],
                           :dadysql.core/dml   :dadysql.core/dml-insert,
                           :dadysql.core/param {:a1 3, :b 4, :id 4}}
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
            expect-result [{:dadysql.core/sql ["select * from where a = :a" :a] :dadysql.core/dml :dadysql.core/dml-select :dadysql.core/index 0}
                           {:dadysql.core/sql ["select * from t where p=:p" :p] :dadysql.core/dml :dadysql.core/dml-select :dadysql.core/index 1}]
            actual-result (sql-emit sql)]
        (is (= actual-result
               expect-result)))))


;(split-sql-params-test)






;(dml-type-test)

(deftest validate-input-type-test
  (testing "test validate-input-type"
    (let [w {:dadysql.core/sql   ["insert into employee_detail (employee_id, street,   city,  state,  country )                     values (:employee_id, :street, :city, :state, :country)"
                                  :employee_id
                                  :street
                                  :city
                                  :state
                                  :country]
             :dadysql.core/dml   :dadysql.core/dml-insert
             :dadysql.core/param [{:street      "Schwan",
                                   :city        "Munich",
                                   :state       "Bayern",
                                   :country     "Germany",
                                   :id          126,
                                   :employee_id 125}]}
          r (validate-input-type! w)]
      (is (= r w)))))


;(validate-input-type-test)