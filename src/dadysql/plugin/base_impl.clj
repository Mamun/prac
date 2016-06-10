(ns dadysql.plugin.base-impl
  (:require
    [schema.core :as s]
    [dadysql.constant :refer :all]
    [dady.common :refer :all]
    [dady.proto :refer :all]))


(defbranch ExtendKey [lname coll lorder])
(defbranch Modulekey [lname coll lorder])
(defbranch GlobalKey [lname coll lorder])


(defn branch?
  [node]
  (satisfies? IBranchNode node))


(defn childrent
  [node]
  (-childs node))


(defn new-module-key-node
  [leaf-node-coll]
  (Modulekey. module-key leaf-node-coll 0))


(defn new-global-key-node
  ([leaf-node-coll ]
   (GlobalKey. global-key leaf-node-coll 0)))


(defn new-extend-key-node
  ([leaf-node-coll ]
   (ExtendKey. extend-meta-key leaf-node-coll 0)))


(defn check-tx-proc?
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


(defn default-global-spec
  []
  `{(schema.core/required-key ~name-key)         schema.core/Keyword
    (schema.core/optional-key ~tx-prop)          schema.core/Any #_(s/pred check-tx-proc? 'check-tx-proc)
    (schema.core/optional-key ~file-name-key)    schema.core/Str
    (schema.core/optional-key ~file-reload-key)  boolean
    (schema.core/optional-key ~reserve-name-key) #{s/Keyword}
    (schema.core/optional-key ~ds-key)           schema.core/Any})





(defn merge-compiler-spec
  [root-spec node-coll]
  (reduce (fn [acc v]
            (if (satisfies? INodeCompiler v)
              (merge acc (spec v))
              acc)
            ) root-spec node-coll))


(defn compiler-emit-batch
  [node-coll node-v-m]
  (let [child-g (group-by-node-name node-coll)]
    (->> (keys node-v-m)
         (reduce (fn [acc k]
                   (if (satisfies? INodeCompiler (k child-g))
                     (update-in acc [k] (fn [v] (compiler-emit (k child-g) v)))
                     acc)) node-v-m))))



(extend-protocol INodeCompiler
  ExtendKey
  (-spec [this]
    (let [r `{(schema.core/optional-key ~model-key) schema.core/Keyword}
          w (merge-compiler-spec r (:coll this))]
      `{(s/optional-key extend-meta-key)
        {s/Keyword ~w}}))
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

