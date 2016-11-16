(ns dadyspec.core-test
  (:use [clojure.test]
        [dadyspec.core])
  (:require [clojure.spec.test :as stest]
            [clojure.spec :as s]))





;(gen-spec-test)



(comment


  (interpose "\n"

    (gen-spec :app '{:dept    {:req {:name string?
                                     :date inst?}
                               :opt {:note string?}}
                     :student {:req {:name string?
                                     :dob  inst?}}}
              :join
              [[:dept :dadyspec.core/one-many :student]])


    )



  (s/valid? ::email? "a.dsfas@test.de")

  (s/exercise ::email?)

  )


(comment




  (s/exercise :dadyspec.core/req-p 2)
  (s/exercise :dadyspec.core/opt-m 2)
  (s/exercise :dadyspec.core/model 2)



  (s/explain :dadyspec.core/req-p {:req {:id nil }}  )

  (s/explain :dadyspec.core/model {:hello {:req n
                                           :opt {:id 'int?}}}   )


  (s/explain :dadyspec.core/model {:hello {:req {:id 'int?}
                                           :opt1 {:id 'int?}}}   )


  (s/explain :dadyspec.core/model {}  )

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

  (s/valid? :dadyspec.core/req {} )

  (s/valid? :dadyspec.core/req-or-opt {:req nil :opt nil} )

  ;(s/exercise (s/map-of #{:a :b} #{1 2} ) )


  (let [model {:dept    {:req {:name 'string?
                               :id   'int?}}
               :student {:req {:name 'string?
                               :id   'int?}}}
        rel [[:dept :dadyspec.core/one-many :student]]]
    ;(s/explain-data ::input [:model model rel ] )
    (gen-spec :model model rel))


  (let [model {:dept    {:opt {:name 'string?
                               :date 'inst?}}}
        rel [[:dept :dadyspec.core/one-many :student]]]
    ;(s/explain-data ::input [:model model rel ] )
    (gen-spec :model model rel))


  (defsp model3 {:dept    {:opt {:name string?
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
