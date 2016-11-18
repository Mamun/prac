(ns dadyspec.core-impl-test
  (:use [clojure.test]
        [dadyspec.core-impl]
        [dadyspec.util])
  (:require [clojure.spec.test :as stest]
            [clojure.spec :as s]))


(deftest model->spec-test
  (testing "spec builder test  "
    (is (= (model->spec :hello {:a {:opt {:id :a}}} {})
           `((clojure.spec/def :hello.a/id :a)
              (clojure.spec/def :hello/a (clojure.spec/keys :req [] :opt [:hello.a/id]))
              (clojure.spec/def :hello/a-list (clojure.spec/coll-of :hello/a :kind clojure.core/vector?))
              (clojure.spec/def :hello.spec/a (clojure.spec/keys :req [:hello/a]))
              (clojure.spec/def :hello.spec/a-list (clojure.spec/coll-of :hello.spec/a :kind clojure.core/vector?))
              (clojure.spec/def :hello/spec (clojure.spec/or :hello.spec/a :hello.spec/a :hello.spec/a-list :hello.spec/a-list))))))
  (testing "spec builder test  "
    (is (= (model->spec :hello {:a {:opt {:id :a}}} {:qualified? false})
           `((clojure.spec/def :hello.a/id :a)
              (clojure.spec/def :hello/a (clojure.spec/keys :req-un [] :opt-un [:hello.a/id]))
              (clojure.spec/def :hello/a-list (clojure.spec/coll-of :hello/a :kind clojure.core/vector?))
              ((clojure.spec/def :hello.spec/a (clojure.spec/keys :req-un [:hello/a]))
                (clojure.spec/def :hello.spec/a-list (clojure.spec/coll-of :hello.spec/a :kind clojure.core/vector?)))
              (clojure.spec/def :hello/spec (clojure.spec/or :hello.spec/a :hello.spec/a :hello.spec/a-list :hello.spec/a-list)))))))






