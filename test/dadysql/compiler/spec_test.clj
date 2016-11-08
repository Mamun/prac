(ns dadysql.compiler.spec-test
  (:use [clojure.test]
        [dadysql.compiler.test-data]
        [dadysql.compiler.core])
  (:require [clojure.spec :as s]
            [dadysql.spec]
            [clojure.spec.gen :as gen]
            [clojure.spec.test :as t]
            #_[dadysql.compiler.file-reader :as f]))

;(tx-prop-spec-test)



(deftest unit-spec-test
  (testing "test params spec "
    (is (s/valid? :dadysql.core/tx-prop [:isolation :serializable :read-only? true]))
    (is (false? (s/valid? :dadysql.core/tx-prop [:isolation :serializable :read-only? 1])) )
    (is (false? (s/valid? :dadysql.core/tx-prop [:isolation :serializable1 :read-only? true])) )
    (is (s/valid? :dadysql.core/default-param [:next_transaction_id '(inc :transaction_id)]) )
    (is (s/valid? :dadysql.core/default-param [:next_transaction_id :id ]))
    (is (s/valid? :dadysql.core/join [[:department :id :dadysql.core/join-one-many :employee :dept_id]
                                       [:employee :id :dadysql.core/join-one-one :employee-detail :employee_id]
                                       [:employee :id :dadysql.core/join-many-many :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]))
    (is (false? (s/valid? :dadysql.core/join  [[:department :id :dadysql.core/join-one-many :employee :dept_id]
                                               [:employee :id :dadysql.core/join-one-one :employee-detail :employee_id]
                                               [:employee :id :dadysql.core/join-many-many :meeting :meeting_id [:employee-meeting :employee_id]]]))
        )
    ))




(deftest spec-test
  (testing "test do-compile "
    (let [r (s/conform :dadysql.core/compiler-spec do-compile-input-data)]
      (is (not= :clojure.spec/invalid r)))))


;(spec-test)

(comment


  (run-tests)
  ;(s/explain-str :dadysql.core/compiler-spec do-compile-input-data)

  ;(s/conform :tie-edn/get-dept-by-id {:id "asdf"})


  )


