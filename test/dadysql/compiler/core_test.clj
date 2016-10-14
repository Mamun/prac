(ns dadysql.compiler.core-test
  (:use [clojure.test]
        [dadysql.compiler.core :as r]
        [dadysql.compiler.spec]
        [dady.common]
        [dadysql.compiler.test-data])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))






;(compiler-merge-test)

(deftest compile-one-test
  (testing "test compile-one "
    (let [config (r/default-config)
          actual-result (r/compile-module compile-one-data config)]
      (is (= actual-result
             compile-one-expected-result))))
  (testing "test compile-one"
    (let [config (r/default-config)
          w {:dadysql.core/doc  "Modify department"
             :dadysql.core/name [:insert-dept :update-dept :delete-dept]
             :dadysql.core/sql  ["call next value for seq_dept"
                                 "call next value for seq_empl"
                                 "call next value for seq_meet"]}
          actual-result (->> (compile-module w config))]
      (is (not (empty? actual-result))))))



(comment
  (compile-one-test)


  ;(run-tests)


  (->> (r/default-config)
       (r/compile-m compile-one-data2))

  )



