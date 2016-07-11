(ns dadysql.compiler.workspace-spec-name
  (:use [clojure.test]
        [dadysql.compiler.core])
  (:require [clojure.spec :as s]
            [clojure.spec.test :as st]
            [clojure.spec.gen :as gen]
            [dadysql.compiler.file-reader :as f]))


(comment

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





