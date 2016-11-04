(ns dady.spec-util
  (:require [clojure.spec :as s]
            [clojure.pprint]
            [dady.spec-generator :as sg]
            [clojure.walk :as w]))


;;Does not execute macro
(defn write-spec-to-file [file-name v]
  (with-open [w (clojure.java.io/writer (str "target/" file-name ".clj"))]
    (.write w (str "(ns " file-name "  \n  (:require [clojure.spec]))"))
    (.write w "\n")
    (doseq [v1 v]
      (.write w (str v1))
      (.write w "\n"))))





(defn registry [n-name]
  (->> (s/registry)
       (w/postwalk (fn [v]
                     (if (map? v)
                       (->> v
                            (filter (fn [[k _]]
                                      (clojure.string/includes? (str k) (str n-name))))
                            (into {}))
                       v)))))


;(as-ns-keyword :a :n)


#_(defn merge-spec [coll-spec]
    (eval
      (cons 'clojure.spec/merge
            coll-spec)))


#_(defn map-un-spec [tm-coll]
    (->> (map :dadysql.core/spec tm-coll)
         (remove nil?)
         (map (fn [w] (sg/add-postfix-to-key w sg/un-postfix)))
         (merge-spec)))



(defn filename-as-keyword [file-name-str]
  (-> (clojure.string/split file-name-str #"\.")
      (first)
      (keyword)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn get-param-spec [coll]
  (let [insert-coll (->> (filter :dadysql.core/param-spec coll)
                         (filter (fn [m] (= (:dadysql.core/dml m)
                                            :dadysql.core/dml-insert)))
                         (group-by :dadysql.core/model)
                         (map (fn [[k v]] {k (apply merge (mapv :dadysql.core/param-spec v))}))
                         (into {}))]
    (->> (filter :dadysql.core/param-spec coll)
         (into {} (map (juxt :dadysql.core/name :dadysql.core/param-spec)))
         (merge insert-coll))))


(defn gen-spec [file-name coll]
  (let [f-k (filename-as-keyword file-name)]
    (->> (get-param-spec coll)
         (sg/model->spec f-k))))



(defn get-spec-map [file-name coll]
  (let [f-k (filename-as-keyword file-name)
        s-m (get-param-spec coll)
        nps (sg/assoc-ns-key f-k s-m)]
    (->> (interleave (keys s-m)
                     (keys nps))
         (apply assoc {}))))



(defn assosc-spec-to-m [psk m]
  (if (and (contains? m :dadysql.core/param-spec)
           (get psk (:dadysql.core/name m)))
    (if (= (:dadysql.core/dml m)
           :dadysql.core/dml-insert)
      (assoc m :dadysql.core/spec (get psk (:dadysql.core/model m)))
      (assoc m :dadysql.core/spec (get psk (:dadysql.core/name m))))
    m))



(defn eval-and-assoc-param-spec [file-name coll]
  (do
    (apply sg/eval-spec (gen-spec file-name coll ) )
    (let [m (get-spec-map file-name coll)]
      (mapv (fn [w] (assosc-spec-to-m m w) ) coll))))









