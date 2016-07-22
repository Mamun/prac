(ns dadysql.compiler.core-test
  (:use [clojure.test]
        [dadysql.compiler.core :as r]
        [dadysql.core2 :as c]
        [dadysql.core]
        [dady.common]
        [dadysql.compiler.test-data])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))



(deftest compiler-merge-test
  (testing "test compiler-merge "
    (let [v [{:dadysql.core/param      [[1 2 3]]
              :clojure.core/column     {:p 1}
              :dadaysql.core/timeout    4
              :dadaysql.core/result     #{result-single-key}
              :dadysql.core/param-spec :a/b}
             {:dadysql.core/param   [[8 9 0]]
              :dadaysql.core/timeout 6}
             {:dadysql.core/param      [[5 6 7]]
              :clojure.core/column     {:p  4
                              :p1 :p}
              :p             9
              :dadaysql.core/result     #{result-array-key}
              :dadysql.core/param-spec :a/b}]
          expected-result {:dadysql.core/param   [[5 6 7] [8 9 0] [1 2 3]],
                           :clojure.core/column  {:p 4, :p1 :p}
                           :dadaysql.core/timeout 6
                           :dadaysql.core/result  #{result-array-key}
                           :dadysql.core/param-spec :a/b
                           :p          9}
          actual-result (apply merge-with compiler-merge v)]
      ;(clojure.pprint/pprint actual-result)
      (is (= actual-result
             expected-result
             )))))


;(compiler-merge-test)

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
      (is (not (empty? actual-result))))))


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




