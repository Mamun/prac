(ns walkthrough2
  (:require [clojure.java.jdbc :as jdbc]
            [dadysql.jdbc :as t]
            [dadysql.jdbc-io :as io]
            [test-data :as td]
            [clojure.walk :as w]
            [dadysql.compiler.spec :as cs]
            [dadysql.impl.param-spec-impl :as ds]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]))


(defn get-m [v]
  (->> (vals v)
       (w/postwalk (fn [w]
                     (if (keyword? w)
                       (list 'get-in 'm [w])
                       w)))
       (mapv (fn [w] (list 'fn ['m] w)))
       (interleave (keys v))
       (apply assoc {})))


;(s/def ::var-r resolve)
;(s/def ::params-v (s/or :k keyword? :t any?))
;(s/def ::params (s/map-of keyword? ::params-v))

(comment

  (s/explain ::params {:a :a :b (list 'inc :a)})

  (let [v {:v '(inc :a)}
        w (:v (get-m v))]
    (println w)
    ;(println   )
    ((eval w) {:a 3})
    ;  (mapv eval)
    )


  (->> {:dadysql.core/name [:init-db :init-data]}
       (t/select-name (t/read-file "tie4.edn.sql"))
       (io/db-do (td/get-ds)))


  (t/get-defined-spec (t/read-file "tie4.edn.sql"))

  (->> (t/read-file "tie4.edn.sql")
       (clojure.pprint/pprint))



  (-> @td/ds
      (t/pull (t/read-file "tie4.edn.sql")
              {:dadysql.core/name  [:get-dept-by-id]
               :dadysql.core/param {:id 1}}
              ;:dadysql.core/output-format :map
              )
      (clojure.pprint/pprint))


  ;((constantly 1))


  )

;(fn [m] (inc (get-in m [w])))





