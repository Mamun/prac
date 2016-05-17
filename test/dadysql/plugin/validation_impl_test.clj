(ns dadysql.plugin.validation-impl-test
  (:use [clojure.test]
        [dady.node-proto])
  (:require [dadysql.plugin.validation-impl :refer :all]
            [dady.common :refer :all]
            [dadysql.common :refer :all]
            [schema.core :as s]))


(defn get-proc []
  (map->ValidationKey {:cname  validation-key
                       :corder 1
                       :ccoll  (new-child-coll)}))


#_(deftest validation-key-impl-test
    (testing "test validation-key-impl "
      (let [coll {validation-key [[:id validation-contain-key Long "error"]
                                  [:id validation-type-key [] "error"]]
                  input-key      [{:id 3}]
                  sql-key        ["select * from tab " :id]}
            actul-result (node-process (get-proc) nil coll)]
        (is (-failed? actul-result))))
    (testing "test validation-key-process "
      (let [coll {validation-key [[:id validation-contain-key Long "error"]
                                  [:id validation-type-key [] "error"]]
                  input-key      {:id 3}
                  sql-key        ["select * from tab " :id]}
            actul-result (node-process (get-proc) nil coll)]
        (is (-failed? actul-result))))
    (testing "test validation-key-process "
      (let [coll {validation-key [[:id validation-contain-key Long "error"]
                                  [:id validation-type-key (type []) "error"]]
                  input-key      {:id [1 2]}
                  sql-key        ["select * from tab " :id]}
            actul-result (node-process (get-proc) nil coll)]
        (is (not (-failed? actul-result))))))


;(validation-key-impl-test)

(comment
  (deftest validation-compile-test
    (testing "test validation-compile"
      (let [v-coll [[:id :type (read-string "Long") " Id contain will be Long"]]
            p-seq ["seekct" :id]
            actual-result (validation-compile
                            (validation-key-child)
                            v-coll
                            p-seq)
            expected-result [[:id :type java.lang.Long " Id contain will be Long"]]]
        (is (= expected-result
               actual-result))

        ))))

;(validation-compile-test)



