(ns dadysql.plugin.sql.bind-test
  (:use [clojure.test]
        [dady.fail])
  (:require                                                 ;[dadysql.plugin.sql-bind-impl :refer :all]
    [dadysql.plugin.sql.bind-impl :refer :all]
    ;[dadysql.plugin.sql-util :refer :all]
    [dady.proto :refer :all]
    [dadysql.core :refer :all]))


(deftest bind-sql-params-test
  (testing "test bind-sql-params "
    (let [m {:dadysql.core/sql   ["select * from dual param = :a and param3 = :b" :a :b]
             :dadysql.core/dml-key   dml-select-key
             input-key {:a 3 :b 4}}
          expected-result {:dadysql.core/sql      ["select * from dual param = ? and param3 = ?" 3 4],
                           :dadysql.core/dml-key :select,
                           :input    {:a 3, :b 4}}

          proc (new-sql-key 0 (new-childs-key))
          actual-result (node-process proc m)]
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


(deftest sql-emit-test
  (testing "test sql-emission"
    (let [w "select * from dual param = :a and param3 = :b"
          expected-result [{:dadysql.core/sql      ["select * from dual param = :a and param3 = :b" :a :b],
                            :dadysql.core/dml-key :select,
                            :dadysql.core/index    0}]
          actual-result (sql-emit w)]
      (is (= expected-result actual-result))))
  (testing "test sql-emission "
    (let [w "select * from dual param = :ID and param3 = :b"
          expected-result [{:dadysql.core/sql      ["select * from dual param = :id and param3 = :b" :id :b],
                            :dadysql.core/dml-key :select,
                            :dadysql.core/index    0}]
          actual-result (sql-emit w)]
      (is (= expected-result actual-result)))))


;(sql-emit-test)



(deftest bind-sql-params-test
  (testing "test bind-sql-params with dml select "
    (let [m {:dadysql.core/sql   ["select * from dual param = :a and param3 = :b" :a :b]
             :dadysql.core/dml-key   dml-select-key
             input-key {:a 3 :b 4}}
          expected-result {:dadysql.core/sql      ["select * from dual param = ? and param3 = ?" 3 4],
                           :dadysql.core/dml-key :select,
                           :input    {:a 3, :b 4}}

          actual-result (default-proc m)]
      (is (= expected-result actual-result))))

  (testing "test bind-sql-params with dml insert  "
    (let [m {:dadysql.core/sql   ["insert into  dual values (:a :b)" :a :b]
             :dadysql.core/dml-key   dml-insert-key
             input-key [{:a 3 :b 4}
                        {:a 5 :b 6}]}
          expected-result {:dadysql.core/sql      ["insert into  dual values (? ?)" [3 4] [5 6]],
                           :dadysql.core/dml-key :insert,
                           :input    [{:a 3, :b 4} {:a 5, :b 6}]}
          actual-result (insert-proc m)]
      (is (= expected-result actual-result))))

  (testing "test bind-sql-params with dml update  "
    (let [m {:dadysql.core/sql   ["update dual set :a= :a1, b1 = :b where id=:id" :a1 :b :id]
             :dadysql.core/dml-key   dml-insert-key
             input-key {:a1 3 :b 4 :id 4}}
          expected-result {:dadysql.core/sql      ["update dual set :a= ?, b1 = ? where id=?" [3 4 4]],
                           :dadysql.core/dml-key :insert,
                           :input    {:a1 3, :b 4, :id 4}}
          actual-result (insert-proc m)]
      (is (= expected-result actual-result)))))



;(bind-sql-params-test)

;(run-tests)

(comment
  (run-tests)

  )


(deftest split-sql-params-test
  (testing "test split-sql-params "
    (let [sql "select * from where a = :a;seelct * from t where p=:p"
          expect-result [{:dadysql.core/sql ["select * from where a = :a" :a] :dadysql.core/dml-key :select :dadysql.core/index 0}
                         {:dadysql.core/sql ["seelct * from t where p=:p" :p] :dadysql.core/dml-key :seelct :dadysql.core/index 1}]
          actual-result (sql-emit sql)]
      (is (= actual-result
             expect-result)))))


;(split-sql-params-test)


(deftest dml-type-test
  (testing "test dml-type "
    (let [t ["select * from p where "]
          actual-result (dml-type t)
          expected-result :select]
      (is (= actual-result
             expected-result)))))

;(dml-type-test)

