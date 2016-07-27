(ns dadysql.plugin.sql.sql-executor-test
  (:use [clojure.test]
        [dady.fail]
        [dadysql.plugin.sql.sql-executor])
  (:require [dadysql.spec :refer :all]
            [dady.common :as cc]))


#_(deftest warp-map-output-test
  (testing "test warp-map-output"
    (let [handler (fn [m] (throw (Exception. "Some text")))
          hand (-> handler
                   (warp-map-output))
          input {sql-key "select * from dual"}
          reslt (hand input)]
      ;(println reslt)
      ;(println (cc/-failed? reslt ))
      (is (failed? reslt)))))



;(warp-map-output-test)

(deftest warp-parallel-coll-test
  (testing "test warp-parallel-coll"
    (let [handler (fn [m] (assoc m output-key {:a 3}))
          han (-> handler
                  (warp-map-output)
                  (warp-async-go))
          m-coll [{:dadysql.spec/sql "select * from dual"}
                  {:dadysql.spec/sql "select * from dual1 "}
                  {:dadysql.spec/sql "select * from dual1 "}]
          result (apply-handler-parallel han m-coll)]
      (is (not (empty? result)))))
  (testing "test warp-parallel-coll"
    (let [handler (fn [m]
                    (do
                      (when (= :b (:dadysql.spec/name m))
                        (Thread/sleep 3000))
                      (assoc m output-key {:a 3})))
          han (-> handler
                  (warp-map-output)
                  (warp-async-go))
          m-coll [{:dadysql.spec/sql  "select * from dual"
                   :dadysql.spec/name :a}
                  {:dadysql.spec/sql  "select * from dual1 "
                   :dadysql.spec/name :b}
                  {:dadysql.spec/sql  "select * from dual1 "
                   :dadysql.spec/name :c}]
          result (apply-handler-parallel han m-coll)]
      (is (failed? (get-in result [2]))))))


;(warp-parallel-coll-test)

(deftest executor-test
  (testing "test warp-until-fail-coll"
    (let [handler (fn [m] (throw (Exception. "Some text")))
          hand (-> handler
                   (warp-map-output)
                   (warp-async-go))
          coll [{:dadysql.spec/sql  "select * from dual"
                 :dadysql.spec/name :a}
                {:dadysql.spec/sql  "select * from dual1 "
                 :dadysql.spec/name :b}
                {:dadysql.spec/sql  "select * from dual1 "
                 :dadysql.spec/name :c}]
          ;actual-result (apply-handler-until-fail hand coll)
          ]
      ;(clojure.pprint/pprint actual-result)
      (is (failed? (fail "df"))))))


;(executor-test)


;(run-tests)



(deftest commit-type-test
  (testing "test commit-type with one dml type "
    (let [data [{:dadysql.spec/dml-key     :dadysql.spec/dml-select
                 :dadysql.spec/commit :dadysql.spec/all}]
          actual-result (commit-type data)]
      (is (= actual-result
             :dadysql.spec/none))))
  (testing "test commit-type with all "
    (let [data [{commit-type :dadysql.spec/all}]
          actual-result (commit-type data)]
      (is (= actual-result
             :dadysql.spec/all))
      ))
  (testing "test commit-type with all "
    (let [data [{commit-type :dadysql.spec/all}]
          actual-result (commit-type data)]
      (is (= actual-result
             :dadysql.spec/all))
      ))
  (testing "test commit-type with all "
    (let [data [{:dadysql.spec/commit :dadysql.spec/all}
                {:dadysql.spec/commit :dadysql.spec/none}]
          actual-result (commit-type data)]
      (is (= actual-result
             :dadysql.spec/none)))))
