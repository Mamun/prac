(ns dadyspec.core-impl-test
  (:use [clojure.test]
        [dadyspec.core-impl])
  (:require [clojure.spec.test :as stest]
            [clojure.spec :as s]))


(deftest update-model-key-test
  (testing "testing gen-spec "
    (is (= #:a {:b {:req #:a.b {:id 3}, :opt #:a.b {:re :f}}}
           (rename-model-key-to-namespace-key {:b {:req {:id 3}
                                                   :opt {:re :f}}} :a)))))


(deftest model->spec-test
  (testing "spec builder test  "
    (is (= (model->spec :hello {:a {:opt {:id :a}}} {})
           `((clojure.spec/def :hello.a/id :a)
              (clojure.spec/def :hello/a
                (clojure.spec/merge (clojure.spec/keys :opt [:hello.a/id])
                                    (clojure.spec/map-of #{:hello.a/id} clojure.core/any?)))
              (clojure.spec/def :hello/a-list
                (clojure.spec/coll-of :hello/a :kind clojure.core/vector?))))))
  (testing "spec builder test  "
    (is (= (model->spec :hello {:a {:opt {:id :a}}} {:qualified? false})
           `((clojure.spec/def :hello.a/id :a)
              (clojure.spec/def :hello/a
                (clojure.spec/merge (clojure.spec/keys :opt-un [:hello.a/id])
                                    (clojure.spec/map-of #{:id} clojure.core/any?)))
              (clojure.spec/def :hello/a-list
                (clojure.spec/coll-of :hello/a :kind clojure.core/vector?)))))))




(comment

  (clojure.pprint/pprint
    (model->spec {:a {:opt {:id :a}}} :hello {:qualified? false})
    )


  (update-model-key-test)

  (spec-builder-test)

  )



