(ns dadysql.clj.spec-generator
  (:require [clojure.walk :as w]
            [dadysql.clj.spec-coerce :as sc]
            [clojure.spec :as s]))


(defn eval-spec [& coll-v]
  (doseq [v coll-v]
    (eval v)))


(defn create-ns-key [ns-key r]
  (let [w (if (namespace ns-key)
            (str (namespace ns-key) "." (name ns-key))
            (name ns-key))]
    (if (namespace r)
      (keyword (str w "." (namespace r) "/" (name r)))
      (keyword (str w "/" (name r))))))



(defn assoc-ns-key [ns-key m]
  (->> (map (fn [v]
              (create-ns-key ns-key v)) (keys m))
       (interleave (keys m))
       (apply assoc {})
       (clojure.set/rename-keys m)))



(defn convert-property-to-def [m]
  (map (fn [[k v]]
         (list 'clojure.spec/def k v))
       m))


(defmulti convert-model-tp-def (fn [k m t] t))

(defmethod convert-model-tp-def
  :default
  [k m _]
  (let [w ['clojure.spec/keys]
        w (if (:req m)
            (into w [:req (into [] (keys (:req m)))])
            w)
        w (if (:opt m)
            (into w [:opt (into [] (keys (:opt m)))])
            w)
        w (apply list w)]
    (list 'clojure.spec/def k
          (list 'clojure.spec/or
                :one w
                :list (list 'clojure.spec/coll-of w :kind 'vector?)))))

(def un-postfix "-un")

(defn add-postfix-to-key [k v]
  (keyword (str (namespace k) "/" (name k) v)))


(defmethod convert-model-tp-def
  un-postfix
  [k m _]
  (let [w-un ['clojure.spec/keys]
        w-un (if (:req m)
               (into w-un [:req-un (into [] (keys (:req m)))])
               w-un)
        w-un (if (:opt m)
               (into w-un [:opt-un (into [] (keys (:opt m)))])
               w-un)
        w-un (apply list w-un)]
    (list 'clojure.spec/def (add-postfix-to-key k un-postfix)
          (list 'clojure.spec/or
                :one w-un
                :list (list 'clojure.spec/coll-of w-un :kind 'vector?)))))



(defn convert-model-spec [[model-k model-v]]
  (let [req-k (:req model-v)
        opt-k (:opt model-v)]
    (into
      (list
        (convert-model-tp-def model-k model-v :default)
        (convert-model-tp-def model-k model-v un-postfix))
      (convert-property-to-def (merge opt-k req-k)))))



(s/def ::req (s/map-of keyword? symbol?))
(s/def ::opt (s/map-of keyword? symbol?))
(s/def ::req-or-opt (s/merge (s/or :req (s/keys :req [::req])
                                   :opt (s/keys :opt [::opt]))
                             (s/map-of #{:req :opt} any?)))

(s/def ::model (s/map-of keyword? ::req-or-opt))


(s/def ::input (s/cat :base-ns keyword? :model ::model))


#_(s/fdef map->spec :args ::input :ret any?)

(defn contains-in?
  [m ks]
  (not= ::absent (get-in m ks ::absent)))

(defn update-if-contains
  [m ks f & args]
  (if (contains-in? m ks)
    (apply (partial update-in m ks f) args)
    m))


(defn map-req-opt [[model-k m]]
  {model-k (-> m
               (update-if-contains [:opt] (fn [w] (assoc-ns-key model-k w)))
               (update-if-contains [:req] (fn [w] (assoc-ns-key model-k w))))})


(defn remove-quote [m]
  (clojure.walk/prewalk (fn [v]
                          (if (var? v)
                            (symbol (clojure.string/replace (str v) #"#'" ""))
                            v)
                          ) m))


(defn model->spec
  [base-ns-name m]
  (if (s/valid? ::input [base-ns-name m])
    (->> (remove-quote m)
         (assoc-ns-key base-ns-name)
         (map map-req-opt)
         (into {})
         (map convert-model-spec)
         (apply concat))
    (throw (ex-info "failed " (s/explain-data ::input [base-ns-name m])))))


(defn resolve-symbol
  [m]
  (clojure.walk/prewalk (fn [v]
                          (if (and (seq? v)
                                   (= 'quote (first v)))
                            (if-let [r (resolve (eval v))]
                              r
                              (throw (ex-info "Could not resolve symbol " {:symbol (eval v)})))
                            v)
                          ) m))


(defmacro defsp [base-ns m]
  (let [r (->> (model->spec base-ns m)
               (resolve-symbol))]
    `~(cons 'do r)))




;;;;;;;;;;;;;;;;;;;;;;;;;;Additional spec

(defn as-merge-spec [spec-coll]
  (if (or (nil? spec-coll)
          (empty? spec-coll))
    spec-coll
    (->> spec-coll
         (remove nil?)
         (map (fn [w] (add-postfix-to-key w un-postfix)))
         (cons 'clojure.spec/merge))))


(defn as-relational-spec [[f-spec & rest-spec]]
  (list 'clojure.spec/merge f-spec
        (list 'clojure.spec/keys :req (into [] rest-spec))))


(defn registry [n-name]
  (->> (s/registry)
       (w/postwalk (fn [v]
                     (if (map? v)
                       (->> v
                            (filter (fn [[k _]]
                                      (clojure.string/includes? (str k) (str n-name))))
                            (into {}))
                       v)))))


(defn write-spec-to-file [dir package-name spec-list]
  (let [as-dir (clojure.string/join "/" (clojure.string/split package-name #"\."))
        file-path (str dir "/" as-dir ".cljc")]
    (with-open [w (clojure.java.io/writer file-path)]
      (.write w (str "(ns " package-name "  \n  (:require [clojure.spec]))"))
      (.write w "\n")
      (doseq [v1 spec-list]
        (.write w (str v1))
        (.write w "\n")))))






(comment

  (s/def :person/id (s/conformer x-integer?))
  (s/def :person/id2 integer?)
  (s/def :model/person (s/keys :req [:person/id :person/id2] ))


  (defsp :model2 {:person {:req {:id 'int? :id2 int?}}})



  (s/explain :model2/person-un {:id 123 :id2 123})


  (s/exercise :model/person )

  (registry :model2)

  (macroexpand-1 '(sg/defs :model {:person {:opt {:name 'int?}}}))

  (let [v (quote 'int?)]

    )

  ;(= 'quote (first (quote 'int?)) )

  (let [v {:dept {:req {:name 'string?
                        :id   'int?}}}
        sp (sg/model->spec :model v)]
    ;(clojure.pprint/pprint sp)
    (->> sp
         (first)
         (last)
         (resolve)
         (eval)
         )

    #_(clojure.pprint/pprint (apply sg/eval-spec sp) ))


  (s/conform ::person {::id "2344" })






  )
