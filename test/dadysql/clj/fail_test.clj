(ns dadysql.clj.fail-test
  (:use [clojure.test]
        [dadysql.clj.fail]))


(deftest fail-test
  (testing "test fail with type "
    (let [v (fail "Hello")]
      (is (= (type v)
             (type (fail 3))))
      (is (= (type (assoc v :r 4))
             (type (fail 3))))
      (is (not= (merge {:r 7} v)
                (type (fail 3))))))
  (testing "test fail"
    (let [v (fail "Error")]
      (is (failed? v))))
  (testing "test fail with additional param"
    (let [r (fail "Error")
          r (assoc r :hello "Hello")]
      (is (failed? r))))
  (testing "test fail with additional param"
    (let [r (fail "Error")
          r (merge r {:hello "Hello"})]
      (is (failed? r))))
  (testing "test fail with map "
    (let [r (assoc {} :hello "Hello")]
      (is (not (failed? r)))))
  (testing "test fail with map "
    (let [r (merge {} (fail "Hell"))]
      (is (not (failed? r))))))



(deftest try->test
  (testing "test try->"
    (let [f3 (fn [w] (fail 3))
          r (try-> {:a 3}
                   (assoc :b 3)
                   f3
                   (assoc :h 4))]
      (is (failed? r)))))

;(try->test)


(deftest try!-test
  (testing "test try! "
    (let [v (try! inc nil)]
      (is (failed? v)))))



(deftest xf-until-test
  (testing "test xf-until "
    (let [p (comp (xf-until odd?)
                  (map inc))]
      (are [x y] (= (transduce p conj x) y)
                 [1 2 3] 1
                 [2 4] [3 5]))))

;(xf-until-test)


(deftest comp-xf-until-test
  (testing "test xf-until "
    (let [p (comp-xf-until (map inc))]
      (are [x y] (= (transduce p conj x) y)
                 [1 (fail "fail") 3] (fail "fail")
                 [2 4] [3 5]))))
