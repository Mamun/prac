(ns dadysql.compiler.spec-workspace
  (:use [clojure.test]
        [dadysql.compiler.core-old])
  (:require [clojure.spec :as s]
            [clojure.spec.test :as st]
            [clojure.spec.gen :as gen]
            [clojure.core.match :as m]
            [dadysql.compiler.file-reader :as f]))






(comment



  (s/explain
    (s/& (s/* boolean?)  #(even? (count %)))
    ["a" "b"] )

  (s/* string?)

  (s/conform
    (s/and  #(even? (count %)))
    ["a" "v" ])


  (s/explain
    (s/& (s/keys* :opt-un [::even]) (fn [m] (not (contains? m :odd))))
    [:even 4 :odd 5]
    )


  (s/explain
    (s/and (s/keys* :opt-un [::even]) (fn [m] (not (contains? m :odd))))
    [:even 4 :odd 5]
    )


  ;(ranged-rand -9 9)

  (let [s (s/cat :global (s/? (s/keys :req-un  [:dadysql.compiler.spec/name]))
                 :module (s/+ (s/keys :req-un [:dadysql.compiler.spec/name
                                               :dadysql.compiler.spec/sql])))]
    (->>
      [{:name :global
        :tx   [:a]
        }

       {:name :hello
        :sql  "select * from hello"}
       ]
      (s/explain s)
      ))


  (s/def ::list-name (s/with-gen (s/cat :name :dadysql.compiler.spec/name)
                                 (fn []
                                   (s/gen #{:global})
                                   )))


  (gen/generate (s/gen (s/with-gen (s/cat :name :dadysql.compiler.spec/name)
                                   (fn []
                                     (s/gen #{[:global]})
                                     ))))


  (gen/generate (s/gen :dadysql.compiler.spec/name))


  (s/def ::glo (s/with-gen (s/+ (s/& (s/keys :req-un [:dadysql.compiler.spec/name])
                                     (s/spec
                                       (s/and #(= :global (get-in % [:name 1]))
                                              #(do (println %)
                                                   (not (contains? % :sql))))
                                       :gen (fn []
                                              (s/gen #{ {:name :global}  })
                                              )
                                       )))
                           (fn []
                             (s/gen #{ {:name :global}  })
                             )
                           ))


  (gen/generate (s/gen ::glo))

  (let [s (s/cat :global  ::glo #_(s/with-gen
                                     #_(fn []
                                       (s/gen #{[{:name :global}]}))
                                     )
                 ;   :module
                 #_(s/+ (s/keys :req-un [:dadysql.compiler.spec/name
                                               :dadysql.compiler.spec/sql]))
                 )]
    (clojure.pprint/pprint (gen/generate (s/gen s)))
    (->>
      [{:name :global
        :tx   [:a]

        }

       #_{:name :global
        :sql  "select * from hello"}
       ]
      (s/explain s)
      ))

  #_(let [a :1]
    (println a))


  (st/instrument `hello)
  (st/test `hello)



  (st/instrument `do-compile)
  (st/unstrument `do-compile)
  ;(st/v)
  (st/test `do-compile)

  ;(s/conform :dadysql.compiler.spec/result2 #{:single} )


  (st/test
    (gen/generate (s/gen :dadysql.compiler.spec/skip)))

  (clojure.pprint/pprint
    (s/exercise :dadysql.compiler.spec/spec 1))

  (s/conform :dadysql.compiler.spec/spec
    (gen/generate (s/gen :dadysql.compiler.spec/spec)))




  (gen/generate (s/gen :dadysql.compiler.spec/result2))

  (gen/generate (s/gen :dadysql.compiler.spec/result))


  (gen/generate (s/gen :dadysql.compiler.spec/tx-prop))

  (gen/sample (s/gen :dadysql.compiler.spec/tx-prop))

  (gen/sample (s/gen :dadysql.compiler.spec/column))



  ;;sample for params





  (gen/sample (s/gen :dadysql.compiler.spec/join))

  (gen/generate (s/gen :dadysql.compiler.spec/join))


  (s/valid?
    (s/spec (fn [v] false)) 12)


  (s/describe :dadysql.compiler.spec/spec)

  (s/def ::first-name string?)
  (s/def ::last-name string?)

  (s/def ::person (s/keys :req-un [::first-name ::last-name]))

  (s/registry)


  ;(s/valid? :clojure.spec/any "aasdf")

  (s/unform
    ::person {:first-name "asdf"})

  (s/conform ::person {:first-name "asdf"})





  (clojure.pprint/pprint
    (s/form :dadysql.compiler.spec/spec))




  (s/explain ::person {:first-name "asdf"})

  (s/conform ::person {:first-name "asdf"})

  (s/conform
    (eval
      (s/form ::person)) {:first-name "adsf" :last-name "sdfsd"} )


  ;(gen/generate (s/gen ::person))

  (gen/generate (s/gen integer?))

  (gen/sample (s/gen string?))

  (gen/sample (s/gen :dadysql.compiler.spec/validation))


  (gen/sample (s/gen #{[:id :type 'vector? "Id will be sequence"]
                       [:id :contain 'int? "Id contain will be Long "]}) 5)


  (s/def ::kws (s/with-gen (s/and keyword? #(= (namespace %) "my.domain"))
                           #(s/gen #{:my.domain/name :my.domain/occupation :my.domain/id :hello.domain/:id})))
  (s/valid? ::kws :my.domain/name)                          ;; true
  (gen/sample (s/gen ::kws))




  )

