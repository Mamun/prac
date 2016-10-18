(ns dadysql.compiler.spec-workspace
  (:use [clojure.test]
        [dadysql.compiler.core])
  (:require [clojure.spec :as s]
            [clojure.spec.test :as st]
            [clojure.spec.gen :as gen]
            [dadysql.compiler.file-reader :as f]))



(defn adder
  "Do add with 1"
  [x] (+ 1 x))

(s/fdef adder
        :args (s/cat :x number?)
        :ret number?)



(s/def :person/fname string?)
(s/def :person/lname string?)
(s/def :customer/id int?)


(s/def :person/name (s/merge
                      (s/keys :req-un [:person/lname :person/fname])
                      (s/map-of #{:lname :fname} any?)))


(s/def :app/customer (s/merge
                       :person/name
                       (s/keys :req-un [:customer/id])
                       (s/map-of #{:lname :fname :id} any?)))


(s/explain-str :person/name {:lname "Max" :fname "Musterman"})
(s/explain-str :app/customer {:lname "Max" :fname "Musterman" :id 9})



(s/def :get-name/id string?)

(s/def :get-name/spec (s/keys :req-un [:get-name/id]) )

(s/explain :get-name/spec {:id "hello"})


(s/def :get-name-details/postcode string?)
(s/def :get-name-details/spec (s/keys :req-un [:get-name-details/postcode]) )

(s/explain (s/merge :get-name/spec :get-name-details/spec) {:id "sdf" :postcode "asdf"} )






(comment


  (adder "asfasd")



  (st/check `adder)



  (clojure.repl/doc adder)

  (clojure.spec.test/instrument 'adder)





  (s/def ::fname string?)
  (s/def ::lname string?)
  ;(s/def ::person (s/keys :req-un [::name] ))
  ;(s/def ::lperson (s/cat :list (s/+ ::person)))



  (defn is-base? [v]
    (and
      (= "hello" (:fname v))
      (not (contains? v :lname))))


  (s/def ::spec (s/cat :base (s/? (s/& (s/keys :req-un [::fname])))
                       :opt (s/+ (s/keys :req-un [::fname ::lname]))))


  (s/conform ::spec [{:fname "hello" :lname "test"}])

  (s/conform ::spec [{:fname "hello"} {:fname "hello" :lname "test"}])

  (s/conform ::spec [{:fname "hello"} {:fname "hello" :lname "test"}])


  (gen/generate (s/gen ::spec)))

;(gen/generate (s/) )





