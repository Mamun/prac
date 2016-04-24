(ns tiesql.http-service-test
  (:use [clojure.test])
  (:require [tiesql.common :refer :all]
            [tiesql.http-service :refer :all]
            [test-data :as td]))




(use-fixtures :once (fn [f]
                      (td/get-ds)
                      (td/get-tms)
                      nil
                      (f)))



(deftest pull-test
  (testing "pull test "
    (let [res (->> {:params         {:name :get-dept-list}

                    :content-type   "transit"}
                   (pull @td/ds @td/tms))]
      (clojure.pprint/pprint res)
      (is (= 1 1

             ))))
  (testing "pull test "
    (let [res (->> {:params         {:name :get-dept-list}
                    :request-method :post
                    :content-type   "transit"}
                   (pull @td/ds @td/tms))]
      ; (println res)
      (is (= 1 1

             )))))

(comment
  (run-tests)

  )

;(run-tests)
;(pull-test)