(ns dadymodel.spec-generator-test
  (:use [clojure.test]
        [dadymodel.spec-generator]
        [dadymodel.util])
  (:require [clojure.spec.test :as stest]
            [clojure.spec :as s]))


(comment
  (run-tests)

  )

(deftest model->spec-test
  (testing "spec builder test  "
    (is (= (model->spec {:dadymodel.core/gen-type         :dadymodel.core/un-qualified
                         :dadymodel.core/ns-identifier    :app
                         :dadymodel.core/entity-identifer :entity
                         :dadymodel.core/prefix           :ex}
                        {:student {:opt {:id :a}}})
           `((clojure.spec/def :ex.app.student/id :a)
              (clojure.spec/def :ex.app/student (clojure.spec/keys :req-un [] :opt-un [:ex.app.student/id]))
              (clojure.spec/def :ex.app/student-list (clojure.spec/coll-of :ex.app/student :kind clojure.core/vector?))
              (clojure.spec/def :ex.entity.app/student (clojure.spec/keys :req-un [:ex.app/student]))
              (clojure.spec/def :ex.entity.app/student-list (clojure.spec/coll-of :ex.entity.app/student :kind clojure.core/vector?))
              (clojure.spec/def
                :ex.app/model
                (clojure.spec/or
                  :ex.app/student
                  :ex.app/student
                  :ex.app/student-list
                  :ex.app/student-list
                  :ex.entity.app/student
                  :ex.entity.app/student
                  :ex.entity.app/student-list
                  :ex.entity.app/student-list))))))
  (testing "spec builder test  "
    (is (= (model->spec {:dadymodel.core/gen-type         :dadymodel.core/qualified
                         :dadymodel.core/ns-identifier    :app
                         :dadymodel.core/entity-identifer :entity}
                        {:student {:opt {:id :a}}})
           `((clojure.spec/def :app.student/id :a)
              (clojure.spec/def
                :app/student
                (clojure.spec/merge
                  (clojure.spec/keys :req [] :opt [:app.student/id])
                  (clojure.spec/map-of #{:app.student/id} clojure.core/any?)))
              (clojure.spec/def :app/student-list (clojure.spec/coll-of :app/student :kind clojure.core/vector?))
              (clojure.spec/def :entity.app/student (clojure.spec/keys :req [:app/student]))
              (clojure.spec/def :entity.app/student-list (clojure.spec/coll-of :entity.app/student :kind clojure.core/vector?))
              (clojure.spec/def
                :app/model
                (clojure.spec/or
                  :app/student
                  :app/student
                  :app/student-list
                  :app/student-list
                  :entity.app/student
                  :entity.app/student
                  :entity.app/student-list
                  :entity.app/student-list)))
           )))
  (testing "spec gen test "
    (is (= (model->spec {:dadymodel.core/gen-type         :dadymodel.core/un-qualified
                         :dadymodel.core/ns-identifier    :app
                         :dadymodel.core/join             [[:dept :id :dadymodel.core/rel-1-n :student :dept-id]]
                         :dadymodel.core/entity-identifer :entity
                         :dadymodel.core/prefix           :unq}
                        {:dept {:opt {:id :a}}})
           '((clojure.spec/def :unq.app.dept/id :a)
              (clojure.spec/def :unq.app/dept (clojure.spec/keys :req-un [] :opt-un [:unq.app/student-list :unq.app.dept/id]))
              (clojure.spec/def :unq.app/dept-list (clojure.spec/coll-of :unq.app/dept :kind clojure.core/vector?))
              (clojure.spec/def :unq.entity.app/dept (clojure.spec/keys :req-un [:unq.app/dept]))
              (clojure.spec/def :unq.entity.app/dept-list (clojure.spec/coll-of :unq.entity.app/dept :kind clojure.core/vector?))
              (clojure.spec/def
                :unq.app/model
                (clojure.spec/or
                  :unq.app/dept
                  :unq.app/dept
                  :unq.app/dept-list
                  :unq.app/dept-list
                  :unq.entity.app/dept
                  :unq.entity.app/dept
                  :unq.entity.app/dept-list
                  :unq.entity.app/dept-list)))))))



(comment

  (model->spec-test)
  )



