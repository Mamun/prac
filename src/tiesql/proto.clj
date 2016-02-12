(ns tiesql.proto
  (:require [tiesql.common :as cc])
  (:import [PersistentVector]
           (clojure.lang PersistentVector)))


(defprotocol ILeafNode
  (-node-name [this] "Node name"))


(defprotocol IBranchNode
  (-childs [this] "Return all chidls ")
  (-get-child [this leaf-node-name] "Return cell ")
  (-get-child-path [this leaf-node-name] "return sub component path ")
  (-add-child [this leaf-node] "Add new cell")
  (-remove-child [this leaf-node-name] "Remove exiting cell")
  (-update-child [this leaf-node] "Update an exiting cell"))


(defprotocol INodeCompiler
  (-compiler-emit [this w] "Change value or ")
  (-schema [this] "Defined schema here")
  (-compiler-validate [this v] "Validate schema here"))


(defprotocol INodeProcessor
  (-lorder [this] "Node order ")
  (-process? [this m] "return true or false ")
  (-process [this m] "do process ")
  (-process-type [this] "return process type "))


(defprotocol IParamNodeProcessor
  (-porder [this] "Node order ")
  (-pprocess-type [this] "return process type ")
  (-pprocess? [this m] "return value m ")
  (-pprocess [this emit-value m] "do process "))


(defn node-name
  "Return node name "
  [node]
  (-node-name node))


(defn node-order
  "Return priority of node "
  [node]
  (cond
    (satisfies? INodeProcessor node) (-lorder node)
    (satisfies? IParamNodeProcessor node) (-porder node)
    :else -1))


(defn node-process
  [node v]
  (if (satisfies? INodeProcessor node)
    (-process node v)
    (cc/fail "Node type is not found")))


(defn compiler-schema
  [node]
  (-schema node))


(defn compiler-validate
  [node v]
  (-compiler-validate node v))


(defn compiler-emit
  [node v]
  (-compiler-emit node v))

;;;; For branch


(defn select-node
  "Return all select node "
  [node-coll node-name-coll]
  (let [s (into #{} node-name-coll)
        r (filter (fn [v] (contains? s (node-name v))) node-coll)]
    (if (empty? r)
      r
      (into (empty node-coll) r))))



;;;;;;;;; End branch

;; Additional API for client



(defn group-by-node-name
  [node-coll]
  (->> node-coll
       (group-by (fn [w]
                   (node-name w)))
       (map (fn [[k v]] [k (first v)]))
       (into {})))



(defn filter-node-processor
  ""
  [node-coll]
  (->> node-coll
       (filter (fn [v] (or (satisfies? INodeProcessor v)
                           (satisfies? IParamNodeProcessor v))))
       (into (empty node-coll))))


(defn get-child
  [node child-name]
  (-get-child node child-name))


(defn add-child-one
  [branch-node leaf-node]
  (-add-child branch-node leaf-node))


(defn add-child-batch
  [branch-node leaf-node-coll]
  (reduce add-child-one branch-node leaf-node-coll))


(defn remove-child
  [branch-node leaf-node]
  (-remove-child branch-node leaf-node))


(defn remove-child-batch
  [branch-node leaf-node-coll]
  (reduce remove-child branch-node leaf-node-coll))


;; PersistentVector as Branch



(defn indices
  [pred coll]
  (keep-indexed #(when (pred %2) %1) coll))


(defn cell-index-single
  [coll cell-name]
  (first (-get-child-path coll cell-name)))


(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))


(extend-protocol IBranchNode
  PersistentVector
  (-childs [this] this)
  (-get-child [this nname]
    (if-let [p (first (indices (fn [v] (= (node-name v) nname)) this))]
      (get this p)
      nil))
  (-add-child [this node] (conj this node))
  (-remove-child [this ln]
    (if-let [p (first (indices (fn [v] (= (node-name v) ln)) this))]
      (vec-remove this p)
      this))
  (-update-child [this node]
    (let [cname (-node-name node)
          p (first (indices (fn [v] (= (node-name v) cname)) this))]
      (if p
        (assoc this p node)
        this)))
  (-get-child-path [this nname]
    (do
      (indices (fn [v] (= (node-name v) nname)) this))))



(defmacro defleaf [name & pr]
  (let [kw (keyword (ffirst pr))
        this (gensym 'this)]
    `(defrecord ~name ~@pr
       ILeafNode
       (-node-name [~this] (~kw ~this)))))


;(macroexpand-1 '(defleaf Hello [lname done]))

;[lname lorder coll]
(defmacro defbranch [name & pr]
  (let [this (gensym 'this)
        cname (gensym 'cname)
        temp (gensym 'temp)
        lw (keyword (ffirst pr))
        kw (keyword (second (first pr)))]
    `(defrecord ~name ~@pr
       ILeafNode
       (-node-name [~this] (~lw ~this))
       IBranchNode
       (-get-child-path [~this ~cname]
         (let [~temp (cell-index-single (~kw ~this) ~cname)]
           (if ~temp
             (vector ~kw ~temp)
             (vector))))
       (-add-child [~this ~cname]
         (->> (-add-child (~kw ~this) ~cname)
              (assoc ~this ~kw)))
       (-remove-child [~this ~cname]
         (->> (-remove-child (~kw ~this) ~cname)
              (assoc ~this ~kw)))
       (-childs [~this] (~kw ~this))
       (-get-child [~this ~cname]
         (-get-child (~kw ~this) ~cname))
       (-update-child [~this ~cname]
         (->> (-update-child (~kw ~this) ~cname)
              (assoc ~this ~kw))))))


;(macroexpand-1 '(defbranch Hello [lanme coll lorder]))


(defrecord FnNode [cname order ptype f]
  ILeafNode
  (-node-name [this] (:cname this))
  INodeProcessor
  (-lorder [this] (:corder this))
  (-process-type [this] (:ptype this))
  (-process? [_ _] true)
  (-process [this m]
    ((:f this) m)))


(defn fn-as-node-processor
  [f & {:keys [name order ptype]
        :or   {name  :identity
               order -1
               ptype (or name :input)}}]
  (FnNode. name order ptype f))


#_(defmacro fn-as-node [f & node-description]
    (let [this (gensym 'this)
          node-name (first node-description)
          cname (keyword node-name)
          node-description (into [] node-description)]
      (println node-description)
      `(defrecord ~node-name ~node-description
         ILeafNode
         (-node-name [~this] (~cname ~this))
         )))

;(macroexpand-1 '(fn-as-node identity hello check ))


;(extend-as-leaf FnNode :he)
;(macroexpand '(extend-as-leaf FnNode :he))

#_(defmacro extend-as-branch
    [vname n]
    (let [msg (gensym 'message)]
      `(extend-protocol ILeafNode
         ~vname
         (-node-name [~msg] (~n ~msg)))))


;(defrecord Hello [name])

;(extend-as-branch Hello :name)
;(macroexpand '(extend-as-branch Hello :name))

;(extend-as-branch Hello :name)
;(-node-name (Hello. "sdfds"))


;(defrecord CollName [ccoll :ccoll])
;(extend-as-branch CollName :ccoll)
;(macroexpand '(extend-as-branch CollName :ccoll))




#_(defmacro extend-as-branch
    [vname ccoll]
    (let [this (gensym 'this)
          cname (gensym 'cname)
          temp (gensym 'temp)]
      `(extend-protocol IBranchNode
         ~vname
         (-get-child-path [~this ~cname]
           (let [~temp (cell-index-single (~ccoll ~this) ~cname)]
             (if ~temp
               (vector ~ccoll ~temp)
               (vector))))
         (-add-child [~this ~cname]
           (->> (-add-child (~ccoll ~this) ~cname)
                (assoc ~this ~ccoll)))
         (-remove-child [~this ~cname]
           (->> (-remove-child (~ccoll ~this) ~cname)
                (assoc ~this ~ccoll)))
         (-childs [~this] (~ccoll ~this))
         (-get-child [~this ~cname]
           (-get-child (~ccoll ~this) ~cname))
         (-update-child [~this ~cname]
           (->> (-update-child (~ccoll ~this) ~cname)
                (assoc ~this ~ccoll))))))




#_(defmacro extend-as-leaf
    [vname n]
    (let [this (gensym 'this)]
      `(extend-protocol ILeafNode
         ~vname
         (-node-name [~this] (~n ~this)))))



(defn remove-type
  [node type]
  (if (satisfies? IBranchNode node)
    (reduce (fn [acc v]
              (if (and
                    (satisfies? INodeProcessor v)
                    (= type
                       (-process-type v)))
                (remove-child acc (node-name v))
                acc)
              ) node (-childs node))
    (throw (ex-info "Node must be INodeProcessor " {}))))


(defn as-xf-process
  [pt node]
  (if (satisfies? IBranchNode node)
    (->> node
         (-childs)
         (filter (fn [n] (satisfies? INodeProcessor n)))
         (filter (fn [n] (= pt (-process-type n))))
         (sort-by (fn [n] (-lorder n)))
         (map (fn [n] (map (fn [m] (-process n m))))))
    (throw (ex-info "Node must be INodeProcessor " {}))))


(defn node-path
  "Find node path "
  [parent-node node-name-coll]
  (loop [i parent-node
         [f & n] node-name-coll
         real-path []]
    (if (nil? f)
      real-path
      (let [np (-get-child-path i f)]
        (if (empty? np)                                     ;;No path found then it will return
          np
          (->> (reduce conj real-path np)
               (recur (-get-child i f) n)))))))


(defn get-node-from-path
  "Return node from parent path "
  [parent-node node-path-coll]
  (let [path-index (node-path parent-node node-path-coll)]
    (if (empty? path-index)
      nil
      (get-in parent-node path-index))))


(defn add-node-to-path
  "Add node to parent path "
  [parent-node node-path-coll new-node]
  (if (< 1 (count node-path-coll))
    (let [p (node-path parent-node node-path-coll)]
      (if (empty? p)
        parent-node
        (update-in parent-node p (fn [v] (-add-child v new-node)))))
    (-add-child parent-node new-node)))


(defn remove-node-from-path
  "Remove node from parent path "
  [parent-node node-path-coll]
  (if-let [w (butlast node-path-coll)]
    (let [path-index (node-path parent-node w)]
      (if (empty? path-index)
        parent-node
        (update-in parent-node path-index (fn [v]
                                            (-remove-child v (last node-path-coll))))))
    (-remove-child parent-node (first node-path-coll))))


(defn remove-node-batch
  [parent-node node-path-coll-coll]
  (reduce remove-node-from-path parent-node node-path-coll-coll))


(defn update-node-to-path
  "Update mode to parent path "
  [parent-node node-path-coll node]
  (let [p (node-path parent-node node-path-coll)]
    (update-in parent-node p (fn [v] (-update-child v node)))))
