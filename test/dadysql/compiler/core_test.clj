(ns dadysql.compiler.core-test
  (:use [clojure.test]
        [dadysql.compiler.core :as r]
        [dadysql.compiler.spec]
        [dady.common]
        [dadysql.compiler.test-data])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))



(deftest compiler-merge-test
  (testing "test compiler-merge "
    (let [v [{:dadysql.core/param-coll [[1 2 3]]
              :clojure.core/column     {:p 1}
              :dadysql.core/timeout    4
              :dadysql.core/result     #{:dadysql.core/result-single}
              :dadysql.core/param-spec :a/b}
             {:dadysql.core/param-coll [[8 9 0]]
              :dadysql.core/timeout    6}
             {:dadysql.core/param-coll [[5 6 7]]
              :clojure.core/column     {:p  4
                                        :p1 :p}
              :p                       9
              :dadysql.core/result     #{:dadysql.core/result-array}
              :dadysql.core/param-spec :a/b}]
          expected-result {:dadysql.core/param-coll [[5 6 7] [8 9 0] [1 2 3]],
                           :clojure.core/column     {:p 4, :p1 :p}
                           :dadysql.core/timeout    6
                           :dadysql.core/result     #{:dadysql.core/result-array}
                           :dadysql.core/param-spec :a/b
                           :p                       9}
          actual-result (apply merge-with compiler-merge v)]
      (is (= actual-result expected-result)))))


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


  (->> (r/default-config)
       (r/compile-one compile-one-data2))

  )



