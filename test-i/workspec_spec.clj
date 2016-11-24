 (ns workspec-spec
   (:require [clojure.java.jdbc :as jdbc]
             [dadysql.jdbc :as t]
             [dadysql.jdbc-io :as io]
             [dadysql.impl.param-spec-impl :as psi]
             [clojure.spec :as s]))


(comment


  (t/read-file "tie.edn.sql")

  (->> {:dadysql.core/name  [:get-employee-by-id :get-employee-dept :get-employee-detail]
        :dadysql.core/param {:id 1}}
       (t/select-name  (t/read-file "tie.edn.sql") )
       (psi/validate-param-spec  {:dadysql.core/name  [:get-employee-by-id :get-employee-dept :get-employee-detail]
                                  :dadysql.core/param {:id 1}})
       )

  (->>
    (t/read-file "tie.edn.sql")
    (vals)
    (map :dadysql.core/spec)
    (remove nil?)
    (clojure.pprint/pprint))


  (->> (t/read-file "tie.edn.sql")
       (vals)
       (psi/gen-spec "tie.edn.sql" )
       )

  ;(s/exercise  :ex.tie/get-dept-employee)

  ;(s/registry)

  )