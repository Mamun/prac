(ns dadysql.http-service-test
  (:use [clojure.test])
  (:require [dadysql.core :refer :all]
            [dady.common :refer :all]
            [dadysql.http-service :refer :all]
            [test-data :as td]
            [dadysql.jdbc :as tj]))




(use-fixtures :once (fn [f]
                      (td/get-ds)
                      (td/get-tms)
                      nil
                      (f)))



#_(deftest pull-test
  (testing "pull test "
    (let [handler (partial tj/pull @td/ds @td/tms)
          res (->> {:params         {:name :get-dept-list}

                    :content-type   "transit"}
                   )
          res ((warp-pull handler) res)]
      (clojure.pprint/pprint res)
      (is (= 1 1

             ))))
  (testing "pull test "
    (let [handler (partial tj/pull @td/ds @td/tms)
          res (->> {:params         {:name :get-dept-list}
                    :request-method :post
                    :content-type   "transit"}
                   )
          res ((warp-pull handler) res)]
      ; (println res)
      (is (= 1 1

             )))))


(comment

  (run-tests)

  )

;(run-tests)
;(pull-test)