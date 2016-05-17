(ns app.state
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [tiesql.jdbc :as tj]
            [dady.common :as cc]
            [clojure.tools.logging :as log])
  (:import
    [com.mchange.v2.c3p0 ComboPooledDataSource]))



(defonce ds-atom (atom nil))
(defonce tms-atom (atom nil))


(defn init-state []
  (when (nil? @ds-atom)
    (reset! ds-atom {:datasource (ComboPooledDataSource.)}))
  (when (nil? @tms-atom)
    (cc/try->> (tj/read-file "tie.edn.sql")
               (tj/db-do @ds-atom [:create-ddl :init-data])
               (tj/validate-dml! @ds-atom)
               (reset! tms-atom))))


(defn get-ds [] @ds-atom)
(defn get-tms [] @tms-atom)






(comment


  (jdbc/query @ds-atom "select * from deal")

  (init-state)

  (tj/db-do @ds-atom [:init-data] @tms-atom)

  (let [v (tj/pull @ds-atom @tms-atom {:name :get-deal-list})]
    (log/info "---" v)
    v)
  )