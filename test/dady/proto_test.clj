(ns dady.proto-test
  (:use [clojure.test]
        [dady.proto])
  (:require [dadysql.plugin.base-impl :as bi]
            [dadysql.plugin.factory :as c]
            [dadysql.plugin.params.core :as p]
            [dadysql.constant :refer :all]
            [schema.core :as s]
            [dady.common :refer :all]))


(defrecord TestParamKey [lname lorder]
  ILeafNode
  (-node-name [this] (:lname this)))


;(TestParamKey. :hello 3)



(deftest cell-index-single-test
  (testing "testing path-index-single "
    (let [impl (c/new-leaf-node-coll)
          gpc (bi/new-global-key-node impl)
          mpc (bi/new-module-key-node impl)
          c [gpc mpc]
          p (cell-index-single c global-key)]
      (is (= p 0)))))


;(path-index-single-test)


(deftest cell-index-batch-test
  (testing "testing path-index-batch "
    (let [impl (c/new-leaf-node-coll)
          gpc (bi/new-global-key-node impl)
          mpc (bi/new-module-key-node impl)
          c [gpc mpc]
          p (node-path c [module-key])]
      (is (= p [1]))))
  (testing "testing path-index-batch 2"
    (let [impl (vector (p/new-param-key 0 (p/new-child-keys)))
          gpc (bi/new-global-key-node impl)
          mpc (bi/new-module-key-node impl)
          c [gpc mpc]
          p (node-path c [module-key param-key])]
      (is (= p [1 :coll 0]))))
  (testing "testing path-index-batch 2"
    (let [impl (vector (p/new-param-key 0 (p/new-child-keys)))
          gpc (bi/new-global-key-node impl)
          mpc (bi/new-module-key-node impl)
          c [gpc mpc]
          p (node-path c [module-key :param-key1])]
      (is (= p [])))))


;(path-index-batch-test)

#_(deftest add-cell-test
    (testing "add-cell testing "
      (let [impl (vector (p/new-param-key (p/new-child-keys)))
            gpc (bi/new-global-key-node impl)
            mpc (bi/new-module-key-node impl)
            c [gpc mpc]
            r (add-node-to-path c [module-key param-key] (TestParamKey. validation-contain-key 6))]
        (is (not (nil? (get-node-from-path r [module-key param-key validation-contain-key]))))))
    (testing "add-cell testing "
      (let [impl (vector (p/new-param-key (p/new-child-keys)))

            mpc (bi/new-module-key-node impl)
            ;c [gpc mpc]
            r (add-node-to-path mpc [param-key] (TestParamKey. validation-contain-key 6))]
        (is (not (nil? (get-node-from-path r [param-key validation-contain-key]))))))

    )

;(add-cell-test)
#_(-> (vector (p/new-param-key (p/new-child-keys)))
      (bi/new-module-key-node)
      (add-node-to-path [] (TestParamKey. validation-contain-key 6)))



(deftest get-cell-test
  (testing "testing path-index-batch 2"
    (let [impl (vector (p/new-param-key 0 (p/new-child-keys)))
          gpc (bi/new-global-key-node impl)
          mpc (bi/new-module-key-node impl)
          c [gpc mpc]
          p (get-node-from-path c [module-key param-key])]
      (is (= (node-name p) param-key))))
  (testing "testing path-index-batch 2"
    (let [impl (vector (p/new-param-key 0 (p/new-child-keys)))
          gpc (bi/new-global-key-node impl)
          mpc (bi/new-module-key-node impl)
          c [gpc mpc]
          p (get-node-from-path c [module-key param-key param-ref-con-key])]
      (is (= (node-name p) param-ref-con-key)))))



;(get-path-component-test)


(deftest remove-cell-test
  (testing "testing remove-path-component "
    (let [impl (vector (p/new-param-key 0 (p/new-child-keys)))
          gpc (bi/new-global-key-node impl)
          mpc (bi/new-module-key-node impl)
          c [gpc mpc]
          p (remove-node-from-path c [module-key param-key param-ref-con-key])
          p (get-node-from-path p [module-key param-key param-ref-con-key])]
      (is (nil? p))))
  (testing "testing remove-path-component "
    (let [impl (vector (p/new-param-key 0 (p/new-child-keys)))
          gpc (bi/new-global-key-node impl)
          mpc (bi/new-module-key-node impl)
          c [gpc mpc]
          p (remove-node-from-path c [module-key param-key])
          p (get-node-from-path p [module-key param-key])]
      (is (nil? p))))
  )

;(remove-cell-test)

;(run-tests)



(deftest update-cell-test
  (testing "testing assoc-path-component "
    (let [impl (c/new-root-node)
          actual-result (update-node-to-path impl [module-key param-key]
                                             (map->TestParamKey {:cname  :param-ref-con-key
                                                                 :corder 1}))]
      (is (not (nil? actual-result))))))


;(update-cell-test)

(deftest cell-emit-test
  (testing "cell emit test "
    (let [app-proc (bi/new-module-key-node (c/new-leaf-node-coll))
          sch-value {doc-key   "hello"
                     name-key  [:hello :hell2]
                     ;      extend-meta-key {:hello {param-key [[:Next_transaction_id param-ref-key :transaction_id]]}}
                     param-key [[:next_transaction_id param-ref-key :transaction_id]]
                     sql-key   "select * from dual; select * from dual"}]
      (->> sch-value
           (s/validate (spec app-proc))
           (-emit app-proc)
           #_(clojure.pprint/pprint)))))



(deftest remove-type-test
  (testing "testing remove type "
    (let [w (->
              (c/new-root-node)
              (select-module-node-processor module-key)
              (remove-type :output))
          w (get-child w :column)]
      (is (nil? w)))))
