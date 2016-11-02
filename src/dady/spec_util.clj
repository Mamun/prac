(ns dady.spec-util
  (:require [clojure.spec :as s]
            [clojure.pprint]
            [clojure.walk :as w]))


;;Does not execute macro
(defn write-spec-to-file [file-name v]
  (let [])
  (with-open [w (clojure.java.io/writer (str "target/" file-name ".clj") )]
    (.write w (str "(ns " file-name "  \n  (:require [clojure.spec]))") )
    (.write w "\n")
    (doseq [v1 v]
      (.write w (str v1))
      (.write w "\n"))))



(defn eval-spec [coll-v]
  (doseq [v coll-v]
    (eval v)))


(defn registry-by-namespace [n-name]
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



(defn create-ns-key [ns-key r]
  (let [w (if (namespace ns-key)
            (str (namespace ns-key) "." (name ns-key))
            (name ns-key))]
    (if (namespace r)
      (keyword (str w "." (namespace r) "/" (name r)))
      (keyword (str w "/" (name r))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn assoc-ns-key [ns-key m]
  (->> (map (fn [v]
              (create-ns-key ns-key v)) (keys m))
       (interleave (keys m))
       (apply assoc {})
       (clojure.set/rename-keys m)))


(defn convert-key-to-ns-key [m]
  (w/prewalk (fn [w]
               (if (and (map? w)
                        (every? map? (vals w)))
                 (into {}
                       (map (fn [[k v]]
                              {k (assoc-ns-key k v)}
                              ) w))
                 w)
               )  m ))



(defn convert-to-spec [m]
  (let [build-spec-one (fn [w]
                         (map
                           (fn [[k v]]
                             (list 'clojure.spec/def k v))
                           w))]
    (map (fn [[k v]]
           (reverse
             (cons
               (list 'clojure.spec/def k (list 'clojure.spec/keys :req-un (into [] (keys v))))
               (build-spec-one v)))) m )))




(defn remove-quote [m]
  (clojure.walk/prewalk (fn [v]
                          (if (var? v)
                            (symbol (clojure.string/replace (str v) #"#'" ""))
                            v)
                          ) m ))


(defn map->spec [parent-ns-keyword m]
  (->> m
       (remove-quote)
       (assoc-ns-key parent-ns-keyword ) ;; add parent
       (convert-key-to-ns-key)
       (convert-to-spec)
       (apply concat)))




(comment

  ;(convert-to-symbol {:check {:id #'clojure.core/int?} })

  (map->spec :hello {:check {:id #'clojure.core/int?} })

  )







;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;






(comment

  (let [w {:a 3 :b 4}]

    (->> (interleave (keys w)
                     (keys (assoc-ns-key :hello w)))
         (apply assoc {})))



  (println "Hello9")

  )


(defn assoc-spec [spec-name-m m]
  (if (and (contains? m :dadysql.core/param-spec)
           (get spec-name-m (:dadysql.core/name m)))
    (assoc m :dadysql.core/spec (get spec-name-m (:dadysql.core/name m)))
    m))



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


(defn map->spec-key [pns m]
  (->> (interleave (keys m)
                   (keys (assoc-ns-key pns m)))
       (apply assoc {})))



(defn eval-param-spec [file-name coll]
  (let [f-k (filename-as-keyword file-name)
        s-m (get-param-spec coll)
        psk (map->spec-key f-k s-m)]
    (eval-spec (map->spec f-k s-m))
    (mapv #(assoc-spec psk %) coll)))



(defn write-param-spec [file-name coll]
  (let [f-k (filename-as-keyword file-name)
        s-m (get-param-spec coll)
        psk (map->spec f-k s-m)]
    (write-spec-to-file (str (name f-k)) psk )))
