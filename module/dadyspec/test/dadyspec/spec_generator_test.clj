(ns dadyspec.spec-generator-test
  (:use [clojure.test]
        [dadyspec.spec-generator]
        [dadyspec.util])
  (:require [clojure.spec.test :as stest]
            [clojure.spec :as s]))


(deftest model->spec-test
  (testing "spec builder test  "
    (is (= (model->spec :app {:student {:opt {:id :a}}}
                        {:dadyspec.core/gen-type :dadyspec.core/un-qualified
                         :postfix                "ex-"})
           `((clojure.spec/def :ex-app.student/id :a)
              (clojure.spec/def :ex-app/student (clojure.spec/keys :req-un [] :opt-un [:ex-app.student/id]))
              (clojure.spec/def :ex-app/student-list (clojure.spec/coll-of :ex-app/student :kind clojure.core/vector?))
              (clojure.spec/def :entity.ex-app/student (clojure.spec/keys :req-un [:ex-app/student]))
              (clojure.spec/def :entity.ex-app/student-list (clojure.spec/coll-of :entity.ex-app/student :kind clojure.core/vector?))
              (clojure.spec/def
                :ex-app/entity
                (clojure.spec/or
                  :entity.ex-app/student
                  :entity.ex-app/student
                  :entity.ex-app/student-list
                  :entity.ex-app/student-list))))))
  (testing "spec builder test  "
    (is (= (model->spec :app {:student {:opt {:id :a}}}
                        {:dadyspec.core/gen-type :dadyspec.core/qualified
                         })
           `((clojure.spec/def :app.student/id :a)
              (clojure.spec/def :app/student (clojure.spec/keys :req [] :opt [:app.student/id]))
              (clojure.spec/def :app/student-list (clojure.spec/coll-of :app/student :kind clojure.core/vector?))
              (clojure.spec/def :entity.app/student (clojure.spec/keys :req [:app/student]))
              (clojure.spec/def :entity.app/student-list (clojure.spec/coll-of :entity.app/student :kind clojure.core/vector?))
              (clojure.spec/def
                :app/entity
                (clojure.spec/or :entity.app/student :entity.app/student :entity.app/student-list :entity.app/student-list)))
           )))
  (testing "spec gen test "
    (is (= (model->spec :app
                        {:dept {:opt {:id :a}}}
                        {:dadyspec.core/gen-type :dadyspec.core/un-qualified
                         :dadyspec.core/join     [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]]
                         :postfix                "un-"})
           '((clojure.spec/def :un-app.dept/id :a)
              (clojure.spec/def :un-app/dept (clojure.spec/keys :req-un [] :opt-un [:un-app/student-list :un-app.dept/id]))
              (clojure.spec/def :un-app/dept-list (clojure.spec/coll-of :un-app/dept :kind clojure.core/vector?))
              (clojure.spec/def :entity.un-app/dept (clojure.spec/keys :req-un [:un-app/dept]))
              (clojure.spec/def :entity.un-app/dept-list (clojure.spec/coll-of :entity.un-app/dept :kind clojure.core/vector?))
              (clojure.spec/def
                :un-app/entity
                (clojure.spec/or :entity.un-app/dept :entity.un-app/dept :entity.un-app/dept-list :entity.un-app/dept-list)))
           ))
    )

  )



(comment

  (model->spec-test)
  )



