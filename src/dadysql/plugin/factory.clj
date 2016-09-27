(ns dadysql.plugin.factory
  (:require

    [dadysql.plugin.common-impl :as c]
    [dadysql.plugin.params.core :as p]
    [dadysql.plugin.sql.bind-impl :as sql]
    [dadysql.plugin.join.join-impl :as join]))


(defn new-root-node
  []
  (vector (c/new-name-key)
          (c/new-doc-key)
          (c/new-model-key)
          (c/new-group-key)
          (c/new-timeout-key)
          (c/new-commit-key)
          (c/new-skip-key)
          (c/new-column-key)
          (c/new-result-key)
          (p/new-param-key 5 (p/new-child-keys))
          ;(v/new-validation-key 10 (v/new-child-coll))
          (sql/new-sql-key 75 (sql/new-childs-key))
          (join/new-join-key)))




