 (ns tie-edn
   (:require [clojure.spec :as s]))


(s/def ::id integer?)

(s/def ::get-dept-by-id (s/keys :req-un [::id]))



(comment

  (s/conform ::get-dept-by-id {:id 2})

  (s/spec? (s/spec ::get-dept-by-id))

  ;(s/valid? keyword? ::a)

  (load-file "tie_edn.clj")


  )