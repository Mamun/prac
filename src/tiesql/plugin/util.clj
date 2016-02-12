(ns tiesql.plugin.util
  (:require [tiesql.proto :refer :all]))


(defn validate-schema-batch
  [node-coll v-coll]
  (let [child-g (group-by-node-name node-coll)]
    (->> v-coll
         (reduce (fn [acc k]
                   ;;todo should not be specific
                   (if-let [child-i ((second k) child-g)]
                     (compiler-validate child-i k)
                     acc
                     )) false))))


(defn merge-compiler-schema
  [root-schema node-coll]
  (reduce (fn [acc v]
            (if (satisfies? INodeCompiler v)
              (merge acc (compiler-schema v))
              acc)
            ) root-schema node-coll))


(defn compiler-emit-batch
  [node-coll node-v-m]
  (let [child-g (group-by-node-name node-coll)]
    (->> (keys node-v-m)
         (reduce (fn [acc k]
                   (if (satisfies? INodeCompiler (k child-g))
                     (update-in acc [k] (fn [v] (compiler-emit (k child-g) v)))
                     acc)) node-v-m))))


