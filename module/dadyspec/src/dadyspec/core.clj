(ns dadyspec.core
  (:require [clojure.walk :as w]
            [dadyspec.core-impl :as sc]
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


(s/def ::email?
  (s/with-gen (s/and string? email?)
              #(s/gen #{"test@test.de" "clojure@clojure.de" "fun@fun.de"})))



(comment



  (s/valid? ::email? "a.dsfas@test.de")

  (s/exercise ::email?)

  )



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




;(.toDate (org.joda.time.DateTime/parse x))

(defn as-conformer [s]
  (if (symbol? s)
    (do
      (condp contains? s
        #{'integer?
          'clojure.core/integer?} `(clojure.spec/conformer dadyspec.core/x-integer?)
        #{'int?
          'clojure.core/int?} (do
                                `(clojure.spec/conformer dadyspec.core/x-int?))
        #{'boolean?
          'clojure.core/boolean?} `(clojure.spec/conformer dadyspec.core/x-boolean?)
        #{'double?
          'clojure.core/double?} `(clojure.spec/conformer dadyspec.core/x-double?)
        #{'keyword?
          'clojure.core/keyword} `(clojure.spec/conformer dadyspec.core/x-keyword?)
        #{'inst?
          'clojure.core/inst?} `(clojure.spec/conformer dadyspec.core/x-inst?)
        #{'uuid?
          'clojure.core/uuid?} `(clojure.spec/conformer dadyspec.core/x-uuid?)
        s))
    s))


#_(defn k-value? [v]
    ;(println v "-- " (type v))
    (symbol? v)
    )

(s/def ::req (s/every-kv keyword? any? :min-count 1))
(s/def ::opt (s/every-kv keyword? any? :min-count 1))

(s/def ::model
  (s/every-kv keyword?
              (s/merge (s/keys :opt-un [::opt ::req]) (s/map-of #{:req :opt} any?))
              :min-count 1))


(def rel-type-set #{::one-one ::one-many ::many-one})
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


(defn- gen-spec-impl [base-ns-name join-list m]
  (let [j-m (sc/format-join base-ns-name join-list)
        f (->> m
               (sc/assoc-ns-key base-ns-name)
               (map (partial sc/model->spec2 true j-m))
               (apply concat))

        j-m (sc/format-join (sc/add-postfix-to-key base-ns-name "-un") join-list)

        f2 (->> m
                (sc/assoc-ns-key (sc/add-postfix-to-key base-ns-name "-un"))
                (map (partial sc/model->spec2 false j-m))
                (apply concat))
        j-m (sc/format-join (sc/add-postfix-to-key base-ns-name "-ex") join-list)

        w (->> m
               (clojure.walk/postwalk as-conformer)
               (sc/assoc-ns-key (sc/add-postfix-to-key base-ns-name "-ex"))
               (map (partial sc/model->spec2 false j-m))
               (apply concat))]
    (concat f f2 w)
    #_(into w (reverse f))))


(defn gen-spec
  ([m-name m join]
   (if (s/valid? ::input [m-name m join])
     (->> (clojure.walk/postwalk var->symbol m)
          (gen-spec-impl m-name join))
     (throw (ex-info "failed " (s/explain-data ::input [m-name m join])))))
  ([m-name m]
   (gen-spec m-name m [])))


;(sc/create-ns-key :hello :a)


(defmacro defsp
  ([m-name m & [join]]
   (let [m-name (keyword m-name)]
     (let [r (if join
               (gen-spec m-name m join)
               (gen-spec m-name m)
               )]
       `~(cons 'do r)))))



(comment


  (gen-spec :model '{:person {:opt {:id int?}}})
  (gen-spec :model '{:person {:opt {:id clojure.core/int?}}})

  (macroexpand-1 '(defsp :model {:person {:opt {:name string?}}}))

  (defsp :model3 {:dept    {:req {:name string?
                                  :id   int?}}
                  :student {:req {:name string? :id int?}}}
         [[:dept :1-n :student]])



  (s/exercise :model3/dept)

  (s/conform :model3/person {:id2 3})
  ;(->> )
  (s/conform :model3/person {:id2 3})
  (s/conform :model3/person-list [{:id2 3}])

  (->> (s/conform :model3-ex/person {:id2 "3"})
       (s/unform :model3/person))


  (->> (s/conform :model3-ex/person-list [{:id2 "3"} {:id2 "4"}])
       (s/unform :model3/person-list))

  )


;;;;;;;;;;;;;;;;;;;;;;;;;;Additional spec




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
        file-path (str dir "/" as-dir ".clj")]
    (with-open [w (clojure.java.io/writer file-path)]
      (.write w (str "(ns " package-name "  \n  (:require [clojure.spec] \n [dadyspec.core]))"))
      (.write w "\n")
      (doseq [v1 spec-list]
        (.write w (str v1))
        (.write w "\n")))))



(comment

  (->> (gen-spec :app '{:student {:req {:id int?}}})
       (write-spec-to-file "test" "app")
       )

  )


