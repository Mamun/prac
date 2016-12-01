 (ns schufa.walkthrogh
   (:require [schufa.sparser :as p ]) )




(comment

  (->> (p/read-file "./src/schufa/in.txt")
       (mapv p/parse-line)
       (p/partition-by-field-100)
       (p/do-line-format))


  )
