(ns dady.walk-test
  (:use [clojure.test]
        [dady.walk]))



(deftest postwalk-remove-with-test
  (testing "test with remove nil  "
    (let [actual-result (postwalk-remove-with nil? {:a 3 :b nil})
          expected-result {:a 3}]
      (is (= actual-result expected-result))))
  (testing "test with remove nil  "
    (let [actual-result (postwalk-remove-with nil? {:a 3 :b {:c nil}})
          expected-result {:a 3}]
      (is (= actual-result expected-result)))))



(deftest postwalk-replace-value-with-test
  (testing "test replace value with "
    (let [actual-result (postwalk-replace-value-with keyword->str
                                                     [{:a 3}])
          expected-result [{"a" 3}]]
      (is ( = actual-result
              expected-result )))))

(comment

  (postwalk-replace-value-with-test)

  (postwalk-remove-with-test)
  )



