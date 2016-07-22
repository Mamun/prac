(ns dadysql.compiler.util-test
  (:use [clojure.test]
        [dadysql.compiler.core :as r]
        [dadysql.core2 :as c]
        [dadysql.core]
        [dadysql.compiler.util])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))



(deftest map-name-model-sql-test
  (testing "test name map sql "
    (let [w {:name  [:gen-dept :gen-empl :gen-meet],
             :model [:dept :empl :meet]
             :sql
                    ["call next value for seq_dept"
                     "call next value for seq_empl"
                     "call next value for seq_meet"]}
          actual-result (map-name-model-sql w)]
      (is (not-empty actual-result))))

  (testing "test name map sql "
    (let [w {:name [:gen-dept :gen-empl :gen-meet],
             :sql
                   ["call next value for seq_dept"
                    "call next value for seq_empl"
                    "call next value for seq_meet"]}
          actual-result (map-name-model-sql w)]
      (is (not-empty actual-result))))

  (testing "test name map sql "
    (let [w {:name :gen-dept,
             :sql  ["select * from dual "]
             }
          actual-result (map-name-model-sql w)]
      (is (not-empty actual-result)))))
