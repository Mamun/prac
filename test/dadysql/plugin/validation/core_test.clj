(ns dadysql.plugin.validation.core-test
  (:use [clojure.test]
        [dady.proto])
  (:require [dadysql.plugin.validation.core :refer :all]
            [dady.fail :refer :all]

            [dadysql.constant :refer :all]))


(defn get-proc []
  (map->ValidationKey {:cname  validation-key
                       :corder 1
                       :ccoll  (new-child-coll)}))


#_(deftest validation-key-spec-test
  (testing "test validation key spec "
    (let [coll {validation-key [[:id validation-contain-key 'long? "error"]
                                [:id validation-type-key    'sequential? "error"]]}
          proc (get-proc)
          result (s/validate (eval (spec proc)) coll)]
      (is (= coll result)))))



#_(let [spec (spec (get-proc))]
  (clojure.pprint/pprint spec)
  (s/validate (eval spec) {validation-key [[:id validation-contain-key 'long? "error"]
                                           [:id validation-type-key    'sequential? "error"]
                                           ]})
  )



;(validation-key-spec-test)


(deftest validation-key-impl-test
  (testing "test validation-key-impl "
    (let [coll {validation-key [[:id validation-contain-key long? "error"]
                                [:id validation-type-key vector? "error"]]
                input-key      [{:id 3}]
                sql-key        ["select * from tab " :id]}
          actul-result (-process (get-proc) coll)]
      (is (failed? actul-result))))
  (testing "test validation-key-process "
    (let [coll {validation-key [[:id validation-contain-key long? "error"]
                                [:id validation-type-key vector? "error"]]
                input-key      {:id 3}
                sql-key        ["select * from tab " :id]}
          actul-result (-process (get-proc) coll)]
      (is (failed? actul-result))))
  (testing "test validation-key-process "
    (let [coll {validation-key [[:id validation-contain-key long? "error"]
                                [:id validation-type-key vector? "error"]]
                input-key      {:id [1 2]}
                sql-key        ["select * from tab " :id]}
          actul-result (-process (get-proc) coll)]
      (is (not (failed? actul-result))))))


;(validation-key-impl-test)




#_(deftest validation-compile-test
  (testing "test validation-compile"
    (let [v-coll [[:id :type (read-string "Long") " Id contain will be Long"]]
          p (get-proc)
          actual-result (-emit
                          p
                          v-coll)
          expected-result [[:id :type java.lang.Long " Id contain will be Long"]]]
      (is (= expected-result
             actual-result)))))

;(validation-compile-test)



