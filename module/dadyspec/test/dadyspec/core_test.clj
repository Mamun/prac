(ns dadyspec.core-test
  (:use [clojure.test]
        [dadyspec.core])
  (:require [clojure.spec.test :as stest]
            [clojure.spec :as s]
            [cheshire.core :as ch]
            [clojure.spec.gen :as gen])
  (:import (java.util Date UUID)))


(deftest relation-merge-test
  (testing "testing relation merge"
    (let [w (relation-merge :hello [[:a :dadyspec.core/rel-one-one :b]
                                    [:a :dadyspec.core/rel-one-one :c]])]
      (is (= w
             [`(clojure.spec/merge
                 :hello/a
                 (clojure.spec/keys :opt [:hello/b-list :hello/c-list]))])))))


(deftest gen-spec-test
  (testing "gen spec test "
    (let [v (gen-spec :app '{:dept    {:req {:name string?
                                             :date inst?}
                                       :opt {:note string?}}
                             :student {:req {:name string?
                                             :dob  inst?}}}
                      :join
                      [[:dept :dadyspec.core/rel-one-one :student]])]
      (is (not-empty v)))))



(deftest check-exec-test
  (testing "test generate spec "
    (do
      (defsp test {:dept    {:req {:id int?}
                             :opt {:note string?}}
                   :student {:req {:name string?
                                   :id   int?}}}
             :join [[:dept :dadyspec.core/rel-one-one :student]])
      (is (s/valid? :test/dept {:test.dept/id 123}))
      (is (s/valid? :test/dept {:test.dept/id      123
                                :test/student-list [{:test.student/id   23
                                                     :test.student/name "asdf"}]}))
      (is (s/valid? :test-un/dept {:id 123}))
      (is (s/valid? :test-ex/dept {:id "123"})))))




(comment

  (gen-spec :app '{:dept    {:req {:id   int?
                                   :name string?}
                             :opt {:note string?}}
                   :student {:req {:name string?
                                   :id   int}}}
            :join
            [[:dept :dadyspec.core/rel-one-many :student]])


  (defsp app {:dept    {:req {:id   int?
                              :name string?}
                        :opt {:note string?}}
              :student {:req {:name string?
                              :id   int?}}}
         :join
         [[:dept :dadyspec.core/rel-one-many :student]])


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :app-un.spec/dept 1)))

  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :app-un/dept 1)))


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :app-un/student 1)))


  ;; Convert to json and back from json again with confrom
  (binding [s/*recursion-limit* 0]
    (let [v (first (gen/sample (s/gen :app-un/student) 1))
          v (update v :id str)                              ;;convert int to string
          v-str (ch/generate-string v)]
      (conform-json :app-ex/student v-str)))


  (binding [s/*recursion-limit* 0]
    (let [v (first (gen/sample (s/gen :app-un/student) 1))]
      (s/conform :app/student v)))

  ;(s/form :app/student)
  ;(s/form :app/dept)

  )





(comment




  ;(s/valid? ::join [[:dept ::one-many :student]])

  (stest/instrument `gen-spec)

  (s/exercise-fn `gen-spec)

  (stest/check `gen-spec)

  (s/exercise :dadyspec.core/model 1)

  (s/exercise :dadyspec.core/req-or-opt 2)

  (s/exercise (s/keys :req [:dadyspec.core/req]))

  (s/exercise :dadyspec.core/req-or-opt 1)

  (s/valid? :dadyspec.core/req {})

  (s/valid? :dadyspec.core/req-or-opt {:req nil :opt nil})

  ;(s/exercise (s/map-of #{:a :b} #{1 2} ) )


  (let [model {:dept    {:req {:name 'string?
                               :id   'int?}}
               :student {:req {:name 'string?
                               :id   'int?}}}
        rel [[:dept :dadyspec.core/one-many :student]]]
    ;(s/explain-data ::input [:model model rel ] )
    (gen-spec :model model rel))


  (let [model {:dept {:opt {:name 'string?
                            :date 'inst?}}}
        rel [[:dept :dadyspec.core/one-many :student]]]
    ;(s/explain-data ::input [:model model rel ] )
    (gen-spec :model model rel))


  (defsp model3 {:dept {:opt {:name string?
                              :date inst?}}}
         [[:dept :dadyspec.core/one-many :student]])

  (x-inst? "2015-12-12")
  (x-inst? "2015-45")

  (s/explain :model3-ex/dept {:name "asdf" :date "2asdf"})


  (clojure.pprint/pprint
    (s/exercise ::join 3))


  (clojure.pprint/pprint
    (s/exercise ::input 1))

  )

(comment


  (clojure.spec/def :app.dept/name string?)
  (clojure.spec/def :app.dept/id int?)
  (clojure.spec/def :app.dept/note string?)
  (clojure.spec/def :app/dept
    (clojure.spec/keys :req-un [:app.dept/id :app.dept/name]
                       :opt-un [:app.dept/note]))



  #_(clojure.pprint/pprint
      (s/exercise :app.spec/dept 1))


  #_(clojure.pprint/pprint
      (s/exercise :app/dept 1))


  (clojure.spec/def :app.student/id int?)
  (clojure.spec/def :app.student/name string?)
  (clojure.spec/def :app/student
    (clojure.spec/keys :req-un [:app.student/name :app.student/id]))


  #_(clojure.pprint/pprint
      (s/exercise :app.spec/student 1))



  (clojure.spec/def :app.spec/dept    (clojure.spec/keys :req-un [:app/dept]))
  (clojure.spec/def :app.spec/student (clojure.spec/keys :req-un [:app/student]))
  (s/def :app/spec (s/or :app.spec/dept :app.spec/dept
                         :app.spec/student :app.spec/student ))



  #_(s/conform :app/spec {:student {:name "", :id -1}})
  #_(s/explain :app/spec {:dept {:name "", :id -1}})








  )

