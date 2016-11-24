(ns workthrough-spec
  (:require [dadymodel.core :as c]
            [dadysql.impl.param-spec-impl :as ds]
            [clojure.spec :as s]
            [dadysql.jdbc :as t]
            [clojure.spec.gen :as gen]))



(comment


  (->> (t/read-file "tie.edn.sql")
       (vals)
       (ds/gen-spec "tie.sql")

       )

  )