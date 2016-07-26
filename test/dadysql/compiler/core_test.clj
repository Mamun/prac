(ns dadysql.compiler.core-test
  (:use [clojure.test]
        [dadysql.compiler.core :as r]
        [dadysql.core2 :as c]
        [dadysql.core]
        [dady.common]
        [dadysql.compiler.test-data])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))



(deftest compiler-merge-test
  (testing "test compiler-merge "
    (let [v [{:dadysql.core/param      [[1 2 3]]
              :clojure.core/column     {:p 1}
              :dadysql.core/timeout    4
              :dadysql.core/result     #{result-single-key}
              :dadysql.core/param-spec :a/b}
             {:dadysql.core/param   [[8 9 0]]
              :dadysql.core/timeout 6}
             {:dadysql.core/param      [[5 6 7]]
              :clojure.core/column     {:p  4
                              :p1 :p}
              :p             9
              :dadysql.core/result     #{result-array-key}
              :dadysql.core/param-spec :a/b}]
          expected-result {:dadysql.core/param   [[5 6 7] [8 9 0] [1 2 3]],
                           :clojure.core/column  {:p 4, :p1 :p}
                           :dadysql.core/timeout 6
                           :dadysql.core/result  #{result-array-key}
                           :dadysql.core/param-spec :a/b
                           :p          9}
          actual-result (apply merge-with compiler-merge v)]
      ;(clojure.pprint/pprint actual-result)
      (is (= actual-result
             expected-result
             )))))


;(compiler-merge-test)

(deftest compile-one-test
  (testing "test compile-one "
    (let [config (r/default-config)
          actual-result (r/compile-one compile-one-data config)]
      (is (= actual-result
             compile-one-expected-result))))
  (testing "test compile-one"
    (let [config (r/default-config)
          w {:dadysql.core/doc  "Modify department"
             :dadysql.core/name [:insert-dept :update-dept :delete-dept]
             :dadysql.core/sql  ["call next value for seq_dept"
                    "call next value for seq_empl"
                    "call next value for seq_meet"]}
          actual-result (->> (compile-one w config))]
      (is (not (empty? actual-result))))))



(comment
  (compile-one-test)


  ;(run-tests)

  )



