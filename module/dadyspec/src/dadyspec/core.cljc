(ns dadyspec.core
  (:require [clojure.walk :as w]
            [dadyspec.spec-generator :as impl]
            [dadyspec.util :as u]
            [clojure.spec :as s]
            [clojure.string]
            [dadyspec.core-xtype :as sg]
            [dadyspec.join.disjoin-impl :as dj-impl]
            [dadyspec.join.join-impl :as j-impl]))


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



(s/def :dadyspec.core/req (s/every-kv keyword? any? :min-count 1))
(s/def :dadyspec.core/opt (s/every-kv keyword? any? :min-count 1))

(s/def :dadyspec.core/model
  (s/every-kv keyword?
              (s/merge (s/keys :opt-un [:dadyspec.core/opt :dadyspec.core/req]) (s/map-of #{:req :opt} any?))
              :min-count 1))



(s/def :dadyspec.core/join
  (clojure.spec/*
    (clojure.spec/alt
      :one  (s/tuple keyword? keyword? #{:dadyspec.core/rel-1-1 :dadyspec.core/rel-1-n :dadyspec.core/rel-n-1} keyword? keyword?)
      :many (s/tuple keyword? keyword? #{:dadyspec.core/rel-n-n} keyword? keyword? (s/tuple keyword? keyword? keyword?)))))


(s/def :dadyspec.core/gen-type (s/coll-of #{:dadyspec.core/qualified :dadyspec.core/un-qualified :dadyspec.core/ex} :pred #{}))

(s/def :dadyspec.core/opt-k (s/merge (s/keys :opt [:dadyspec.core/join :dadyspec.core/gen-type])
                                     (s/map-of #{:dadyspec.core/join :dadyspec.core/gen-type} any?)))


(s/def :dadyspec.core/input (s/cat :base-ns keyword?
                                   :model ::model
                                   :opt ::opt-k))
(s/def :dadyspec.core/output any?)


(defn- var->symbol [v]
  (if (var? v)
    (symbol (clojure.string/replace (str v) #"#'" ""))
    v))


(defn gen-spec
  ([namespace-name model-m opt-config-m]
   (if (s/valid? :dadyspec.core/input [namespace-name model-m opt-config-m])
     (let [join (or (:dadyspec.core/join opt-config-m) [])
           gen-type (or (:dadyspec.core/gen-type opt-config-m)
                        #{:dadyspec.core/un-qualified})

           m (clojure.walk/postwalk var->symbol model-m)
           q-list (when (contains? gen-type :dadyspec.core/qualified)
                    (->> {:fixed? false
                          :dadyspec.core/gen-type :dadyspec.core/qualified
                          :dadyspec.core/join join}
                         (impl/model->spec namespace-name m)))
           unq-list (when (contains? gen-type :dadyspec.core/un-qualified)
                      (->> {:fixed? false
                            :dadyspec.core/gen-type :dadyspec.core/un-qualified
                            :postfix "un-"
                            :dadyspec.core/join join}
                           (impl/model->spec namespace-name m)))
           ex-list (when (contains? gen-type :dadyspec.core/ex)
                     (->> {:dadyspec.core/join join
                           :fixed? false
                           :dadyspec.core/gen-type :dadyspec.core/un-qualified
                           :postfix "ex-"}
                          (impl/model->spec namespace-name (conform* m))))]
       (concat q-list unq-list ex-list))
     #?(:cljs (throw (js/Error. "Opps! spec validation exception  "))
        #_(s/explain-data ::input [namespace-name model-m opt-config-m])
        :clj  (throw (ex-info (s/explain-str ::input [namespace-name model-m opt-config-m]) {})))))
  ([namespace-name model-m]
   (gen-spec namespace-name model-m {:dadyspec.core/join     []
                                     :dadyspec.core/gen-type #{:dadyspec.core/un-qualified}})))


(s/fdef gen-spec :args ::input :ret ::output)


(defmacro defentity
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


(defn do-disjoin [join-coll data]
  (dj-impl/do-disjoin (dj-impl/assoc-join-key data join-coll) join-coll))


(defn do-join [join-coll data ]
  (j-impl/do-join data join-coll))



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
  (let [w (str "(ns " ns-name " \n (:require [clojure.spec :as s] [dadyspec.core])) \n\n")]
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


  #_(format "(ns %s \n (:require [clojure.spec] \n [dadyspec.core]) " "com.dir")

  #_(write-spec-to-file "src" :app '{:student {:req {:id int?}}} {:gen-type #{:ex}})

  )


