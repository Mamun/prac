(ns dadysql.jdbc-test
  (:use [clojure.test])
  (:require [dady.common :refer :all]
            [dadysql.constant :refer :all]
            [dadysql.jdbc :refer :all]))


(deftest has-dml-type?-test
  (testing "test has-dml-type? "
    (let [data {dml-key dml-select-key}]
      (is (not (nil? (has-dml-type? data)))))))


(deftest get-dml-test
  (testing "get-dml test "
    (let [tms {:get-dml     {sql-key ["select * from tab where tab = :tab" :tab]
                             dml-key dml-select-key}
               :create-dual {sql-key ["insert into dual value (a )"]
                             dml-key dml-select-key}}
          actual-result (get-dml tms)
          expected-result ["select * from tab where tab = ?"]]
      (is (= expected-result
             actual-result)))))




(deftest read-file-test

  (testing "test read file "
    (let [w (read-file "tie.edn.sql")]
      (do
        (are [expected actual]
          (= expected actual)
          :gen-dept (get-in w [:gen-dept :model]))

        (are [expected actual]
          (= expected actual)
          ;get-dept-by-id
          :department (get-in w [:get-dept-by-id model-key])
          dml-select-key (get-in w [:get-dept-by-id dml-key])
          3000 (get-in w [:get-dept-by-id timeout-key])
          ["select * from department where id = :id " :id] (get-in w [:get-dept-by-id sql-key])
          [[:id :type java.lang.Long "Id will be Long "]] (get-in w [:get-dept-by-id validation-key])
          [[:department :id :1-n :employee :dept_id]] (get-in w [:get-dept-by-id join-key])

          [[:id :type clojure.lang.PersistentVector "Id will be sequence"]
           [:id :contain java.lang.Long "Id contain will be Long "]]
          (get-in w [:get-dept-by-ids :validation]))

        (are [expected actual]
          (= expected actual)

          [[:id :type clojure.lang.PersistentVector "Id will be sequence"]
           [:id :contain java.lang.Long "Id contain will be Long "]] (get-in w [:delete-dept :validation])

          )

        (are [expected actual]
          (= expected actual)

          ;join test
          [[:employee :id :1-1 :employee-detail :employee_id]
           [:employee
            :id
            :n-n
            :meeting
            :meeting_id
            [:employee-meeting :employee_id :meeting_id]]
           [:employee :dept_id :n-1 :department :id]] (get-in w [:get-dept-employee :join]))

        (are [expected actual]
          (= expected actual)

          :meeting (get-in w [:create-meeting :model])
          [[:meeting
            :meeting_id
            :n-n
            :employee
            :id
            [:employee-meeting :meeting_id :employee_id]]] (get-in w [:get-meeting-by-id :join]))))))




