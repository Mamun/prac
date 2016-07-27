(ns dadysql.plugin.base-impl
  (:require
   ; [schema.core :as s]
    [dadysql.spec :refer :all]
    [dady.common :refer :all]
    [dady.proto :refer :all]))





#_(defn new-global-key-node
  ([leaf-node-coll ]
   (GlobalKey. global-key leaf-node-coll 0)))


#_(defn new-extend-key-node
  ([leaf-node-coll ]
   (ExtendKey. :dadysql.spec/extend leaf-node-coll 0)))


#_(defn check-tx-proc?
  [v]
  (let [tt [{:isolation #{:none :read-committed :read-uncommitted :repeatable-read :serializable}}
            {:read-only? #{true false}}]
        v (partition 2 v)
        is-contains? (fn [k kv]
                       (->> (map #(contains? (k %) kv) tt)
                            (some true?)))]
    (reduce (fn [acc [k kv]]
              (if-not (is-contains? k kv)
                (reduced false)
                acc)
              ) true v)))







#_(defn merge-compiler-spec
  [root-spec node-coll]
  (reduce (fn [acc v]
            (if (satisfies? INodeCompiler v)
              (merge acc (spec v))
              acc)
            ) root-spec node-coll))


#_(defn compiler-emit-batch
  [node-coll node-v-m]
  (let [child-g (group-by-node-name node-coll)]
    (->> (keys node-v-m)
         (reduce (fn [acc k]
                   (if (satisfies? INodeCompiler (k child-g))
                     (update-in acc [k] (fn [v] (compiler-emit (k child-g) v)))
                     acc)) node-v-m))))



#_(extend-protocol INodeCompiler
  ExtendKey
  (-spec [this]
    (let [r `{(schema.core/optional-key ~model-key) schema.core/Keyword}
          w (merge-compiler-spec r (:coll this))]
      `{(schema.core/optional-key ~:dadysql.spec/extend)
        {schema.core/Keyword ~w}}))
  (-emit [this v-map]
    (->> (keys v-map)
         (reduce (fn [acc k]
                   (assoc acc k (compiler-emit-batch (:coll this) (k v-map)))
                   ) {})))
  ;;Global key
  GlobalKey
  (-spec [this]
    (merge-compiler-spec (default-global-spec) (:coll this)))
  (-emit [this v-map]
    (compiler-emit-batch (:coll this) v-map))
  ;; Module key
  Modulekey
  (-spec [this]
    (merge-compiler-spec {} (:coll this)))
  (-emit [this v]
    (compiler-emit-batch (:coll this) v)))

