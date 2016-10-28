(ns dadysql.jdbc-test
  (:use [clojure.test])
  (:require [dady.common :refer :all]
            [dadysql.jdbc :refer :all]))


(deftest has-dml-type?-test
  (testing "test has-dml-type? "
    (let [data {:dadysql.core/dml :dadysql.core/dml-select}]
      (is (not (nil? (has-dml-type? data)))))))


(deftest get-dml-test
  (testing "get-dml test "
    (let [tms {:get-dml     {:dadysql.core/sql ["select * from tab where tab = :tab" :tab]
                             :dadysql.core/dml :dadysql.core/dml-select}
               :create-dual {:dadysql.core/sql ["insert into dual value (a )"]
                             :dadysql.core/dml :dadysql.core/dml-select}}
          actual-result (get-all-parameter-sql tms)
          expected-result ["select * from tab where tab = ?"]]
      (is (= expected-result
             actual-result)))))

;(get-dml-test)


;(var int?)

#_(clojure.pprint/pprint
  (get-in (read-file "tie.edn.sql") [:get-dept-employee :dadysql.core/join]))


(deftest read-file-test

  (testing "test read file "
    (let [w (read-file "tie.edn.sql")]
     ; (clojure.pprint/pprint (:get-meeting-by-id w))

      (do
        (are [e a] (= e a)
          :gen-dept (get-in w [:gen-dept :dadysql.core/model]))

       ; (clojure.pprint/pprint (get-in w [:get-dept-by-id validation-key]))

        (are [e a]  (= e a)
          ;get-dept-by-id
          :department (get-in w [:get-dept-by-id :dadysql.core/model])
          :dadysql.core/dml-select (get-in w [:get-dept-by-id :dadysql.core/dml])
          2000 (get-in w [:get-dept-by-id :dadysql.core/timeout])
          ;["select * from department where id = :id " :id] (get-in w [:get-dept-by-id :dadysql.core/sql])
       ;   [[:id :type (resolve 'int?) "Id will be Long "]] (get-in w [:get-dept-by-id validation-key])
          [[:department :id :dadysql.core/join-one-many :employee :dept_id]] (get-in w [:get-dept-by-id :dadysql.core/join])

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
          [[:employee :id :dadysql.core/join-one-one :employee-detail :employee_id]
           [:employee
            :id
            :dadysql.core/join-many-many
            :meeting
            :meeting_id
            [:employee-meeting :employee_id :meeting_id]]
           [:employee :dept_id :dadysql.core/join-many-one :department :id]] (get-in w [:get-dept-employee :dadysql.core/join]))

        (are [expected actual]
          (= expected actual)

          :meeting (get-in w [:create-meeting :dadysql.core/model])
          [[:meeting
            :meeting_id
            :dadysql.core/join-many-many
            :employee
            :id
            [:employee-meeting :meeting_id :employee_id]]] (get-in w [:get-meeting-by-id :dadysql.core/join])))))
  )


;(clojure.pprint/pprint (read-file "tie.edn.sql"))

#_(deftest read-file-test2
  (ts))

;(read-file-test)



;((resolve 'int?) "sdf")

(deftest check-test
  (testing "hello "
    (are [e a]
      (= e a)
      [[:id :type (resolve 'int?) "id will be long "]]
      [[:id :type #'clojure.core/int? "id will be long "]])))


;(check-test)

(comment

  (run-tests)
  )