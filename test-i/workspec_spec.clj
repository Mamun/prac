(ns workspec-spec
  (:require [clojure.java.jdbc :as jdbc]
            [dadysql.jdbc :as t]
            [dadysql.jdbc-io :as io]
            [clojure.spec :as s]))


(comment


  (t/read-file "tie.edn.sql")

  (->> {:dadysql.core/name  [:get-employee-by-id :get-employee-dept :get-employee-detail]
        :dadysql.core/param {:id 1}}
       (t/select-name (t/read-file "tie.edn.sql"))

       )

  (->>
    (t/read-file "tie.edn.sql")
    (vals)
    (map :dadysql.core/spec)
    (remove nil?)
    (clojure.pprint/pprint))


  (s/exercise :unq.entity.tie/department)

  (spit
    "./target/tie.cljs"
    (-> (t/read-file "tie.edn.sql")
        (t/get-spec-str "target.core")))

  (s/registry)

  (-> (t/read-file "tie.edn.sql")
      (t/get-spec)
      )

  ;(s/exercise  :ex.tie/get-dept-employee)

  ;(s/registry)

  )