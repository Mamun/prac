(ns dadysql.compiler.spec-test
  (:use [clojure.test]
        [dadysql.compiler.test-data]
        [dadysql.compiler.core])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.spec.test :as t]
            [dadysql.compiler.file-reader :as f]))

(s/def ::id (s/and string?
                   #(clojure.string/starts-with?  % "Foo")
                   #(do (println %) true )))

(defn foo-gen []
  (->> (s/gen (s/int-in 1 10))
       (gen/fmap (fn [v] (str "Foo" v)))))


(s/exercise ::id 10 {::id foo-gen})

;(gen/generate (s/spec ::id :gen foo-gen) )


(s/def ::lookup (s/map-of keyword? string?) )


(s/exercise ::lookup)



(deftest tx-prop-spec-test
  (testing "tx-prop spec for valid condtion "
    (let [v [:isolation :serializable :read-only? true]
          r (s/conform :dadysql.core/tx-prop v)]
      (is (not= ::s/invalid r))))

  (testing "tx-prop spec for valid condtion "
    (let [v [:isolation :serializable :read-only? 1]
          r (s/conform :dadysql.core/tx-prop v)]
      (is (= ::s/invalid r))))
  (testing "tx-prop spec for valid condtion "
    (let [v [:isolation :serializable1 :read-only? true]
          r (s/conform :dadysql.core/tx-prop v)]
      (is (= ::s/invalid r)))))


;(tx-prop-spec-test)



(deftest params-spec-test
  (testing "test params spec "
    (let [v [[:next_transaction_id :dadysql.core/param-ref-fn-key inc :transaction_id]]
          r (s/conform :dadysql.core/param-coll v)]

      (is (not= :clojure.spec/invalid r))))
  (testing "test params spec for invalid case "
    (let [v [[:next_transaction_id :dadysql.core/param-ref-fn-key inc "transaction_id"]]
          r (s/conform :dadysql.core/param-coll v)]
      (is (= :clojure.spec/invalid r)))))

#_(gen/sample (gen/fmap (fn [w]
                          (into [] (into #{} w)))
                        (gen/such-that not-empty (s/gen :dadysql.core/param))))

;(params-spec-test)
;

;(run-tests)

;(ifn? inc)

(deftest join-spec-test
  (testing "test join spec"
    (let [v [[:department :id :dadysql.core/join-one-many :employee :dept_id]
             [:employee :id :dadysql.core/join-one-one :employee-detail :employee_id]
             [:employee :id :dadysql.core/join-many-many :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]
          r (s/conform :dadysql.core/join v)]
      (is (not= :clojure.spec/invalid r))))
  (testing "test join spec for invalid missing n-n key "
    (let [v [[:department :id :dadysql.core/join-one-many :employee :dept_id]
             [:employee :id :dadysql.core/join-one-one :employee-detail :employee_id]
             [:employee :id :dadysql.core/join-many-many :meeting :meeting_id [:employee-meeting :employee_id]]]
          r (s/conform :dadysql.core/join v)]
      (is (= :clojure.spec/invalid r)))))

#_(gen/sample (gen/fmap (fn [w]
                          (into [] (into #{} w)))
                        (gen/such-that not-empty (s/gen :dadysql.core/join))))


;(join-spec-test)



(deftest spec-test
  (testing "test do-compile "
    (let [r (s/conform :dadysql.core/compiler-spec do-compile-input-data)]
      (is (not= :clojure.spec/invalid r)))))


;(spec-test)

(comment

  ;(s/explain-str :dadysql.core/compiler-spec do-compile-input-data)

  ;(s/conform :tie-edn/get-dept-by-id {:id "asdf"})


  )


