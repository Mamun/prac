(ns dadysql.plugin.base-impl-test
  (:use [clojure.test]
        [dady.proto]
        )
  (:require [dadysql.plugin.common-impl :refer :all]
            [dadysql.plugin.base-impl :refer :all]
            [dadysql.plugin.factory :refer :all]
            [dadysql.constant :refer :all]
            [dady.common :refer :all]
            [dadysql.core :as c]
            [dadysql.jdbc :as j]
            ))


#_(deftest doc-key-test
  (testing "testing doc key "
    (let [doc-ins (DocKey. "Hello") ]
      (println doc-ins)
      ))
  )


(comment

  (-> (new-root-node)
      (select-module-node-processor ))


  ;todo Need to check here to select process
  (let [m (-> (j/read-file "tie.edn.sql")
              (c/select-name [:get-dept-by-id]))]
    ; (clojure.pprint/pprint m)
    (->> (get-node-from-path (new-root-node) [module-key])
         (tree-seq branch? childrent)
         (filter (fn [v]
                   (and (satisfies? INodeProcessor v)
                        (not (satisfies? IBranchNode v))
                        (-process? v (first m)))))
         (distinct)
         ;  (sort-by (fn [v] (-lorder v)))
         (group-by (fn [v]
                     (-process-type v)))
         #_(clojure.pprint/pprint)))


  (let [m (-> (j/read-file "tie.edn.sql")
              (c/select-name [:get-dept-by-id]))]
    ; (clojure.pprint/pprint m)
    (->> (get-node-from-path (new-root-node) [module-key])
         (tree-seq branch? childrent)
         (filter (fn [v]
                   (and (satisfies? IParamNodeProcessor v)
                        (not (satisfies? IBranchNode v))
                        (-pprocess? v (first m)))))
         ;(filter (fn [v] (not (satisfies? IBranchNode v))))
         (distinct)
         ;(sort-by (fn [v] (-porder v)))
         #_(clojure.pprint/pprint)))


  (let [m (-> (j/read-file "tie.edn.sql")
              (c/select-name [:insert-dept]))]
    ; (clojure.pprint/pprint m)
    (->> (get-node-from-path (new-root-node) [module-key])
         (tree-seq branch? childrent)
         (filter (fn [v]
                   (and (satisfies? IParamNodeProcessor v)
                        (not (satisfies? IBranchNode v))
                        (-pprocess? v (first m)))))
         ;(filter (fn [v] (not (satisfies? IBranchNode v))))
         (distinct)
         ;(sort-by (fn [v] (-porder v)))
         #_(clojure.pprint/pprint)))


  (-> (new-root-node)
      (select-module-node-processor)
      (remove-node [param-key])
      )

  #_(->> (new-root-node)
         (select-module-node-processor)
         (group-by-process-type)
         (:input)
         (sort-by-order)
         )

  (->>
    (select-node (new-root-node) [module-key])
    (first)
    (-childs)
    (tree-seq (fn [v]
                (do
                  (println "v" v)
                  (or (satisfies? INodeProcessor v)
                      (satisfies? IParamNodeProcessor v))))
              (fn [v] (-childs v)))
    #_(filter (fn [v] (or (satisfies? INodeProcessor v)
                          (satisfies? IParamNodeProcessor v))))
    (clojure.pprint/pprint))

  ;(select-processor (new-sub-key-impl-map))




  (let [app-proc (new-global-key-node (new-leaf-node-coll))
        sch-value {doc-key         "hello"
                   file-name-key   "check.tx"
                   name-key        :hello
                   extend-meta-key {:hello {param-key [[:Next_transaction_id param-ref-key :transaction_id]]}}
                   param-key       [[:Next_transaction_id param-ref-key :transaction_id]]
                   }]
    (->> sch-value
         (-compiler-validate app-proc)
         (-compiler-emit app-proc)
         ;(map-name-with-sql)
         (clojure.pprint/pprint))
    )

  )


