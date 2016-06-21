(ns dadysql.plugin.factory
  (:require
    [dadysql.constant :refer :all]
    [dadysql.plugin.common-impl :as c]
    [dadysql.plugin.params.core :as p]
    [dadysql.plugin.validation.core :as v]
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
          (v/new-validation-key 10 (v/new-child-coll))
          (sql/new-sql-key 75 (sql/new-childs-key))
          (join/new-join-key)))



#_(defn select-node
  "Return all select node "
  [node-coll node-name-coll]
  (let [s (into #{} node-name-coll)
        r (filter (fn [v] (contains? s (np/node-name v))) node-coll)]
    (if (empty? r)
      r
      (into (empty node-coll) r))))


#_(defbranch ExtendKey [lname coll lorder])
#_(defbranch Modulekey [lname coll lorder])
#_(defbranch GlobalKey [lname coll lorder])


#_(defn branch?
  [node]
  (satisfies? IBranchNode node))


#_(defn childrent
  [node]
  (-childs node))


#_(defn new-module-key-node
  [leaf-node-coll]
  (Modulekey. module-key leaf-node-coll 0))


