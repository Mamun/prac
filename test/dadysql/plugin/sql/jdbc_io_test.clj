(ns dadysql.plugin.sql.jdbc-io-test
  (:use [clojure.test]
        [dady.fail]
        [dadysql.plugin.sql.jdbc-io])
  (:require #_[dadysql.spec :refer :all]
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

#_(deftest warp-parallel-coll-test
  (testing "test warp-parallel-coll"
    (let [handler (fn [m] (assoc m :dadysql.core/output {:a 3}))
          han (-> handler
                  (warp-map-output)
                  (warp-async-go))
          m-coll [{:dadysql.core/sql "select * from dual"}
                  {:dadysql.core/sql "select * from dual1 "}
                  {:dadysql.core/sql "select * from dual1 "}]
          result (apply-handler-parallel han m-coll)]
      (is (not (empty? result)))))
  (testing "test warp-parallel-coll"
    (let [handler (fn [m]
                    (do
                      (when (= :b (:dadysql.core/name m))
                        (Thread/sleep 3000))
                      (assoc m :dadysql.core/output {:a 3})))
          han (-> handler
                  (warp-map-output)
                  (warp-async-go))
          m-coll [{:dadysql.core/sql  "select * from dual"
                   :dadysql.core/name :a}
                  {:dadysql.core/sql  "select * from dual1 "
                   :dadysql.core/name :b}
                  {:dadysql.core/sql  "select * from dual1 "
                   :dadysql.core/name :c}]
          result (apply-handler-parallel han m-coll)]
      (is (failed? (get-in result [2]))))))


;(warp-parallel-coll-test)

(deftest executor-test
  (testing "test warp-until-fail-coll"
    (let [handler (fn [m] (throw (Exception. "Some text")))
          hand (-> handler
                   (warp-map-output)
                   (warp-async-go))
          coll [{:dadysql.core/sql  "select * from dual"
                 :dadysql.core/name :a}
                {:dadysql.core/sql  "select * from dual1 "
                 :dadysql.core/name :b}
                {:dadysql.core/sql  "select * from dual1 "
                 :dadysql.core/name :c}]
          ;actual-result (apply-handler-until-fail hand coll)
          ]
      ;(clojure.pprint/pprint actual-result)
      (is (failed? (fail "df"))))))


;(executor-test)


;(run-tests)



(deftest commit-type-test
  (testing "test commit-type with one dml type "
    (let [data [{:dadysql.core/dml-key     :dadysql.core/dml-select
                 :dadysql.core/commit :dadysql.core/all}]
          actual-result (commit-type data)]
      (is (= actual-result
             :dadysql.core/none))))
  (testing "test commit-type with all "
    (let [data [{commit-type :dadysql.core/all}]
          actual-result (commit-type data)]
      (is (= actual-result
             :dadysql.core/all))
      ))
  (testing "test commit-type with all "
    (let [data [{commit-type :dadysql.core/all}]
          actual-result (commit-type data)]
      (is (= actual-result
             :dadysql.core/all))
      ))
  (testing "test commit-type with all "
    (let [data [{:dadysql.core/commit :dadysql.core/all}
                {:dadysql.core/commit :dadysql.core/none}]
          actual-result (commit-type data)]
      (is (= actual-result
             :dadysql.core/none)))))
