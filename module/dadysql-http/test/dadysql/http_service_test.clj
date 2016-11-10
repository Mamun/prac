(ns dadysql.http-service-test
  (:use [clojure.test])
  (:require #_[dadysql.spec :refer :all]
            [dadysql.clj.common :refer :all]
            [dadysql.http-service :refer :all]
            [test-data :as td]
            [dadysql.jdbc :as tj]))




(use-fixtures :once (fn [f]
                      (td/get-ds)
                      (td/get-tms)
                      nil
                      (f)))





(comment

  (run-tests)

  )

;(run-tests)
;(pull-test)