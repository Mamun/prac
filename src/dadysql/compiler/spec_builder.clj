(ns dadysql.compiler.spec-builder
  (:require [clojure.spec :as s]
            [clojure.walk :as w]))


(defn as-key [v]
  (let [na (reduce str (interpose "." (map name (butlast v))))
        k (name (last v))]
    (keyword (str na "/" k))))


(defn namespace-key [ns-coll m]
  (->> m
       (map (fn [[k v]]
              (let [n1 (conj ns-coll k)
                    r1 (as-key n1)]
                [r1 v])))
       (into {})))


(defn build-sepc [ns-coll m]
  (cons
    (list 'clojure.spec/def (as-key (conj ns-coll :spec)) (list 'clojure.spec/keys :req-un (into [] (keys m))))
    (map
      (fn [[k v]]
        (list 'clojure.spec/def k v))
      m)))


(defn as-parent-ns [file-name]
  (-> (clojure.string/split file-name #"\.")
      (first)
      (keyword)))


(defn eval-param-spec [file-name m]
  (if (contains? m :dadysql.core/param-spec)
    (let [parent-ns (as-parent-ns file-name)
          n (:dadysql.core/name m)
          ns [parent-ns n]
          k (as-key [parent-ns n :spec])]

      (->> (:dadysql.core/param-spec m)
           (namespace-key ns )
           (build-sepc ns)
           (eval )
           )
      (assoc m :dadysql.core/param-spec k
               :dadysql.core/param-spec-defined (:dadysql.core/param-spec m) ))
    m))


(defn registry-by-namespace [n-name]
  (->> (s/registry)
       (w/postwalk (fn [v]
                     (if (map? v)
                       (->> v
                            (filter (fn [[k _]]
                                      (clojure.string/includes? (str k) (str n-name))))
                            (into {}))
                       v)))))



(comment



  (as-key [:hello :get-name :id])


  (clojure.string/includes? (str :hello) (str :he))

  ;(namespace (symbol "adsf") )

  (let [n [:hello :get-by-id]
        m {:id   (var int?)
           :name (var string?)}]
    (->> (namespace-key n m)
         (build-sepc n)
         #_(eval)))

  (s/explain :hello.get-by-id/spec {:id 1 :name 3})

  (:hello.get-by-id/spec
    (s/registry))

  (registry-by-namespace :tie3)


  (s/explain :tie3.get-dept-by-id/spec {:id [1 2 3 "asdf"]})




  (->
    (#'clojure.spec/coll-of
      #'clojure.core/int?
      :kind
      #'clojure.core/vector?)
    (first )
    ;(name )
    )

  )
