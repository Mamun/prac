(ns dadysql.jdbc-core-test
  (:use [clojure.test])
  (:require [dadysql.jdbc-core :refer :all]
            [dadysql.plugin.factory :refer :all]
            [dadysql.plugin.params.core :as p]
            [dadysql.plugin.util :as cu]))


(deftest validate-param-spec!-test
  (testing "testing apply validation "
    (let [input [{:dadysql.core/name        :get-dept-by-id,
                  :dadysql.core/sql         ["select * from department where id = ?" 1],
                  :dadysql.core/model       :department,
                  :dadysql.core/result      #{:single},
                  :dadysql.core/param-spec  :get-dept-by-id/spec,
                  :dadysql.core/timeout     2000,
                  :dadysql.core/dml-key     :dadysql.core/dml-select,
                  :dadysql.core/join        [],
                  :dadysql.core/group       :load-dept,
                  :dadysql.core/index       0,
                  :dadysql.core/input {:id 1}}]
          r (validate-param-spec! input)]
      (is (= r input)))))



(deftest do-param-test
  (testing "do param test"
    (let [tm-coll [{:dadysql.core/name       :get-dept-by-id,
                    :dadysql.core/sql        ["select * from department where id = ?" 1],
                    :dadysql.core/model      :department,
                    :dadysql.core/result     #{:single},
                    :dadysql.core/param-spec :get-dept-by-id/spec,
                    :dadysql.core/timeout    2000,
                    :dadysql.core/dml-key    :dadysql.core/dml-select,
                    :dadysql.core/join       [],
                    :dadysql.core/group      :load-dept,
                    :dadysql.core/index      0}]
          r (do-param tm-coll [(p/new-param-key 4 (p/new-child-keys))] {:dadysql.core/input-format :dadysql.core/format-map
                                                                        :params  {:id 4}})]
      (is (= (get-in r [0 :dadysql.core/input]) {:id 4})))))



(comment

  (do-param-test)
  )

;(apply-validation-test!)


#_(deftest do-output-bind-test
    (testing "test do-output-bind "
      (let [pc (comp-child-key (new-process-context-impl) true)
            r (-> (fail "NOt found")
                  (assoc model-key :check))
            coll [r]
            actual-result (warp-post-exec coll pc)]
        (is (-failed? (:check actual-result))))))

;(do-output-bind-test)






#_(deftest do-input-bind-test
    (testing "test do-input-bind  "
      (let [coll [{:group      :load-employee,
                   :index      0,
                   :name       :get-employee-by-id,
                   :params     [],
                   :sql        ["select * from employee where id = :id" :id],
                   :result     #{:single},
                   :timeout    1000,
                   :validation [[:id :type Long "Id will be Long"]],
                   :dml-type   :select,
                   :join       [],
                   :model      :employee}]

            expected-result ["select * from employee where id = ?" 3]
            actual-result (warp-pre-exec
                            coll
                            (comp-child-key (new-process-context-impl) true)
                            map-format
                            {:id 3})]

        (is (= (get-in actual-result [0 :sql])
               expected-result))))
    (testing "test do-input-bind for delete o/p "
      (let [coll [{:group    :modify-dept,
                   :index    2,
                   :name     :delete-dept,
                   :sql      [" delete from department where id in (:id)" :id],
                   :commit   :all,
                   :timeout  1000,
                   :validation
                             [[:id :type (type (read-string "[]")) "Id will be sequence"]
                              [:id :contain Long "Id contain will be Long "]],
                   :dml-type :delete,
                   :join     [],
                   :model    :department}]
            pc (new-process-context-impl)
            ;(merge (cu/s-pprint 6) )
            ;pc (dissoc (new-process-context-impl) validation-key)
            expected-result [" delete from department where id in (?)" 107]
            actual-result (warp-pre-exec
                            coll
                            (comp-child-key pc true)
                            model-key
                            {:department {:id [107]}})]
        ;  (clojure.pprint/pprint pc)
        ;(clojure.pprint/pprint actual-result)
        (is (= (get-in actual-result [0 :dadysql.core/sql])
               expected-result))
        ))
    )

;(do-input-bind-test)


;(run-tests)