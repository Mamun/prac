 (ns dady.common-test
   (:use [clojure.test]
         [dady.common]
         [dady.fail])
   (:require [dadysql.constant :refer :all]
             [dadysql.core-util :refer :all]))




(deftest as-lower-case-keyword-test
  (testing "test keyword->lower-case-keyword"
    (are [x y] (= x y)
               :hello (as-lower-case-keyword :Hello)
               :hello (as-lower-case-keyword :hello))))


;(keyword->lower-case-keyword-test)

(deftest replace-last-in-vector-test
  (testing "test update-last "
    (are [x y] (= x y)
               [1 2 3] (replace-last-in-vector [1 2 5] 3)
               [2] (replace-last-in-vector [1] 2)
               [] (replace-last-in-vector [] 2))
    (is (thrown? ClassCastException
                 (replace-last-in-vector (list 2 3) 2)))))



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



;(try!-test)

(deftest merge-with-key-type-test
  (testing "test merge-with-key-type "
    (let [v {:a 1
             :b {:hello3 "hello4"}}
          v1 {:a 2
              :b {:hello "Hello"}}
          f (fn [k v1 v2] (do
                            (if (= k :b)
                              (merge v1 v2)
                              (or v1 v2))))
          expected-result {:a 1, :b {:hello3 "hello4", :hello "Hello"}}
          actual-result (merge-with-key-type f v v1)]
      (is (= actual-result
             expected-result)))))

;(merge-with-key-type-test)

;(merge-with concat {:params [[1 2 3]]} {:params [[5 6 7]]})

;;;;;;;;;;;;;;;;;

(deftest group-by-value-test
  (testing "test group-by-value "
    (let [dt :a
          m {:a 3 :b 4}
          expected-result {3 {:a 3, :b 4}}
          actual-result (group-by-value dt m)]
      (is (= expected-result actual-result))))
  (testing "test group-by-value "
    (let [dt :a
          m [{:a 3 :b 4}
             {:a 4 :b 4}]
          expected-result {3 [{:a 3, :b 4}], 4 [{:a 4, :b 4}]}
          actual-result (group-by-value dt m)]
      (is (= expected-result actual-result))))
  (testing "test group-by-value with collection collection "
    (let [dt :a
          m [[:a :b] [3 4] [4 4]]
          expected-result {3 [[:a :b] [3 4]], 4 [[:a :b] [4 4]]}
          actual-result (group-by-value dt m)]
      (is (= expected-result actual-result))))
  (testing "test group-by-value with value  "
    (let [dt :a
          m [45]
          expected-result [45]
          actual-result (group-by-value dt m)]
      (is (= expected-result actual-result)))))








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




;(get-key-path-with-child-test)


(deftest acc-with-range-tst
  (testing "test acc-with-range "
    (let [actual-result (acc-with-range 2 [[:a :b :v1 "Hello"]] [:a :b1 :v "Hello"])
          expected-result [[:a :b :v1 "Hello"]
                           [:a :b1 :v "Hello"]]]
      (is (= expected-result
             actual-result))))
  (testing "test acc-with-range "
    (let [actual-result (acc-with-range 10 [[:a :b :v1 "Hello"]] [:a :b1 :v "Hello"])
          expected-result [[:a :b :v1 "Hello"]
                           [:a :b1 :v "Hello"]]]
      (is (= expected-result
             actual-result)))))


(deftest concat-distinct-with-range-test
  (testing "test concat-distinct-with-range "
    (let [w [[:a :b :v "Hello"]]
          w1 [[:a :b :v1 "Hello"]]
          expected-result [[:a :b :v1 "Hello"]]
          actul-result (concat-distinct-with-range 2 w w1)]
      (is (= actul-result
             expected-result)))))



(deftest distinct-with-range-test
  (testing "test distinct-with-range "
    (let [w [[:a :b :c "Testing"]
             [:a :b :c "hello"]]
          expected-result [[:a :b :c "Testing"]]
          actual-result (distinct-with-range 3 w)]
      (is (= actual-result
             expected-result)))))

;(distinct-with-range-test)

(deftest contain-all?-test
  (testing "test contain-all? "
    (let [v [:commit :commit-any]
          actual-result (contain-all? v :commit)]
      (is (nil? actual-result))))
  (testing "run contain-all? "
    (let [v [:commit-any :commit-any]
          actual-result (contain-all? v :commit-any)]
      (is (= :commit-any actual-result)))))

;(contain-all?-test)
;(keyword "sdf")

(deftest as-keyword-test
  (testing "test as-keyword with number"
    (let [actual-result (as-keyword nil)]
      (is (nil? actual-result))))
  (testing "test as-keyword with number"
    (let [actual-result (as-keyword 3)]
      (is (= actual-result 3))))
  (testing "test as-keyword with string"
    (let [actual-result (as-keyword "sdf")]
      (is (= actual-result :sdf)))))

;(as-keyword-test)

(deftest select-values-test
  (testing "test select-values "
    (let [data {:a "a"
                :b "b"
                :c "c"}
          actual-result (select-values data [:a :c])]
      (is (= actual-result
             ["a" "c"]))))
  (testing "test select-values "
    (let [data [1 2 3]
          actual-result (select-values data [:a :c])]
      (is (= actual-result
             [1 2 3])))))




(deftest xf-skip-type-test
  (testing "test xf-skip-type "
    (let [p (comp (xf-skip-type odd?)
                  (map inc))]
      (are [x y] (= (transduce p conj x) y)
                 [1 2 3] [1 3 3]
                 [2 4] [3 5]
                 ))))



;(xf-skip-type-test)

(deftest xf-until-test
  (testing "test xf-until "
    (let [p (comp (xf-until odd?)
                  (map inc))]
      (are [x y] (= (transduce p conj x) y)
                 [1 2 3] 1
                 [2 4] [3 5]
                 ))))

;(xf-until-test)


(deftest comp-xf-until-test
  (testing "test xf-until "
    (let [p (comp-xf-until (map inc))]
      (are [x y] (= (transduce p conj x) y)
                 [1 (fail "fail") 3] (fail "fail")
                 [2 4] [3 5]
                 ))))
