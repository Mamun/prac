(ns dadysql.file-reader-test
  (:use [clojure.test]
        [dadysql.workflow-exec]
        [dadysql.file-reader])
  (:require [dadysql.workflow-exec :as tie]))


(deftest map-sql-tag-test
  (testing "test map-sql-tag"
    (let [v [{:a 2} "select * from tab;select * from tab3"]
          actual-result (map-sql-tag v)
          expected-result (list {:a 2 :sql ["select * from tab"
                                            "select * from tab3"]
                                 })]
      (is (= actual-result
             expected-result
             )))))


;(read-file "tie.edn.sql")

(deftest read-file-test
    (testing "test read-file "
      (let [t (read-file "tie.edn.sql" )]
        ;(clojure.pprint/pprint t)
        (is (not (nil? t))))))

;(read-file-test)




