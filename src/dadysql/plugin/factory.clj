(ns dadysql.plugin.factory
  (:require
    [dadysql.constant :refer :all]
    [dady.proto :as np]
    [dadysql.plugin.base-impl :as b]
    [dadysql.plugin.common-impl :as c]
    [dadysql.plugin.params.core :as p]
    [dadysql.plugin.validation.core :as v]
    [dadysql.plugin.sql.bind-impl :as sql]
    [dadysql.plugin.join.join-impl :as join]))


(defn new-leaf-node-coll
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
          (v/new-validation-key 10 (v/new-child-coll))
          (sql/new-sql-key 75 (sql/new-childs-key))
          (join/new-join-key)))




(defn select-node
  "Return all select node "
  [node-coll node-name-coll]
  (let [s (into #{} node-name-coll)
        r (filter (fn [v] (contains? s (np/node-name v))) node-coll)]
    (if (empty? r)
      r
      (into (empty node-coll) r))))


(defn new-root-node
  []
  (let [leaf-node-coll (new-leaf-node-coll)
        leaf-name-for-extends [param-key
                               column-key
                               timeout-key
                               validation-key
                               skip-key
                               result-key]
        leaf-node-coll (->> leaf-name-for-extends
                            (select-node leaf-node-coll)
                            (b/new-extend-key-node)
                            (conj leaf-node-coll))
        leaf-for-global (select-node leaf-node-coll [doc-key
                                                     param-key
                                                     column-key
                                                     validation-key
                                                     result-key
                                                     extend-meta-key
                                                     timeout-key
                                                     join-key])
        gpc (b/new-global-key-node leaf-for-global)
        mpc (b/new-module-key-node leaf-node-coll)]
    (vector gpc mpc)))