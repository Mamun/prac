(ns dadyspec.walkthrough
  (:use [dadyspec.core])
  (:require [clojure.spec.test :as stest]
            [cheshire.core :as ch]
            [clojure.spec :as s]))


(comment

  ;;Example 1

  (defsp app {:author {:req {:id int?}}})
  (gen-spec :app '{:author {:req {:id int?}}})


  (s/explain :app/author {:app.author/id 23})
  (s/explain :app-un/author {:id 23})
  (s/explain :app-ex/author {:id "23"})

  ;;Example 2

  (defsp app {:dept    {:req {:name string?
                              :date inst?}
                        :opt {:note string?}}
              :student {:req {:name string?
                              :dob  inst?}}}
         [[:dept :dadyspec.core/one-many :student]])

  (gen-spec :app '{:dept    {:req {:name string?
                                   :date inst?}
                             :opt {:note string?}}
                   :student {:req {:name string?
                                   :dob  inst?}}}
            [[:dept :dadyspec.core/one-many :student]])

  (s/exercise :app/dept)


  ;; Example

  (defsp app {:company {:req {:name string?
                              :id int?
                              :type (s/coll-of (s/and keyword? #{:software :hardware})
                                               :into #{})}} })

  (gen-spec :app '{:company {:req {:name string?
                                   :id int?
                                   :type (s/coll-of (s/and keyword? #{:software :hardware})
                                                   :into #{})}} })

  (s/conform :app/company    {:app.company/id 123 :app.company/name "Web De" :app.company/type [:software]})
  (s/conform :app-un/company {:id 123 :name "Web De" :type [:software]})
  (s/conform :app-ex/company {:id "123" :name "Web De" :type ["software" "hardware"]})




  )


(comment





  (defsp app {:author {:req {:name string?
                              :type (s/coll-of (s/and keyword? #{:clj :cljs})
                                               :into #{})}} })


  (gen-spec :app '{:author {:req {:name string?
                                 :type (s/coll-of (s/and keyword? #{:clj :cljs})
                                                  :into #{})}} })

  (s/conform :app/author {:name "asdf" :type [:cljs]})

  (s/conform :app-ex/author {:name "asdf" :type ["cljs"]} )


  (ch/decode "{\"myarray\":[2,3,3,2],\"myset\":[\"ad\"]}" true
          (fn [field-name]
            (if (= field-name "myset")
              #{}
              [])))



  (gen-spec :model {:dept    {:opt {:name 'string?
                                    :date 'inst?}}
                    }
            []
            )

  (macroexpand-1 '(defsp app {:dept    {:opt {:name string?
                                              :date inst?}}
                              :student {:opt {:name string?
                                              :dob  inst?}}}
                         [[:dept :dadyspec.core/one-many :student]]))


  (defsp app {:dept    {:req {:name string?
                              :date inst?}
                        :opt {:desc string?}}
              :student {:req {:name string?
                              :dob  inst?}}}
         [;[:dept :dadyspec.core/one-many :student]
          [:student :dadyspec.core/many-one :dept]
          ])
  (s/exercise :app/dept 1)

  (clojure.pprint/pprint
    (s/exercise :app/student 1))


  (clojure.pprint/pprint
    (s/exercise :app/student-list 1))




  (ch/parse-string "{\"foo\":\"123\"}")

  )