(ns dady.spec-util
  (:require [clojure.spec :as s]
            [clojure.pprint]
            [clojure.walk :as w]))


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


(defn as-ns-format [m]
  (->> m
       (w/prewalk (fn [w]
                    (if (and (map? w)
                             (every? map? (vals w)))
                      (into {}
                            (map (fn [[k v]]
                                   {k (assoc-ns-key k v)}
                                   ) w))
                      w)
                    ))))




(defn build-spec-one [m]
  (map
    (fn [[k v]]
      (list 'clojure.spec/def k v))
    m))


(defn build-spec-batch [m]
  (->> m
       (map (fn [[k v]]
              (reverse
                (cons
                  (list 'clojure.spec/def k (list 'clojure.spec/keys :req-un (into [] (keys v))))
                  (build-spec-one v)))))))



(defn remove-quote [m]
  (clojure.walk/prewalk (fn [v]
                          (if (var? v)
                            (symbol (clojure.string/replace (str v) #"#'" ""))
                            v)
                          ) m ))


(defn map->spec [parent-ns-keyword m]
  (->> m
       (remove-quote)
       (assoc-ns-key parent-ns-keyword )
       (as-ns-format)
       (build-spec-batch)
       (apply concat)))



(comment


  ;(convert-to-symbol {:check {:id #'clojure.core/int?} })

  (map->spec :hello {:check {:id #'clojure.core/int?} })



  )



(defn map->spec-key [pns m]
  (->> (interleave (keys m)
                   (keys (assoc-ns-key pns m)))
       (apply assoc {})))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;Does not execute macro
(defn write-to-file [file-name v]
  (let [])
  (with-open [w (clojure.java.io/writer (str file-name ".clj") )]
    (.write w (str "(ns " file-name "  \n  (:require [clojure.spec :as s]))") )
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





(comment

  (let [w {:a 3 :b 4}]

    (->> (interleave (keys w)
                     (keys (assoc-ns-key :hello w)))
         (apply assoc {})))



  (println "Hello9")

  )




