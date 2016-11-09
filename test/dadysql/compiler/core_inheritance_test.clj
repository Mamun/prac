(ns dadysql.compiler.core-inheritance-test
  (:use [clojure.test]
        [dadysql.compiler.core-inheritance]))


(deftest compiler-merge-test
  (testing "test compiler-merge "
    (let [v [{:dadysql.core/default-param {:offset 1}
              :clojure.core/column     {:p 1}
              :dadysql.core/timeout    4
              :dadysql.core/result     #{:dadysql.core/result-single}
              :dadysql.core/param-spec {:req {:id 'int?}
                                        :opt {:name 'string?}} }
             {:dadysql.core/default-param {:offset 10}
              :dadysql.core/timeout    6}
             {:dadysql.core/default-param {:offset 20}
              :clojure.core/column     {:p  4
                                        :p1 :p}
              :p                       9
              :dadysql.core/result     #{:dadysql.core/result-array}
              :dadysql.core/param-spec {:req {:id 'integer? }}}]
          expected-result {:dadysql.core/default-param {:offset 20},
                           :clojure.core/column {:p 4, :p1 :p},
                           :dadysql.core/timeout 6,
                           :dadysql.core/result #{:dadysql.core/result-array},
                           :dadysql.core/param-spec {:req {:id 'integer?},
                                                     :opt {:name 'string?}},
                           :p 9}
          actual-result (apply merge-with compiler-merge v)]
      ;(clojure.pprint/pprint actual-result)
      (is (= actual-result expected-result)))))


(comment

  (compiler-merge-test)
  )
