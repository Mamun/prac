(ns dadysql.plugin.base-impl-test
  (:use [clojure.test]
        [dady.proto])
  (:require [dadysql.plugin.common-impl :refer :all]
            [dadysql.plugin.base-impl :refer :all]
            [dadysql.plugin.factory :refer :all]
            [dadysql.spec :refer :all]
            [dady.common :refer :all]
            [dadysql.jdbc-core :as c]
            [dadysql.jdbc :as j]
            #_[schema.core :as s]))


#_(deftest doc-key-test
  (testing "testing doc key "
    (let [doc-ins (DocKey. "Hello") ]
      (println doc-ins)
      ))
  )


#_(deftest module-key-spec-test
  (testing "testing module key spec "
    (let [s (spec (get-node-from-path (new-root-node) [module-key]))]
      ;(clojure.pprint/pprint s)
      (is (not (nil? s)))
      )
    ))

;(module-key-spec-test)

#_(deftest config-key-spec-test
  (testing "testing module key spec "
    (let [s (spec (get-node-from-path (new-root-node) [global-key]))]
      ;(clojure.pprint/pprint s)
      (is (not (nil? s)))
      )
    ))

;(config-key-spec-test)
;









