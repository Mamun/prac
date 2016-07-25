 (ns tie-edn
   (:require [clojure.spec :as s]
             [dady.spec :as ds]))


(dady.spec/defsp
  int-id
  (s/def ::id int?)
  (s/def ::spec (s/keys :req-un [::id])))


(dady.spec/defsp
  get-dept-by-id
  (s/def ::id int?)
  (s/def ::spec (s/keys :req-un [::id])))


(dady.spec/defsp
  get-dept-by-ids
  (s/def ::id (s/every int?))
  (s/def ::spec (s/keys :req-un [::id])))







(comment


  #_(s/valid?
    (::s/kvs->map {:id int?})
    {:id "hello"}
    )

  ;(s/form :get-dept-by-ids/id)
  ;(ds/find-ns-spec 'cfg )

  (s/registry)

  {:id int? :vip string?}

  ;(resolve 'int1?)

  (s/conform ::int "asdfsd")

  (s/conform :get-dept-by-id/spec {:id 2})

  (s/spec? (s/spec ::get-dept-by-id))

  ;(s/valid? keyword? ::a)

  (load-file "tie_edn.clj")


  )