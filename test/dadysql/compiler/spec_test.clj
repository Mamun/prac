(ns dadysql.compiler.spec-test
  (:use [clojure.test]
        [dadysql.compiler.test-data]
        [dadysql.compiler.core])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.spec.test :as t]
            [dadysql.compiler.file-reader :as f]))




(deftest tx-prop-spec-test
  (testing "tx-prop spec for valid condtion "
    (let [v [:isolation :serializable :read-only? true]
          r (s/conform :dadysql.compiler.spec/tx-prop v)]
      (is (not= ::s/invalid r))))

  (testing "tx-prop spec for valid condtion "
    (let [v [:isolation :serializable :read-only? 1]
          r (s/conform :dadysql.compiler.spec/tx-prop v)]
      (is (= ::s/invalid r))))
  (testing "tx-prop spec for valid condtion "
    (let [v [:isolation :serializable1 :read-only? true]
          r (s/conform :dadysql.compiler.spec/tx-prop v)]
      (is (= ::s/invalid r)))))


;(tx-prop-spec-test)

(deftest validation-spec-test
  (testing "test validation spec "
    (let [v [[:id :type 'vector? "Id will be sequence"]
             [:id :contain 'int? "Id contain will be Long "]]
          r (s/conform :dadysql.compiler.spec/validation v)]
      (is (not= :clojure.spec/invalid r)))))


;(validation-spec-test)


(deftest params-spec-test
  (testing "test params spec "
    (let [v [[:next_transaction_id :ref-fn-key 'inc :transaction_id]]
          r (s/conform :dadysql.compiler.spec/params v)]
      (is (not= :clojure.spec/invalid r))))
  (testing "test params spec for invalid case "
    (let [v [[:next_transaction_id :ref-fn-key 'inc "transaction_id"]]
          r (s/conform :dadysql.compiler.spec/params v)]
      (is (= :clojure.spec/invalid r)))))

#_(gen/sample (gen/fmap (fn [w]
                          (into [] (into #{} w)))
                        (gen/such-that not-empty (s/gen :dadysql.compiler.spec/params))))

;(params-spec-test)
;


(deftest join-spec-test
  (testing "test join spec"
    (let [v [[:department :id :1-n :employee :dept_id]
             [:employee :id :1-1 :employee-detail :employee_id]
             [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]
          r (s/conform :dadysql.compiler.spec/join v)]
      (is (not= :clojure.spec/invalid r))))
  (testing "test join spec for invalid missing n-n key "
    (let [v [[:department :id :1-n :employee :dept_id]
             [:employee :id :1-1 :employee-detail :employee_id]
             [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id]]]
          r (s/conform :dadysql.compiler.spec/join v)]
      (is (= :clojure.spec/invalid r)))))

#_(gen/sample (gen/fmap (fn [w]
                          (into [] (into #{} w)))
                        (gen/such-that not-empty (s/gen :dadysql.compiler.spec/join))))


;(join-spec-test)



(deftest spec-test
  (testing "test do-compile "
    (let [r (s/conform :dadysql.compiler.spec/compiler-input-spec do-compile-input-data)]
      (is (not= :clojure.spec/invalid r)))))


(comment

  ;(s/conform :tie-edn/get-dept-by-id {:id "asdf"})

  (s/valid? :dadysql.compiler.spec/compiler-input-spec do-compile-input-data2)
  )


;(spec-test)


(deftest spec-test2
  (testing "test do compile file "
    (let [w (-> "tie.edn.sql"
                (f/read-file)
                )
          actual-result (s/conform :dadysql.compiler.spec/compiler-input-spec w)]
      ; (clojure.pprint/pprint actual-result)
      (is (not= :clojure.spec/invalid actual-result)))))


;(spec-test2)







;(run-tests)


;(spec-file-test)



#_(comment


  (gen/generate (s/gen :dadysql.compiler.spec/validation))


  (gen/generate (s/gen :dadysql.compiler.spec/extend))

  (gen/generate (s/gen :dadysql.compiler.spec/module))

  (gen/generate (s/gen :dadysql.compiler.spec/global))

  (gen/generate  (s/gen :dadysql.compiler.spec/compiler-input-spec))


  (gen/sample
    (gen/bind (s/gen :dadysql.compiler.spec/vali-type2)
              (fn [v]
                (println v)
                {:a v}

                (s/gen int?)
                )))



  (gen/sample
    (gen/bind (s/gen :dadysql.compiler.spec/params)
              (fn [v]
                (println v)
                {:a v}

                (s/gen int?)
              )))



  

    (s/gen :dadysql.compiler.spec/params)




  (gen/generate
    (gen/bind (s/gen :dadysql.compiler.spec/params) (fn [v]
                                                      (println v)
                                                      [:a]
                                                      )))

  (gen/sample (gen/fmap (fn [w]
                            (into [] (into #{} w)))
                          (gen/such-that not-empty (s/gen :dadysql.compiler.spec/params))))


  (gen/sample (gen/fmap (fn [w]
                          (into [] (into #{} w)))
                        (gen/such-that not-empty (s/gen :dadysql.compiler.spec/validation))))


  (gen/sample (gen/fmap (fn [w]
                            (into [] (into #{} w)))
                          (gen/such-that not-empty (s/gen :dadysql.compiler.spec/join))))






  (->> "tie.edn.sql"
         (f/read-file)
         (s/explain :dadysql.compiler.spec/compiler-input-spec)

         )


  (gen/generate (s/gen :dadysql.compiler.spec/extend) )




  (gen/generate (s/gen :dadysql.compiler.spec/compiler-input-spec))

  )



#_(do-compile-file-test)









