(ns dadysql.impl.util-test
  (:use [clojure.test]
        [dadysql.impl.util]))


(deftest get-path-test
         (testing "test get-path "
                  (let [d {:a {:b [{:b 4}]}}
                        expected-result [[:a]]
                        acutal-result (get-path d :a)]
                    (is (= acutal-result expected-result))))
         (testing "test get-path "
                  (let [d {:a [{:b 4}]}
                        expected-result [[:a 0 :b]]
                        acutal-result (get-path d [[:a 0]] :b)]
                    (is (= expected-result acutal-result))))
         (testing "test get-path "
                  (let [d {:a [{:b 4}
                               {:c 8}]}
                        expected-result [[:a 0] [:a 1]]
                        acutal-result (get-path d :a)]
                    (is (= expected-result acutal-result)))))