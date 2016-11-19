(ns dadyspec.core
  (:require [clojure.walk :as w]
            [dadyspec.core-impl :as impl]
            [dadyspec.util :as u]
;            [cheshire.core :as ch]
            [clojure.spec :as s]
            [dadyspec.core-xtype :as sg]))


(defn- conform* [m]
  (clojure.walk/postwalk (fn [s]
                           (if-let [r (get sg/*conformer-m* s)]
                             r
                             s)
                           ) m))



(s/def ::req (s/every-kv keyword? any? :min-count 1))
(s/def ::opt (s/every-kv keyword? any? :min-count 1))

(s/def ::model
  (s/every-kv keyword?
              (s/merge (s/keys :opt-un [::opt ::req]) (s/map-of #{:req :opt} any?))
              :min-count 1))


(def rel-type-set #{:dadyspec.core/rel-one-many
                    :dadyspec.core/rel-one-one
                    :dadyspec.core/rel-many-one})

(s/def ::join (s/coll-of (s/tuple keyword? rel-type-set keyword?) :type vector?))

(s/def ::gen-type (s/coll-of #{:qualified :unqualified :ex} :pred #{}))

(s/def ::opt-k (s/merge (s/keys :opt-un [::join ::gen-type])
                        (s/map-of #{:join :gen-type} any?)))


(s/def ::input (s/cat :base-ns keyword?
                      :model ::model
                      :opt ::opt-k))
(s/def ::output any?)


(defn- var->symbol [v]
  (if (var? v)
    (symbol (clojure.string/replace (str v) #"#'" ""))
    v))


(defn gen-spec
  ([namespace-name model-m opt-config-m]
   (if (s/valid? ::input [namespace-name model-m opt-config-m])
     (let [{:keys [join gen-type]} opt-config-m
           m (clojure.walk/postwalk var->symbol model-m)
           q-list (when (contains? gen-type :qualified)
                    (->> {:fixed? false :qualified? true :join join}
                         (impl/model->spec namespace-name m)))
           unq-list (when (contains? gen-type :unqualified)
                      (->> {:fixed? false :qualified? false :postfix "un-" :join join}
                           (impl/model->spec namespace-name m)))
           ex-list (when (contains? gen-type :ex)
                     (->> {:join join :fixed? false :qualified? false :postfix "ex-"}
                          (impl/model->spec namespace-name (conform* m))))]
       (concat q-list unq-list ex-list))
     (throw (ex-info "failed " (s/explain-data ::input [namespace-name model-m opt-config-m])))))
  ([namespace-name model-m]
   (gen-spec namespace-name model-m {:join [] :gen-type #{:qualified}})))



(s/fdef gen-spec :args ::input :ret ::output)

(defn merge-spec [& spec-coll]
  (->> spec-coll
       (remove nil?)
       (cons 'clojure.spec/merge)))


(defn relation-merge [namespace join & {:as m}]
  (let [w (mapv #(u/assoc-ns-join namespace %) join)
        w-m (group-by first w)]
    (mapv (fn [[k v]]
            (impl/relational-merge-spec-template k (mapv #(nth % 2) v) m)
            ) w-m)))



;(sc/create-ns-key :hello :a)


(defmacro defsp
  ([m-name m & {:as opt-m}]
   (let [m-name (keyword m-name)]
     (let [r (gen-spec m-name m opt-m)]
       `~(cons 'do r)))))



#_(defn conform-json [spec json-str]
  (->> (ch/parse-string json-str true)
       (s/conform spec)))


;;;;;;;;;;;;;;;;;;;;;;;;;;Additional spec


(defn registry [namespace-name]
  (->> (s/registry)
       (w/postwalk (fn [v]
                     (if (map? v)
                       (->> v
                            (filter (fn [[k _]]
                                      (clojure.string/includes? (str k) (str namespace-name))))
                            (into {}))
                       v)))))


#_(defn eval-spec [& coll-v]
  (doseq [v coll-v]
    (eval v)))



#_(defn as-file-str [ns-name spec-list]
  (let [w (format "(ns %s \n (:require [clojure.spec] [dadyspec.core])) \n\n" ns-name)]
    (->> (map str spec-list)
         (interpose "\n")
         (cons w)
         (clojure.string/join))))


#_(defn write-spec-to-file [dir package-name spec-list]
  (let [as-dir (clojure.string/join "/" (clojure.string/split package-name #"\."))
        file-path (str dir "/" as-dir ".clj")
        file-str (as-file-str package-name spec-list)]
    (with-open [w (clojure.java.io/writer file-path)]
      (.write w file-str))))



(comment


  (format "(ns %s \n (:require [clojure.spec] \n [dadyspec.core]) " "com.dir")

  (->> (gen-spec :app '{:student {:req {:id int?}}})
       (write-spec-to-file "src" "hello")

       )

  )


