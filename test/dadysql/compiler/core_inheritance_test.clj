(ns dadysql.compiler.core-inheritance-test
  (:use [clojure.test]
        [dadysql.compiler.core-inheritance]))


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
