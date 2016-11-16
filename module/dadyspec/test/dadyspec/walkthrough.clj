(ns dadyspec.walkthrough
  (:use [dadyspec.core])
  (:require [cheshire.core :as ch]
            [clojure.spec :as s]))


(comment

  ;;Example 1

  (defsp app {:author {:req {:id int? :email :dadyspec.core/email?}}})
  (gen-spec :app '{:author {:req {:id int? :email :dadyspec.core/email?}}})


  (s/explain :app/author {:app.author/id 23 :app.author/email "t.test@test.de"})
  (s/explain :app-un/author {:id 23 :email "test@test.de"})
  (s/explain :app-ex/author {:id "23" :email "test@test.de"})


  (s/exercise :app/author)

  ;;Example 2

  (defsp app {:dept    {:req {:name string?
                              :date inst?}
                        :opt {:note string?}}
              :student {:req {:name string?
                              :dob  inst?}}}
         :join
         [[:dept :dadyspec.core/one-many :student]])

  (gen-spec :app '{:dept    {:req {:name string?
                                   :date inst?}
                             :opt {:note string?}}
                   :student {:req {:name string?
                                   :dob  inst?}}}
            :join
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

  (s/conform :app/company    {:app.company/id 123 :app.company/name "Web De" :app.company/type [:software] })
  (s/conform :app-un/company {:id 123 :name "Web De" :type [:software]  })
  (s/conform :app-ex/company {:id "123" :name "Web De" :type ["software" "hardware"]})




  )


