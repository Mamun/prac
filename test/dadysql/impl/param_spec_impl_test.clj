(ns dadysql.impl.param-spec-impl-test
  (:use [dadysql.impl.param-spec-impl]
        [clojure.test])
  (:require [clojure.spec :as s]
            [clojure.walk :as w]))


#_(deftest create-ns-key-test
  (testing "testing add-ns-to-keyword "
    (are [a e] (= a e)
               (create-ns-key :a/b :hello) :a.b/hello
               (create-ns-key :hello :t) :hello/t
               (create-ns-key :t :p) :t/p)))

;(create-ns-key-test)




#_(deftest convert-key-to-ns-key-test
  (testing "testing as-ns-key-format"
    (are [a e] (= a e)
               (convert-key-to-ns-key {:get-by-id {:id   :a
                                                   :name :name}})
               {:get-by-id {:get-by-id/id   :a,
                            :get-by-id/name :name}}

               (convert-key-to-ns-key {:get-by-id         {:id   :id
                                                           :name :name}
                                       :get-details-by-id {:id :id}})
               {:get-by-id
                {:get-by-id/id   :id
                 :get-by-id/name :name}
                :get-details-by-id
                {:get-details-by-id/id :id}})))


;(update-ns-key-test)


#_(deftest map->spec-test
  (testing "testing map->spec "
    (let [v {:get-by-id   {:id :id}
             :get-by-name {:name :name}}
          e-result '((clojure.spec/def
                       :emp/get-by-id
                       (clojure.spec/keys :req-un [:emp.get-by-id/id]))
                      (clojure.spec/def :emp.get-by-id/id :id)
                      (clojure.spec/def
                        :emp/get-by-name
                        (clojure.spec/keys :req-un [:emp.get-by-name/name]))
                      (clojure.spec/def :emp.get-by-name/name :name))
          a-result (map->spec :emp v)]
      (is (= e-result a-result)))))









;(map->spec-test)



(comment


  (map->spec :t {:person {:name 'string?}
                 :credit {:id 'int?}})

  (eval-spec (map->spec :t {:person {:name string?}
                            :credit {:id int?}}))


  (s/explain :t/person {:t.person/name "asdf"} )

  (s/explain :t/person-list [{:t.person/name "asdf"}])


  (s/explain :t/person-un {:name "asdf"} )



  ;(s/explain :t/hello-list [{:a 12}] )

  (s/valid?
    (merge-spec (list :hello3.get-by-id/spec :hello3.get-details-by-id/spec))
    {:id 3 :name "asdf"})


  (s/valid?
    (merge-spec (list :hello3.get-by-id/spec :hello3.get-details-by-id/spec))
    {:id 3 :name 3})


  (let [p {:person {:fname string?
                    :lname string?} } ])


  (s/def :person/fname string?)
  (s/def :person/lname string?)
  (s/def :credit/id int?)
  (s/def :credit/detail (s/keys :req [:credit/id]))
  (s/def :credit/list (s/coll-of :credit/detail :kind vector?))

  (s/explain :credit/detail {:credit/id 23})
  (s/explain :credit/list [{:credit/id 23}])




  (s/def :person/detail (s/keys :req [:person/fname
                                      :person/lname
                                      :credit/detail-list]))


  (s/explain :person/detail {:person/fname       "Hello"
                             :person/lname       "Check"
                             :credit/detail-list [{:credit/id 12}]})


  (s/exercise :person/detail)

  ;Person -> 1:n -> Credit

  ;;Person
  {:person/detail {:person/fname       "Max"
                   :person/lname       "Musterman"
                   :credit/detail-list [{:credit/id 12}]}}

  ;;Credit
  {:credit/detail {:credit/id     12
                   :person/detail {:person/fname "Max"
                                   :person/lname "Musterman"}}}

  )










