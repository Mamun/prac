(ns test-data
  (:require ;[dadysql.spec :refer :all]
            [dadysql.jdbc :refer :all]
            [dadysql.jdbc-io :as jio])
  (:import [com.mchange.v2.c3p0 ComboPooledDataSource]))


(defonce ds (atom nil))
(defonce tms (atom nil))


(defn get-ds
  []
  (when (nil? @ds)
    (reset! ds {:datasource (ComboPooledDataSource.)})
    (println "Init datasource connection  "))
  @ds)


(defn get-tms
  []
  (when (nil? @tms)
    (let [w (read-file "tie.edn.sql")]
      (->> (select-name w {:dadysql.core/name [:create-ddl :init-data]})
           (jio/db-do (get-ds) ))
      (reset! tms w))
    (println "reading "))
  @tms)


(defn init-tms
  []
  (let [w (read-file "tie.edn.sql")]
    ;(db-do (get-ds) [:create-ddl :init-data] w)
    (reset! tms w))
  )


(comment

  (clojure.pprint/pprint
    (:create-dept (read-file "tie.edn.sql")))


  (init-tms)

  (get-ds)

  (get-tms)

  (->
    (read-file "tie.edn.sql")

    )

  )