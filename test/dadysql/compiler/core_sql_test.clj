(ns dadysql.compiler.core-sql-test
  (:use [clojure.test]
        [dadysql.compiler.core-sql]))

(comment

  (run-tests)
  )

(deftest map-name-model-sql-test
  (testing "test name map sql "
    (let [w {:dadysql.core/name  [:gen-dept :gen-empl :gen-meet],
             :dadysql.core/model [:dept :empl :meet]
             :dadysql.core/sql
                    ["call next value for seq_dept"
                     "call next value for seq_empl"
                     "call next value for seq_meet"]}
          actual-result (map-sql-with-name-model w)]
      (is (not-empty actual-result))))

  (testing "test name map sql "
    (let [w {:dadysql.core/name [:gen-dept :gen-empl :gen-meet],
             :dadysql.core/sql
                   ["call next value for seq_dept"
                    "call next value for seq_empl"
                    "call next value for seq_meet"]}
          actual-result (map-sql-with-name-model w)]
   ;   (clojure.pprint/pprint actual-result)
      (is (not-empty actual-result))))

  (testing "test name map sql "
    (let [w {:dadysql.core/name :gen-dept,
             :dadysql.core/sql  ["select * from dual "]}
          actual-result (map-sql-with-name-model w)]


      (is (not-empty actual-result)))))


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


(deftest dml-type-test
  (testing "test dml-type "
    (let [t "select * from p where "
          actual-result (dml-type t)
          expected-result :dadysql.core/dml-select]
      (is (= actual-result
             expected-result)))))


(comment

  (map-name-model-sql-test)

  )