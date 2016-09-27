(ns dady.proto-test
  (:use [clojure.test]
        [dady.proto])
  (:require [dadysql.plugin.factory :as bi]
            [dadysql.plugin.factory :as c]
            [dadysql.plugin.params.core :as p]
            [dadysql.spec :refer :all]
        ;    [schema.core :as s]
            [dady.common :refer :all]))


(defrecord TestParamKey [lname lorder]
  ILeafNode
  (-node-name [this] (:lname this)))


;(TestParamKey. :hello 3)



(deftest cell-index-single-test
  (testing "testing path-index-single "
    (let [mpc (bi/new-root-node )
          p   (cell-index-single mpc :dadysql.core/name)]
      (is (= p 0)))))


;(path-index-single-test)


(deftest cell-index-batch-test

  (testing "testing path-index-batch 2"
    (let [impl (vector (p/new-param-key 0 (p/new-child-keys)))
          p (node-path impl [:dadysql.core/param ])]
      (is (= p [0]))))
  #_(testing "testing path-index-batch 2"
    (let [impl (vector (p/new-param-key 0 (p/new-child-keys)))

          mpc (bi/new-module-key-node impl)
          c [ mpc]
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



#_(deftest get-cell-test
  (testing "testing path-index-batch 2"
    (let [impl (vector (p/new-param-key 0 (p/new-child-keys)))

          mpc (bi/new-module-key-node impl)
          c [ mpc]
          p (get-node-from-path c [module-key param-key])]
      (is (= (node-name p) param-key))))
  (testing "testing path-index-batch 2"
    (let [impl (vector (p/new-param-key 0 (p/new-child-keys)))

          mpc (bi/new-module-key-node impl)
          c [mpc]
          p (get-node-from-path c [module-key param-key param-ref-con-key])]
      (is (= (node-name p) param-ref-con-key)))))



;(get-path-component-test)


#_(deftest remove-cell-test
  (testing "testing remove-path-component "
    (let [impl (vector (p/new-param-key 0 (p/new-child-keys)))

          mpc (bi/new-module-key-node impl)
          c [mpc]
          p (remove-node-from-path c [module-key param-key param-ref-con-key])
          p (get-node-from-path p [module-key param-key param-ref-con-key])]
      (is (nil? p))))
  (testing "testing remove-path-component "
    (let [impl (vector (p/new-param-key 0 (p/new-child-keys)))

          mpc (bi/new-module-key-node impl)
          c [mpc]
          p (remove-node-from-path c [module-key param-key])
          p (get-node-from-path p [module-key param-key])]
      (is (nil? p))))
  )

;(remove-cell-test)

;(run-tests)



#_(deftest update-cell-test
  (testing "testing assoc-path-component "
    (let [impl (c/new-root-node)
          actual-result (update-node-to-path impl [module-key param-key]
                                             (map->TestParamKey {:cname  :param-ref-con-key
                                                                 :corder 1}))]
      (is (not (nil? actual-result))))))


;(update-cell-test)

#_(deftest cell-emit-test
  (testing "cell emit test "
    (let [app-proc (bi/new-module-key-node (c/new-leaf-node-coll))
          sch-value {doc-key   "hello"
                     ;      :dadysql.core/extend {:hello {param-key [[:Next_transaction_id param-ref-key :transaction_id]]}}
                     param-key [[:next_transaction_id param-ref-key :transaction_id]]
                     sql-key   "select * from dual; select * from dual"}]
      (->> sch-value
           (s/validate (eval (spec app-proc)))
           (-emit app-proc)
           #_(clojure.pprint/pprint)))))



(deftest remove-type-test
  (testing "testing remove type "
    (let [w (->
              (c/new-root-node)
              ;(select-module-node-processor module-key)
              (remove-type :output))
          w (get-child w :column)]
      (is (nil? w)))))
