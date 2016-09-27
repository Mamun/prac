(ns dadysql.plugin.params.core-test
  (:use [clojure.test]
        [dady.fail])
  (:require
    [dadysql.plugin.factory :as b]

    ;[dady.node-proto :as p]
    [dady.proto :refer :all]
    [dadysql.plugin.params.core :refer :all]
    #_[dadysql.spec :refer :all]
    [dady.common :refer :all]
    [clojure.spec :as sp]))


(deftest param-paths-test
  (testing "test param-paths  "
    (let [coll [{:dadysql.core/param [[:id :ref-gen :gen-dept]]}
                {:dadysql.core/param [[:id3 :ref-gen :gen-dept]]}]
          expected-result (list [[:id] :ref-gen :gen-dept] [[:id3] :ref-gen :gen-dept])
          actual-result (param-paths :dadysql.core/format-map coll {:id2 1})]
      (is (= actual-result
             expected-result)))))

;(param-paths-test)


(deftest model-param-paths-test
  (testing "test model-param-paths  "
    (let [coll [{:dadysql.core/param [[:transaction_id :ref-con 0]
                            [:transaction_id2 :ref-key :id]
                            [:id :dadysql.core/ref-gen :gen-dept]],
                 :dadysql.core/model :employee}
                {:dadysql.core/param [[:city :ref-con 0]],
                 :dadysql.core/model :employee-detail}]
          param {:employee {:firstname "Schwan"
                            :lastname  "Ragg"
                            :dept_id   1
                            :employee-detail
                                       {:street  "Schwan",
                                        :state   "Bayern",
                                        :country "Germany"}}}
          expected-result [[[:employee :transaction_id] :ref-con 0]
                           [[:employee :transaction_id2] :ref-key :id]
                           [[:employee :id] :dadysql.core/ref-gen :gen-dept]
                           [[:employee :employee-detail :city] :ref-con 0]]
          actual-result (param-paths :dadysql.core/format-nested coll :dadysql.core/param)]
      (is (= actual-result
             expected-result)))))


(deftest param-ref-con-test

  (testing "test param-ref-con "
    (let [w (new-child-keys)
          cw (get-node-from-path w [:dadysql.core/ref-con])]
      (is (= 5
             (-pprocess cw [:id :dadysql.core/ref-con 5] {})))))
  (testing "test param-ref-con-key"
    (let [context (new-param-key 0 (new-child-keys))
          coll [{:dadysql.core/param [[:transaction_id :dadysql.core/ref-con 0]],
                 :dadysql.core/model :employee}]
          input {:id 2}
          expected-result {:id 2 :transaction_id 0}
          actual-result (apply-param-proc input
                                          :dadysql.core/format-map
                                          coll
                                          context
                                          )]
      (is (= expected-result
             actual-result))))
  (testing "test param-ref-con "
    (let [context (-> (b/new-root-node)
                      ;(select-module-node-processor module-key)
                      (assoc-param-ref-gen (fn [k v] 5)))
          context (get-node-from-path context [:dadysql.core/param])
          coll [{:dadysql.core/param [[:transaction_id :dadysql.core/ref-con 0]],
                 :dadysql.core/model :employee}]
          input {:employee {:id 2}}
          expected-result {:employee {:id 2 :transaction_id 0}}
          actual-result (apply-param-proc input
                                          :dadysql.core/format-nested
                                          coll
                                          context)]
      (is (= expected-result
             actual-result)))))


;(param-ref-con-test)


(deftest param-ref-key-test
  (testing "test param-ref-key"
    (let [context (new-param-key 0 (new-child-keys))
          coll [{:dadysql.core/param [[:transaction_id :dadysql.core/ref-key :id]],
                 :dadysql.core/model :employee}]
          input {:id 2}
          expected-result {:id 2 :transaction_id 2}
          actual-result (apply-param-proc input
                                          :dadysql.core/format-map
                                          coll
                                          context
                                          )]
      (is (= expected-result
             actual-result))))
  (testing "test param-ref-key "
    (let [context (-> (b/new-root-node)
                      ;(select-module-node-processor module-key)
                      (assoc-param-ref-gen (fn [k v] 5)))
          context (get-node-from-path context [:dadysql.core/param])
          ;         _ (clojure.pprint/pprint context)
          coll [{:dadysql.core/param [[:transaction_id :dadysql.core/ref-key :id]],
                 :dadysql.core/model :employee}]
          input {:employee {:id 2}}
          expected-result {:employee {:id 2 :transaction_id 2}}
          actual-result (apply-param-proc input
                                          :dadysql.core/format-nested
                                          coll
                                          context)]
      (is (= expected-result
             actual-result)))))

;(param-ref-key-test)

(deftest param-ref-fn-key-test
  (testing "test params-ref-fn-key "
    (let [context (new-param-key 0 (new-child-keys))
          coll [{:dadysql.core/param [[:transaction_id :dadysql.core/ref-fn-key inc :id]],
                 :dadysql.core/model :employee}]
          input {:id 2}
          expected-result {:id 2 :transaction_id 3}
          actual-result (apply-param-proc input
                                          :dadysql.core/format-map
                                          coll
                                          context
                                          )]
      (is (= expected-result
             actual-result))))
  (testing "test param-ref-fn-key "
    (let [context (-> (b/new-root-node)
                      ;(select-module-node-processor module-key)
                      (assoc-param-ref-gen (fn [k v] 5)))
          context (get-node-from-path context [:dadysql.core/param])
          coll [{:dadysql.core/param [[:transaction_id :dadysql.core/ref-fn-key inc :id]],
                 :dadysql.core/model :employee}]
          input {:employee {:id 2}}
          expected-result {:employee {:id 2 :transaction_id 3}}
          actual-result (apply-param-proc input
                                          :dadysql.core/format-nested
                                          coll
                                          context)]
      (is (= expected-result
             actual-result)))))




(deftest param-impl-test
  (testing "test params-ref-gen-key"
    (let [context (-> (b/new-root-node)
                      ;(select-module-node-processor module-key)
                      (assoc-param-ref-gen (fn [k v] 5)))
          context (get-node-from-path context [:dadysql.core/param])
          coll [{:dadysql.core/param [[:transaction_id :dadysql.core/ref-gen :id]],
                 :dadysql.core/model :employee}]
          input {:id 2}
          expected-result {:id 2 :transaction_id 5}
          actual-result (apply-param-proc input
                                          :dadysql.core/format-map
                                          coll
                                          context
                                          )]
      (is (= expected-result
             actual-result))))
  (testing "test params-ref-gen-key"
    (let [context (-> (b/new-root-node)
                      ;(select-module-node-processor module-key)
                      (assoc-param-ref-gen (fn [k v] (fail "Not found"))))
          context (get-node-from-path context [:dadysql.core/param])
          coll [{:dadysql.core/param [[:transaction_id :dadysql.core/ref-gen :id]],
                 :dadysql.core/model :employee}]
          input {:id 2}
          actual-result (apply-param-proc input
                                          :dadysql.core/format-map
                                          coll
                                          context)]
      (is (failed? actual-result)))))


;(param-impl-test)


(deftest params-ref-gen-key-test
  (testing "test param-ref-fn-key "
    (let [context (-> (b/new-root-node)
                      ;(select-module-node-processor module-key)
                      (assoc-param-ref-gen (fn [k v] 3)))
          context (get-node-from-path context [:dadysql.core/param])
          coll [{:dadysql.core/param [[:transaction_id :dadysql.core/ref-gen :id]],
                 :dadysql.core/model :employee}]
          input {:employee {:id 2}}
          expected-result {:employee {:id 2, :transaction_id 3}}
          actual-result (apply-param-proc input
                                          :dadysql.core/format-nested
                                          coll
                                          context)]
      (is (= (expected-result
               actual-result)))))
  (testing "test params-ref-gen-key"
    (let [context (-> (b/new-root-node)
                      ;(select-module-node-processor module-key)
                      (assoc-param-ref-gen (fn [k v] (fail "Not found"))))
          context (get-node-from-path context [:dadysql.core/param])
          coll [{:dadysql.core/param [[:transaction_id :dadysql.core/ref-gen :id]],
                 :dadysql.core/model :employee}]
          input {:employee {:id 2}}
          actual-result (apply-param-proc input
                                          :dadysql.core/format-nested
                                          coll
                                          context)]
      (is (failed? actual-result)))))


;(params-ref-gen-key-test)


(deftest do-param-comp-test
  (testing "test do-params-comp  "
    (let [context (-> (b/new-root-node)
                      ;(select-module-node-processor module-key)
                      (assoc-param-ref-gen (fn [k v] 5)))
          context (get-node-from-path context [:dadysql.core/param])
          coll [{:dadysql.core/param [[:transaction_id :dadysql.core/ref-con 0]
                            [:id2 :dadysql.core/ref-fn-key inc :transaction_id]
                            [:id4 :dadysql.core/ref-key :id]
                            [:id3 :dadysql.core/ref-gen :id]]
                 :dadysql.core/model :employee}]
          input {:employee {:id 2}}
          expected-result {:employee {:id 2, :transaction_id 0, :id4 2, :id2 1, :id3 5}}
          actual-result (apply-param-proc input
                                          :dadysql.core/format-nested
                                          coll
                                          context)]
      (is (= expected-result
             actual-result))))
  (testing "test do-params-comp for empty collection  "
    (let [context (-> (b/new-root-node)
                      ;(select-module-node-processor module-key)
                      (assoc-param-ref-gen (fn [k v] 5)))
          context (get-node-from-path context [:dadysql.core/param])
          coll []
          input {:employee {:id 2}}
          expected-result {:employee {:id 2}}
          actual-result (apply-param-proc input
                                          :dadysql.core/format-nested
                                          coll
                                          context)]
      (is (= expected-result
             actual-result)))))

;(do-param-comp-test)




;(param-key-compile-test)

(comment
  (run-tests)
  )

