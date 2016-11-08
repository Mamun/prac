(ns dadysql.impl.param-impl-test
  (:use [clojure.test]
        [dady.fail])
  (:require
    [dadysql.impl.param-impl :refer :all]
    [dady.common :refer :all]))


(deftest param-paths-test
  (testing "test param-paths  "
    (let [coll [{:dadysql.core/default-param  [:id  (fn [m] 2)]}
                {:dadysql.core/default-param  [:id3 (fn [m] 3)]}]
          expected-result {:id [:id], :id3 [:id3]}
          actual-result (get-in (param-paths :dadysql.core/format-map coll {:id2 1}) [0 :dadysql.core/param-path]) ]
      ;(println actual-result)
      (is (= actual-result
             expected-result)))))

;(param-paths-test)


(deftest model-param-paths-test
  (testing "test model-param-paths  "
    (let [coll [{:dadysql.core/default-param [:transaction_id 0
                                              :transaction_id2 :id
                                              :id 2],
                 :dadysql.core/model         :employee}
                {:dadysql.core/default-param [:city :a],
                 :dadysql.core/model         :employee-detail}]
          param {:employee {:firstname "Schwan"
                            :lastname  "Ragg"
                            :dept_id   1
                            :employee-detail
                                       {:street  "Schwan",
                                        :state   "Bayern",
                                        :country "Germany"}}}
          expected-result [{:transaction_id  [:employee :transaction_id],
                            :transaction_id2 [:employee :transaction_id2],
                            :id              [:employee :id]}
                           {:city [:employee :employee-detail :city]}

                           ]
          actual-result (mapv :dadysql.core/param-path (param-paths :dadysql.core/format-nested coll param)) ]
      (is (= actual-result
             expected-result)))))

;(model-param-paths-test)

;(def apply-param-proc (param-exec identity))


(deftest param-exec-test
  (testing "test param-ref-con-key"
    (let [coll [{:dadysql.core/default-param [:transaction_id (fn [v] 0)],
                 :dadysql.core/model         :employee}]
          input {:id 2}
          expected-result {:id 2 :transaction_id 0}
          actual-result (param-exec coll input :dadysql.core/format-map)]
      (is (= expected-result
             actual-result))))

  (testing "test param-ref-key "
    (let [coll [{:dadysql.core/default-param [:transaction_id (fn [m] (:id m))],
                 :dadysql.core/model         :employee}]
          input {:employee {:id 2}}
          expected-result {:employee {:id 2 :transaction_id 2}}
          actual-result (param-exec coll input :dadysql.core/format-nested)]
      (is (= expected-result
             actual-result))))
  (testing "test params-ref-fn-key "
    (let [coll [{:dadysql.core/default-param [:transaction_id (fn [m] (inc (:id m)))],
                 :dadysql.core/model         :employee}]
          input {:id 2}
          expected-result {:id 2 :transaction_id 3}
          actual-result (param-exec coll input :dadysql.core/format-map)]
      (is (= expected-result
             actual-result))))
  (testing "test param-ref-fn-key "
    (let [coll [{:dadysql.core/default-param [:transaction_id (fn [m] (inc (:id m)))]
                 :dadysql.core/model         :employee}]
          input {:employee {:id 2}}
          expected-result {:employee {:id 2 :transaction_id 3}}
          actual-result (param-exec coll input :dadysql.core/format-nested)]
      (is (= expected-result
             actual-result))))
  (testing "test params-ref-gen-key"
    (let [
          coll [{:dadysql.core/default-param [:transaction_id (fn [m] 5)],
                 :dadysql.core/model         :employee}]
          input {:id 2}
          expected-result {:id 2 :transaction_id 5}
          actual-result (param-exec coll input :dadysql.core/format-map)]
      (is (= expected-result
             actual-result))))
  (testing "test params-ref-gen-key"
    (let [coll [{:dadysql.core/default-param [:id (fn [m] (do
                                                            (fail "Failed ")
                                                            ))]
                 :dadysql.core/model         :employee}]
          input {:id2 2}
          actual-result (param-exec coll input :dadysql.core/format-map)]
      (is (failed? actual-result))))
  (testing "test param-ref-fn-key "
    (let [coll [{:dadysql.core/default-param [:transaction_id (fn [m] 3)],
                 :dadysql.core/model         :employee}]
          input {:employee {:id 2}}
          expected-result {:employee {:id 2, :transaction_id 3}}
          actual-result (param-exec coll input :dadysql.core/format-nested)]
      (is (= (expected-result
               actual-result)))))
  (testing "test params-ref-gen-key"
    (let [coll [{:dadysql.core/default-param [:transaction_id (fn [m] (fail "Failed "))],
                 :dadysql.core/model         :employee}]
          input {:employee {:id 2}}
          actual-result (param-exec coll input :dadysql.core/format-nested)]
      (is (failed? actual-result))))

  (testing "test do-params-comp  "
    (let [coll [{:dadysql.core/default-param [:transaction_id (fn [_] 0)
                                              :id2            (fn [m] (inc (:transaction_id m)))
                                              :id4            (fn [m] (:id m))
                                              :id3            (fn [m] (:id m))]
                 :dadysql.core/model         :employee}]
          input {:employee {:id 2}}
          expected-result {:employee {:id 2, :transaction_id 0, :id4 2, :id2 1, :id3 2}}
          actual-result (param-exec coll input :dadysql.core/format-nested)]
      (is (= expected-result
             actual-result))))
  (testing "test do-params-comp for empty collection  "
    (let [coll []
          input {:employee {:id 2}}
          expected-result {:employee {:id 2}}
          actual-result (param-exec coll input :dadysql.core/format-nested)]
      (is (= expected-result
             actual-result))))
  )

;(param-exec-test)

#_(deftest convert-param-test
  (testing "convert param test "
    (let [input-m {:b 1}
          tem-gen (fn [r] (do 3))
          w (-> (convert-param-t [:a '(constantly 0)
                                  :b :w
                                  :c '(inc :b)])
                (assoc-temp-gen tem-gen))
          a-gen (:a w)
          b-gen (:b w)
          c-gen (:c w)]
      (is (= (a-gen input-m) 0))
      (is (= (b-gen input-m) 3))
      (is (= (c-gen input-m) 2)))))


(comment

  ;(convert-param-test)
  (run-tests)
  )

