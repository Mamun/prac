(ns dadyspec.core
  (:require [clojure.walk :as w]
            [dadyspec.core-impl :as impl]
            [dadyspec.util :as u]
            [cheshire.core :as ch]
            [clojure.spec :as s])
  (:import [BigInteger]
           [Double]
           (org.joda.time DateTime)
           (java.util Date UUID)))



(defn- matches-regex?
  "Returns true if the string matches the given regular expression"
  [v regex]
  (boolean (re-matches regex v)))


(defn email?
  "Returns true if v is an email address"
  [v]
  (if (nil? v)
    false
    (matches-regex? v
                    #"(?i)[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")))


(s/def ::email
  (s/with-gen (s/and string? email?)
              #(s/gen #{"test@test.de" "clojure@clojure.de" "fun@fun.de"})))



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


(defn x-double? [x]
  (cond
    (double? x) x
    (string? x) (try
                  (Double/parseDouble x)
                  (catch Exception e
                    :clojure.spec/invalid))
    :else :clojure.spec/invalid))


(defn x-boolean? [x]
  (cond
    (boolean? x) x
    (string? x) (cond
                  (= "true" x) true
                  (= "false" x) false
                  :else ::s/invalid)
    :else :clojure.spec/invalid))


(defn x-keyword? [x]
  (cond
    (keyword? x) x
    (string? x) (keyword x)
    :else :clojure.spec/invalid))


(defn x-inst? [x]
  (cond
    (inst? x) x
    (string? x) (try
                  (.toDate (org.joda.time.DateTime/parse x))
                  (catch Exception e
                    :clojure.spec/invalid))
    :else :clojure.spec/invalid))


(defn x-uuid? [x]
  (cond
    (uuid? x) x
    (string? x) (try
                  (UUID/fromString x)
                  (catch Exception e
                    :clojure.spec/invalid))
    :else :clojure.spec/invalid))


(def ^:dynamic *conformer-m*
  {'integer?              `(clojure.spec/conformer dadyspec.core/x-integer?)
   'clojure.core/integer? `(clojure.spec/conformer dadyspec.core/x-integer?)
   'int?                  `(clojure.spec/conformer dadyspec.core/x-int?)
   'clojure.core/int?     `(clojure.spec/conformer dadyspec.core/x-int?)
   'boolean?              `(clojure.spec/conformer dadyspec.core/x-boolean?)
   'clojure.core/boolean? `(clojure.spec/conformer dadyspec.core/x-boolean?)
   'double?               `(clojure.spec/conformer dadyspec.core/x-boolean?)
   'clojure.core/double?  `(clojure.spec/conformer dadyspec.core/x-double?)
   'keyword?              `(clojure.spec/conformer dadyspec.core/x-keyword?)
   'clojure.core/keyword  `(clojure.spec/conformer dadyspec.core/x-keyword?)
   'inst?                 `(clojure.spec/conformer dadyspec.core/x-inst?)
   'clojure.core/inst?    `(clojure.spec/conformer dadyspec.core/x-inst?)
   'uuid?                 `(clojure.spec/conformer dadyspec.core/x-uuid?)
   'clojure.core/uuid?    `(clojure.spec/conformer dadyspec.core/x-uuid?)})

;(.toDate (org.joda.time.DateTime/parse x))

(defn- conform* [m]
  (clojure.walk/postwalk (fn [s]
                           (if-let [r (get *conformer-m* s)]
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


(s/def ::input (s/cat :base-ns keyword?
                      :model ::model
                      :join ::join))
(s/def ::output any?)


(s/fdef gen-spec
        :args ::input
        :ret ::output)


(defn- var->symbol [v]
  (if (var? v)
    (symbol (clojure.string/replace (str v) #"#'" ""))
    v))


(defn gen-spec
  [namespace-name model-m & {:keys [join]
                             :or   {join []}}]
  (if (s/valid? ::input [namespace-name model-m join])
    (let [m (clojure.walk/postwalk var->symbol model-m)]
      (concat (impl/model->spec namespace-name m {:fixed? false :qualified? true :join join})
              (impl/model->spec namespace-name m {:fixed? false :qualified? false :postfix "-un" :join join})
              (impl/model->spec namespace-name (conform* m) {:join join :fixed? false :qualified? false :postfix "-ex"})))
    (throw (ex-info "failed " (s/explain-data ::input [namespace-name model-m join])))))


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
  ([m-name m & {:keys [join]}]
   (let [m-name (keyword m-name)]
     (let [r (if join
               (gen-spec m-name m :join join)
               (gen-spec m-name m)
               )]
       `~(cons 'do r)))))



(defn conform-json [spec json-str]
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


(defn eval-spec [& coll-v]
  (doseq [v coll-v]
    (eval v)))



(defn as-file-str [ns-name spec-list]
  (let [w (format "(ns %s \n (:require [clojure.spec] [dadyspec.core])) \n\n" ns-name)]
    (->> (map str spec-list)
         (interpose "\n")
         (cons w)
         (clojure.string/join))))


(defn write-spec-to-file [dir package-name spec-list]
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


