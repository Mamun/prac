(ns dadyspec.core-xtype
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen])
  (:import [BigInteger]
           [Long]
           [Double]
           (java.text SimpleDateFormat)
           (java.util Date UUID)))


(defn x-int? [x]
  (cond
    (integer? x) x
    (string? x) (try
                  (Integer/parseInt x)
                  (catch Exception e
                    :clojure.spec/invalid))
    :else :clojure.spec/invalid))



(def x-int-gen (gen/fmap #(str %) (gen/int)))

(defn x-integer? [x]
  (cond
    (integer? x) x
    (string? x) (try
                  (Long/parseLong x)
                  (catch Exception e
                    :clojure.spec/invalid))
    :else :clojure.spec/invalid))


(def x-integer-gen (gen/fmap #(str %) (gen/large-integer)))

(defn x-double? [x]
  (cond
    (double? x) x
    (string? x) (try
                  (Double/parseDouble x)
                  (catch Exception e
                    :clojure.spec/invalid))
    :else :clojure.spec/invalid))


(def x-double-gen (gen/fmap #(str %) (gen/double)))

(defn x-boolean? [x]
  (cond
    (boolean? x) x
    (string? x) (cond
                  (= "true" x) true
                  (= "false" x) false
                  :else ::s/invalid)
    :else :clojure.spec/invalid))


(def x-boolean-gen (gen/fmap #(str %) (gen/boolean)))

(defn x-keyword? [x]
  (cond
    (keyword? x) x
    (string? x) (keyword x)
    :else :clojure.spec/invalid))


(def x-keyword-gen (gen/fmap #(name %) (gen/keyword)))

(def date-formater (SimpleDateFormat. "dd-MM-yyyy HH:mm:ss")) ;

(defn x-inst? [x]
  (cond
    (inst? x) x
    (string? x) (try
                  (.parse date-formater x)
                  (catch Exception e
                    :clojure.spec/invalid))
    :else :clojure.spec/invalid))


(def x-inst-gen (gen/fmap #(.format date-formater (java.util.Date. %)) (gen/large-integer)))

(defn x-uuid? [x]
  (cond
    (uuid? x) x
    (string? x) (try
                  (UUID/fromString x)
                  (catch Exception e
                    :clojure.spec/invalid))
    :else :clojure.spec/invalid))


(def x-uuid-gen (gen/fmap #(str %)
                          (gen/uuid)))


(comment

  ;(name :asd)

  (gen/sample x-uuid-gen)

  (gen/sample x-inst-gen)

  (gen/sample x-int-gen)
  (gen/sample x-integer-gen)
  (gen/sample x-double-gen)
  )


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


