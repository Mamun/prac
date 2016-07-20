(ns dadysql.compiler.core-test
  (:use [clojure.test]
        [dadysql.compiler.core :as r]
        [dadysql.core :as c]
        [dadysql.constant]
        [dady.common]
        [dadysql.compiler.test-data])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))



(deftest compiler-merge-test
  (testing "test compiler-merge "
    (let [v [{param-key      [[1 2 3]]
              column-key     {:p 1}
              timeout-key    4
              result-key     #{result-single-key}
              validation-key [[:id :type 'int? "id will be long"]]}
             {param-key   [[8 9 0]]
              timeout-key 6}
             {param-key      [[5 6 7]]
              column-key     {:p  4
                              :p1 :p}
              :p             9
              result-key     #{result-array-key}
              validation-key [[:id :type 'vector? "id will be sequence"]
                              [:id :contain 'int? "id contain will be int "]]}]
          expected-result {param-key   [[5 6 7] [8 9 0] [1 2 3]],
                           column-key  {:p 4, :p1 :p}
                           timeout-key 6
                           result-key  #{result-array-key}
                           :p          9}
          actual-result (apply merge-with compiler-merge v)]
      ;(clojure.pprint/pprint actual-result)
      (is (= expected-result
             actual-result)))))


;(compiler-merge-test)


;(apply-compile-test)

;(resolve 'long?)

;(map-name-model-sql-test)

;(declare compile-one-data)
;(declare compile-one-expected-result)




(deftest compile-one-test
  (testing "test compile-one "
    (let [config (r/default-config)


          actual-result (r/compile-one compile-one-data config)]

      (is (= actual-result
             compile-one-expected-result))))
  (testing "test compile-one"
    (let [config (r/default-config)
          w {:doc  "Modify department"
             :name [:insert-dept :update-dept :delete-dept]
             :sql  ["call next value for seq_dept"
                    "call next value for seq_empl"
                    "call next value for seq_meet"]}
          actual-result (->> (compile-one w config))]
      (is (not (empty? actual-result)))))
  )



;(compile-one-test)


;(declare do-compile-input-data)
;(declare do-compile-expected-result)


(deftest do-compile-test
  (testing "test do-compile "
    (let [actual-result (r/do-compile do-compile-input-data)]
      ; (clojure.pprint/pprint actual-result)
      (is (not-empty actual-result))
      (is (= do-compile-expected-result actual-result))
      (is (not-empty (:get-dept-list actual-result)))
      (is (not-empty (:get-dept-by-ids actual-result)))
      (is (not-empty (:get-employee-list actual-result))))))


(comment

  (r/do-compile do-compile-input-data2)
  )


;(do-compile-test)




;(do-compile-test)

;(run-tests)




;(do-compile2-test)



;(do-compile-test2)




;(run-tests)


;(compilter-test)



#_(deftest do-compile4-test
  (testing "test do -compile"
    (let [w (r/read-file "tie.edn2.sql")]
      ;(clojure.pprint/pprint w)
      )
    ))


(comment

  (do
    (do-compile4-test)
    nil)

  )




