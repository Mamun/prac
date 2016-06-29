(ns dadysql.compiler.spec-workspace
  (:use [clojure.test]
        [dadysql.compiler.core])
  (:require [clojure.spec :as s]
            [clojure.spec.test :as st]
            [clojure.spec.gen :as gen]
            [dadysql.compiler.file-reader :as f]))



(comment


  ;(ranged-rand -9 9)



  (st/instrument `do-compile)
  ;(st/v)
  (st/test `do-compile)

  ;(s/conform :dadysql.compiler.spec/result2 #{:single} )


  (st/test
    (gen/generate (s/gen :dadysql.compiler.spec/skip))

    )


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

