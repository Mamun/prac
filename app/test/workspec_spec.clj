 (ns workspec-spec
   (:require [clojure.java.jdbc :as jdbc]
             [dadysql.jdbc :as t]
             [dadysql.clj.walk :as w]
             [dadysql.jdbc-io :as io]
             [clojure.spec :as s]))


(comment


  ;(w/postwalk-filter "1" {:a 2 :b 1})


  (t/read-file "app.sql")

  (->> {:dadysql.core/name  [:get-employee-by-id :get-employee-dept :get-employee-detail]
        :dadysql.core/param {:id 1}}
       (t/select-name  (t/read-file "app.sql") ))


  (->> (t/read-file "app.sql")
       (vals)
       (map :dadysql.core/spec)
       (remove nil?)
       (clojure.pprint/pprint))




  (spit
    "./target/tie.cljs"
    (-> (t/read-file "app.sql")
        (t/get-spec-str "target.core")))



  (-> (t/read-file "app.sql")
      (t/get-spec )
      )

  ;(s/exercise  :ex.tie/get-dept-employee)

  ;(s/registry)

  )