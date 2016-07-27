(ns dadysql.compiler.core-test
  (:use [clojure.test]
        [dadysql.compiler.core :as r]
        [dadysql.jdbc-core :as c]
        [dadysql.spec]
        [dady.common]
        [dadysql.compiler.test-data])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))



(deftest compiler-merge-test
  (testing "test compiler-merge "
    (let [v [{:dadysql.spec/param      [[1 2 3]]
              :clojure.core/column     {:p 1}
              :dadysql.spec/timeout    4
              :dadysql.spec/result     #{:dadysql.spec/single}
              :dadysql.spec/param-spec :a/b}
             {:dadysql.spec/param   [[8 9 0]]
              :dadysql.spec/timeout 6}
             {:dadysql.spec/param      [[5 6 7]]
              :clojure.core/column     {:p  4
                              :p1 :p}
              :p             9
              :dadysql.spec/result     #{:dadysql.spec/array}
              :dadysql.spec/param-spec :a/b}]
          expected-result {:dadysql.spec/param   [[5 6 7] [8 9 0] [1 2 3]],
                           :clojure.core/column  {:p 4, :p1 :p}
                           :dadysql.spec/timeout 6
                           :dadysql.spec/result  #{:dadysql.spec/array}
                           :dadysql.spec/param-spec :a/b
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
          w {:dadysql.spec/doc  "Modify department"
             :dadysql.spec/name [:insert-dept :update-dept :delete-dept]
             :dadysql.spec/sql  ["call next value for seq_dept"
                    "call next value for seq_empl"
                    "call next value for seq_meet"]}
          actual-result (->> (compile-one w config))]
      (is (not (empty? actual-result))))))



(comment
  (compile-one-test)


  ;(run-tests)

  )



