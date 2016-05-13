(ns tiesql.plugin.sql-executor-test
  (:use [clojure.test]
        [tiesql.plugin.sql-executor])
  (:require [tiesql.common :refer :all]
            [cljc.common :as cc]))


(deftest warp-map-output-test
  (testing "test warp-map-output"
    (let [handler (fn [m] (throw (Exception. "Some text")))
          hand (-> handler
                   (warp-map-output))
          input {sql-key "select * from dual"}
          reslt (hand input)]
      ;(println reslt)
      ;(println (cc/-failed? reslt ))
      (is (cc/failed? reslt)))))



;(warp-map-output-test)

(deftest warp-parallel-coll-test
  (testing "test warp-parallel-coll"
    (let [handler (fn [m] (assoc m output-key {:a 3}))
          han (-> handler
                  (warp-map-output)
                  (warp-async-go))
          m-coll [{sql-key "select * from dual"}
                  {sql-key "select * from dual1 "}
                  {sql-key "select * from dual1 "}]
          result (apply-handler-parallel han m-coll)]
      (is (not (empty? result)))))
  (testing "test warp-parallel-coll"
    (let [handler (fn [m]
                    (do
                      (when (= :b (name-key m))
                        (Thread/sleep 3000))
                      (assoc m output-key {:a 3})))
          han (-> handler
                  (warp-map-output)
                  (warp-async-go))
          m-coll [{sql-key  "select * from dual"
                   name-key :a}
                  {sql-key  "select * from dual1 "
                   name-key :b}
                  {sql-key  "select * from dual1 "
                   name-key :c}]
          result (apply-handler-parallel han m-coll)]
      (is (cc/failed? (get-in result [2]))))))


;(warp-parallel-coll-test)

(deftest executor-test
  (testing "test warp-until-fail-coll"
    (let [handler (fn [m] (throw (Exception. "Some text")))
          hand (-> handler
                   (warp-map-output)
                   (warp-async-go))
          coll [{sql-key  "select * from dual"
                 name-key :a}
                {sql-key  "select * from dual1 "
                 name-key :b}
                {sql-key  "select * from dual1 "
                 name-key :c}]
          ;actual-result (apply-handler-until-fail hand coll)
          ]
      ;(clojure.pprint/pprint actual-result)
      (is (cc/failed? (cc/fail "df"))))))


;(executor-test)


;(run-tests)



(deftest commit-type-test
  (testing "test commit-type with one dml type "
    (let [data [{dml-key     dml-select-key
                 commit-type commit-all-key}]
          actual-result (commit-type data)]
      (is (= actual-result
             commit-none-key))))
  (testing "test commit-type with all "
    (let [data [{commit-type commit-all-key}]
          actual-result (commit-type data)]
      (is (= actual-result
             commit-all-key))
      ))
  (testing "test commit-type with all "
    (let [data [{commit-type commit-all-key}]
          actual-result (commit-type data)]
      (is (= actual-result
             commit-all-key))
      ))
  (testing "test commit-type with all "
    (let [data [{commit-key commit-all-key}
                {commit-key commit-none-key}]
          actual-result (commit-type data)]
      (is (= actual-result
             commit-none-key)))))
