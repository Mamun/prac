(ns tiesql.core-test
  (:use [clojure.test])
  (:require [tiesql.core :refer :all]
    ;[tie.impl.impl-factory :as fac]
            [tiesql.common :refer :all]
            [tiesql.core-util :as cu]
            ))




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
        (is (= (get-in actual-result [0 sql-key])
               expected-result))
        ))
    )

;(do-input-bind-test)


;(run-tests)