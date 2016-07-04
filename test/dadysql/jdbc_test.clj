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


;(var int?)

(deftest read-file-test

  (testing "test read file "
    (let [w (read-file "tie.edn.sql")]
      (do
        (are [e a] (= e a)
          :gen-dept (get-in w [:gen-dept :model]))

       ; (clojure.pprint/pprint (get-in w [:get-dept-by-id validation-key]))

        (are [e a]  (= e a)
          ;get-dept-by-id
          :department (get-in w [:get-dept-by-id model-key])
          dml-select-key (get-in w [:get-dept-by-id dml-key])
          3000 (get-in w [:get-dept-by-id timeout-key])
          ;["select * from department where id = :id " :id] (get-in w [:get-dept-by-id sql-key])
       ;   [[:id :type (resolve 'int?) "Id will be Long "]] (get-in w [:get-dept-by-id validation-key])
          [[:department :id :1-n :employee :dept_id]] (get-in w [:get-dept-by-id join-key])

          #_[[:id :type #'clojure.core/vector? "Id will be sequence"]
           [:id :contain #'clojure.core/int? "Id contain will be Long "]]
          ;M(get-in w [:get-dept-by-ids :validation])
                    )



        #_(are [expected actual]
          (= expected actual)

          [[:id :type #'clojure.core/vector? "Id will be sequence"]
           [:id :contain #'clojure.core/int? "Id contain will be Long "]] (get-in w [:delete-dept :validation])

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


#_(deftest read-file-test2
  (ts))

;(read-file-test)

;(run-tests)

;((resolve 'int?) "sdf")

(deftest check-test
  (testing "hello "
    (are [e a]
      (= e a)
      [[:id :type (resolve 'int?) "id will be long "]]
      [[:id :type #'clojure.core/int? "id will be long "]])))


;(check-test)