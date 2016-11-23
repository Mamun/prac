(ns dadymodel.core
  (:require [clojure.walk :as w]
            [dadymodel.spec-generator :as impl]
            [dadymodel.util :as u]
            [clojure.spec :as s]
            [clojure.string]
            [dadymodel.xtype :as sg]
            [dadymodel.join.join-key-impl :as dj-impl]
            [dadymodel.join.core :as j-impl]))


(s/def ::email (s/with-gen (s/and string? sg/email?)
                           #(s/gen #{"test@test.de" "clojure@clojure.de" "fun@fun.de"})))
(s/def ::x-int (s/with-gen (s/conformer sg/x-int? (fn [_ v] (str v))) (fn [] sg/x-int-gen)))
(s/def ::x-integer (s/with-gen (s/conformer sg/x-integer? (fn [_ v] (str v))) (fn [] sg/x-integer-gen)))
(s/def ::x-double (s/with-gen (s/conformer sg/x-double? (fn [_ v] (str v))) (fn [] sg/x-double-gen)))
(s/def ::x-boolean (s/with-gen (s/conformer sg/x-boolean? (fn [_ v] (str v))) (fn [] sg/x-boolean-gen)))
(s/def ::x-keyword (s/with-gen (s/conformer sg/x-keyword? (fn [_ v] (str v))) (fn [] sg/x-keyword-gen)))
(s/def ::x-inst (s/with-gen (s/conformer sg/x-inst? (fn [_ v] (str v))) (fn [] sg/x-inst-gen)))
(s/def ::x-uuid (s/with-gen (s/conformer sg/x-uuid? (fn [_ v] (str v))) (fn [] sg/x-uuid-gen)))



(def ^:dynamic *conformer-m*
  {'integer?              ::x-integer
   'clojure.core/integer? ::x-integer
   'int?                  ::x-int
   'clojure.core/int?     ::x-int
   'boolean?              ::x-boolean
   'clojure.core/boolean? ::x-boolean
   'double?               ::x-double
   'clojure.core/double?  ::x-double
   'keyword?              ::x-keyword
   'clojure.core/keyword  ::x-keyword
   'inst?                 ::x-inst
   'clojure.core/inst?    ::x-inst
   'uuid?                 ::x-uuid
   'clojure.core/uuid?    ::x-uuid})


(defn- conform* [m]
  (clojure.walk/postwalk (fn [s]
                           (if-let [r (get *conformer-m* s)]
                             r
                             s)
                           ) m))



(s/def :dadymodel.core/req (s/every-kv keyword? any? :min-count 1))
(s/def :dadymodel.core/opt (s/every-kv keyword? any? :min-count 1))

(s/def :dadymodel.core/model
  (s/every-kv keyword?
              (s/merge (s/keys :opt-un [:dadymodel.core/opt :dadymodel.core/req]) (s/map-of #{:req :opt} any?))
              :min-count 1))



(s/def :dadymodel.core/join
  (clojure.spec/*
    (clojure.spec/alt
      :one (s/tuple keyword? keyword? #{:dadymodel.core/rel-1-1 :dadymodel.core/rel-1-n :dadymodel.core/rel-n-1} keyword? keyword?)
      :many (s/tuple keyword? keyword? #{:dadymodel.core/rel-n-n} keyword? keyword? (s/tuple keyword? keyword? keyword?)))))


(s/def :dadymodel.core/gen-type (s/coll-of #{:dadymodel.core/qualified :dadymodel.core/un-qualified :dadymodel.core/ex} :pred #{}))

(s/def :dadymodel.core/opt-k (s/merge (s/keys :opt [:dadymodel.core/join :dadymodel.core/gen-type])
                                     (s/map-of #{:dadymodel.core/join :dadymodel.core/gen-type} any?)))


(s/def :dadymodel.core/input (s/cat :base-ns keyword?
                                   :model ::model
                                   :opt ::opt-k))
(s/def :dadymodel.core/output any?)


(defn- var->symbol [v]
  (if (var? v)
    (symbol (clojure.string/replace (str v) #"#'" ""))
    v))

(defn get-type [w t]
  (println t)
  (condp = t
    :dadymodel.core/un-qualified
    (assoc w :fixed? false
             :dadymodel.core/gen-type :dadymodel.core/un-qualified
             :postfix :unq )
    :dadymodel.core/qualified
    (assoc w :fixed? false
             :dadymodel.core/gen-type :dadymodel.core/qualified)

    :dadymodel.core/ex
    (assoc w :fixed? false
             :dadymodel.core/gen-type :dadymodel.core/un-qualified
             :postfix :ex )))


(defn gen-spec
  ([namespace-name model-m opt-config-m]
   (if (s/valid? :dadymodel.core/input [namespace-name model-m opt-config-m])
     (let [gen-type-set (or (:dadymodel.core/gen-type opt-config-m)
                            #{:dadymodel.core/qualified
                              :dadymodel.core/un-qualified
                              :dadymodel.core/ex})
           m (clojure.walk/postwalk var->symbol model-m)]
       (->> (map (fn [t]
                   (if (= t :dadymodel.core/ex)
                     (impl/model->spec namespace-name (conform* m) (get-type opt-config-m t))
                     (impl/model->spec namespace-name m (get-type opt-config-m t)))
                   ) gen-type-set)
            (apply concat)))
     #?(:cljs (throw (js/Error. "Opps! spec validation exception  "))
        :clj  (throw (ex-info (s/explain-str ::input [namespace-name model-m opt-config-m]) {})))))
  ([namespace-name model-m]
   (gen-spec namespace-name model-m {})))


(s/fdef gen-spec :args ::input :ret ::output)


(defmacro defmodel
  ([m-name m & {:as opt-m}]
   (let [m-name (keyword m-name)
         opt-m (if (nil? opt-m)
                 (gen-spec m-name m)
                 (gen-spec m-name m opt-m))]
     `~(cons 'do opt-m))))


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
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn do-assoc-relation-key [data join-coll]
  (dj-impl/assoc-join-key data join-coll))


(defn do-disjoin [data join-coll]
  (j-impl/do-disjoin-impl data join-coll))


(defn do-join [data join-coll]
  (j-impl/do-join-impl data join-coll))



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



(defn as-file-str [ns-name spec-list]
  (let [w (str "(ns " ns-name " \n (:require [clojure.spec :as s] [dadymodel.core])) \n\n")]
    (->> (map str spec-list)
         (interpose "\n")
         (cons w)
         (clojure.string/join))))


#?(:clj

   (defn write-spec-to-file* [dir spec-des]
     (let [package-name (name (first spec-des))
           spec-list (apply gen-spec spec-des)
           file-str (as-file-str package-name spec-list)

           as-dir (clojure.string/join "/" (clojure.string/split package-name #"\."))
           file-path (str dir "/" as-dir ".cljc")]
       (spit file-path file-str)))

   )


#?(:clj
   (defmacro write-spec-to-file [dir & spec-des]
     (write-spec-to-file* dir spec-des))

   )



(comment




  #_(s/write-spec-to-file
      "dev"
      :app.spec
      {:company {:req {:name string?
                       :id   int?
                       :type (s/coll-of (s/and keyword? #{:software :hardware})
                                        :into #{})}}})


  #_(format "(ns %s \n (:require [clojure.spec] \n [dadymodel.core]) " "com.dir")

  #_(write-spec-to-file "src" :app '{:student {:req {:id int?}}} {:gen-type #{:ex}})

  )


