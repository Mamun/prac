(ns ring.middleware.dadysql-test
  (:use [clojure.test])
  (:require [dadysql.core :refer :all]

            [ring.middleware.dadysql :refer :all]
            [test-data :as td]))


(use-fixtures :once (fn [f]
                      (td/get-ds)
                      (td/get-tms)
                      nil
                      (f)))


(defn make-request
  [& {:as m}]
  {:params         m
   :request-method :post
   :content-type   "application/transit+json"})


#_(deftest pull-test
  (testing "pull test "
    (let [res (->> (make-request :name [:get-dept-list])
                   (pull @td/ds @td/tms))]
      (is (= res
             {:status 200, :headers {}, :body [{:department [[:id :transaction_id :dept_name] [1 0 "Business"] [2 0 "Marketing"] [3 0 "HR"]]} nil]}
             )))))




#_(deftest push-test
  (testing "pull test "
    (let [res (->> (make-request :name [:create-dept]
                                 :params {:department {:dept_name "IT"}})
                   (push! @td/ds @td/tms))]
      (is (= res
             {:status 200, :headers {}, :body [{:department [1]} nil]})))))


;(push-test)
