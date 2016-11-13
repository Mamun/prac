(ns dadysql.clj.spec-generator
  (:require [clojure.walk :as w]
            [dadysql.clj.spec-gen-impl :as sc]
            [clojure.spec :as s])
  (:import [BigInteger]))


(defn x-int? [x]
  (cond
    (integer? x) x
    (string? x) (try
                  (Integer/parseInt x)
                  (catch Exception e
                    :clojure.spec/invalid))
    :else :clojure.spec/invalid))


(defn x-integer? [x]
  (cond
    (integer? x) x
    (string? x) (try
                  (BigInteger. x)
                  (catch Exception e
                    :clojure.spec/invalid))
    :else :clojure.spec/invalid))



(defn fn->conformer-fn [s]
  (if (symbol? s)
    (condp = s
      'integer? (list 'clojure.spec/conformer 'dadysql.clj.spec-generator/x-integer?)
      'int? (list 'clojure.spec/conformer 'dadysql.clj.spec-generator/x-int?)
      s)
    s))



(defn assoc-ns-key [ns-key m]
  (if (nil? m)
    {}
    (->> (map (fn [v]
                (sc/create-ns-key ns-key v)) (keys m))
         (interleave (keys m))
         (apply assoc {})
         (clojure.set/rename-keys m))
    )

  )






(s/def ::req (s/map-of keyword? symbol?))
(s/def ::opt (s/map-of keyword? symbol?))
(s/def ::req-or-opt (s/merge (s/or :req (s/keys :req [::req])
                                   :opt (s/keys :opt [::opt]))
                             (s/map-of #{:req :opt} any?)))

(s/def ::model (s/map-of keyword? ::req-or-opt))


(s/def ::input (s/cat :base-ns keyword? :model ::model))


(defn gen-spec+ [base-ns-name m]
  (let [f (->> m
               (assoc-ns-key base-ns-name)
               (map sc/model->spec2)
               (apply concat))
        w (->> m
               (clojure.walk/postwalk fn->conformer-fn)
               (assoc-ns-key (sc/add-postfix-to-key base-ns-name "-ex"))
               (map sc/model->spec2)
               (apply concat))]
    (into w (reverse f))))



(defn gen-spec
  [base-ns-name m]
  (if (s/valid? ::input [base-ns-name m])
    (->> (clojure.walk/postwalk sc/var->symbol m)
         (gen-spec+ base-ns-name))

    (throw (ex-info "failed " (s/explain-data ::input [base-ns-name m])))))


(comment
  (let [v {:dept    {:req {:name 'string?
                           :id   'int?}
         }
           ;:student {:req {:name 'string :id   'int?}}
           }]
    (gen-spec :model v))


  (group-by first
            [[:dept :1-1 :empl]
             [:company :1-n :dept]
             ])


  )


(defmacro defsp [base-ns-name m]
  (if (s/valid? ::input [base-ns-name m])
    (let [r (gen-spec+ base-ns-name m)]
      `~(cons 'do r))
    (throw (ex-info "failed " (s/explain-data ::input [base-ns-name m])))))



(comment

  (macroexpand-1 '(defsp :model {:person {:opt {:name string?}}}))

  (defsp :model3 {:person {:req {:id2 int?}}})

  (s/conform :model3/person {:id2 3})
  ;(->> )
  (s/conform :model3/person {:id2 3})
  (s/conform :model3/person-list [{:id2 3}] )

  (->> (s/conform :model3-ex/person {:id2 "3"} )
       (s/unform :model3/person) )


  (->> (s/conform :model3-ex/person-list  [{:id2 "3"} {:id2 "4"}] )
       (s/unform :model3/person-list) )


  )


;;;;;;;;;;;;;;;;;;;;;;;;;;Additional spec

(defn as-merge-spec [spec-coll]
  (if (or (nil? spec-coll)
          (empty? spec-coll))
    spec-coll
    (->> spec-coll
         (remove nil?)
         (map (fn [w] (sc/add-postfix-to-key w "-un")))
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


(defn eval-spec [& coll-v]
  (doseq [v coll-v]
    (eval v)))

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

  (println "Hello")

  (s/def :person/id (s/conformer sc/x-integer?))
  (s/def :person/id2 integer?)
  (s/def :model/person (s/keys :req [:person/id :person/id2]))


  ;(= 'int? 'int?)



  (println "hello")

  (s/explain :model/person {:person/id "123" :person/id2 123})


  (s/exercise :model/person)

  (registry :model2)

  (println
    (macroexpand-1 '(defsp :model {:person {:opt {:name int?}}}))
    )


  (let [v (quote 'int?)]

    )

  ;(= 'quote (first (quote 'int?)) )

  (let [v {:dept {:req {:name 'string?
                        :id   'int?}}}
        sp (gen-spec :model v)]
    (clojure.pprint/pprint sp)
    #_(->> sp
           (first)
           (last)
           (resolve)
           (eval)
           )

    )

  (symbol? 'int?)




  (let [v {:dept {:req {:name 'string?
                        :id   'int?}}}]
    (gen-ex-spec :model v))


  (s/conform ::person {::id "2344"})

  )
