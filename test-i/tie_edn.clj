(ns tie-edn
  (:require [clojure.spec :as s]
            [dady.spec :as ds]))


(dady.spec/defm int-id {:id int?})
(dady.spec/defm get-dept-by-id {:id int?})
(dady.spec/defm get-dept-by-ids {:id (s/every int?)})



(comment

  (s/valid? :int-id/spec {:id 34})

  (s/explain :int-id/spec {:id "asdf"} )

  )