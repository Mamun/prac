 (ns tie-edn2
   (:require [clojure.spec :as s]))


(s/def ::id integer?)

(s/def ::get-dept-by-id (s/keys :req-un [::id]))
