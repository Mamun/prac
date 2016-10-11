(ns dadysql.compiler.data-gen
  (:require  [clojure.spec.gen :as gen]
             [clojure.spec]
             [clojure.spec :as s]))


(def model (s/cat :name string? :age int?) )


(defn generate-data []
  (gen/fmap
    (fn [v]
      v
      )
    (s/gen model)
    )
  )

(comment



  (gen/sample (generate-data))

  (gen/generate generate-data)

  (s/sample (s/spec :gen generate-data))
  (generate-data)


  )


