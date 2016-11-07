(ns dadysql.compiler.core-inheritance)


(defn compiler-merge
  [old new]
  (cond (map? new) (merge old new)
        (vector? new) (into (empty old) (concat new old))
        :else (or new old)))


(defn do-inheritance
  [w module-m f-config]
  (let [name-v (get w :dadysql.core/name)
        w1 (merge-with compiler-merge
                       (get-in f-config [:dadysql.core/extend name-v])
                       (get-in module-m [:dadysql.core/extend name-v])
                       w)

        model-v (get w1 :dadysql.core/model)
        w2 (merge-with compiler-merge
                       (get-in f-config [:dadysql.core/extend model-v])
                       (get-in module-m [:dadysql.core/extend model-v])
                       w1)

        module-m (dissoc module-m :dadysql.core/name :dadysql.core/model :dadysql.core/sql :dadysql.core/extend :dadysql.core/doc)
        f-config (select-keys f-config [:dadysql.core/default-param :dadysql.core/param-spec :dadysql.core/timeout])]
    (merge-with compiler-merge f-config module-m w2)))
