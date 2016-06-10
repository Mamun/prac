(ns dadysql.plugin.validation.vspec
  (:require [clojure.spec :as sp]
            [schema.core :as s]))




(comment


  (let [v-fn (fn [] true)
        params-pred? (s/pred v-fn   'k-spec-spec-valid?)
        schema '{(s/optional-key :hello) (s/pred (fn [v]
                                                   true
                                                   )  'k-spec-spec-valid?)}]

    (s/validate (eval schema)  {:hello2 1} )
    )





  (let [w1 {:key  '(clojure.spec/tuple keyword?)
            :key2 '(clojure.spec/tuple number?)}
        spec (->> w1
                  (seq)
                  (apply concat)
                  (cons 'clojure.spec/alt)
                  (list)
                  (cons 'clojure.spec/*)
                  )]
    (clojure.pprint/pprint spec)
    ;(sp/valid? (eval spec) [[:he] ["string"]])
    )





  (sp/valid?
    (sp/tuple keyword? keyword? resolve-type string?)
    [:a :b 'Integer "sdfsd"])


  (sp/explain
    (sp/tuple keyword? keyword? resolve-type string?)
    [:a :b 'Integer2 "sdfsd"])

  #_(s/validate validation-type-key-schema [:a :b 'Integer "sdfsd"])





  ;(sp/valid? ::hello3 "asdfasd")

  )
