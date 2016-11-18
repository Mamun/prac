(ns dadyspec.core-impl-test
  (:use [clojure.test]
        [dadyspec.core-impl]
        [dadyspec.util])
  (:require [clojure.spec.test :as stest]
            [clojure.spec :as s]))


#_(deftest update-model-key-test
    (testing "testing gen-spec "
      (is (= #:a {:b {:req #:a.b {:id 3}, :opt #:a.b {:re :f}}}
             (rename-model-key-to-namespace-key {:b {:req {:id 3}
                                                     :opt {:re :f}}} :a)))))


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
              (clojure.spec/def :hello/spec (clojure.spec/or :hello.spec/a :hello.spec/a :hello.spec/a-list :hello.spec/a-list)))

           ))))


(defmacro hello [r]
  (clojure.walk/postwalk (fn [w]
                           (when (symbol? w)
                             (println w)
                             (println (resolve w))
                             )
                           w
                           )r))


(comment


  (def my-spec )

  (s/valid? (s/spec int?  ) 34)

  (macroexpand-1 '(hello (s/int-in 10 10)))

  (macroexpand-1 '(s/int-in 10 10))

  (clojure.pprint/pprint
    (model->spec :hello {:a {:opt {:id :a}}} {:qualified? false})
    )


  (update-model-key-test)

  (model->spec-test)





  (s/def ::roll (s/int-in 0 11))
  (s/valid? ::roll 12)


  )



