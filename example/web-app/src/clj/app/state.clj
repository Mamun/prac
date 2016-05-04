(ns app.state
  (:require [clojure.java.io :as io]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [tiesql.jdbc :as tj]
            [tiesql.common :as cc]
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