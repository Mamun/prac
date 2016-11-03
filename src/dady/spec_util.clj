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

(defn merge-spec2 [coll-spec]
  (cons 'clojure.spec/merge
        coll-spec))


(defn merge-spec [coll-spec]
  (eval
    (cons 'clojure.spec/merge
          coll-spec)))



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



(defn assoc-spec2 [file-name coll]
  (let [f-k (filename-as-keyword file-name)
        s-m (get-param-spec coll)
        nps (sg/assoc-ns-key f-k s-m)
        psk (->> (interleave (keys s-m)
                             (keys nps))
                 (apply assoc {}))
        ;_ (clojure.pprint/pprint  psk )
        xf (fn [m]
             (if (and (contains? m :dadysql.core/param-spec)
                      (get psk (:dadysql.core/name m)))
               (assoc m :dadysql.core/spec (get psk (:dadysql.core/name m)))
               m))]
    (mapv xf coll)))



(defn eval-and-assoc-param-spec [file-name coll]
  (let [f-k (filename-as-keyword file-name)]
    (->> (get-param-spec coll)
         (sg/map->spec f-k)
         (sg/eval-spec))
    (assoc-spec2 file-name coll)))



(defn write-param-spec [file-name coll]
  (let [f-k (filename-as-keyword file-name)]
    (->> (get-param-spec coll)
         ;(assoc-ns-key f-k)
         (sg/map->spec f-k)
         (write-spec-to-file (str (name f-k))))))



