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
    (list 's/def (as-key (conj ns-coll :spec)) (list 's/keys :req-un (into [] (keys m))))
    (map
      (fn [[k v]]
        (list 's/def k v))
      m)))

(defn as-parent-ns [file-name]
  (-> (clojure.string/split file-name #"\.")
      (first)
      (keyword)))


(defn load-param-spec [file-name m]
  (if (contains? m :dadysql.core/param-spec)
    (let [parent-ns (as-parent-ns file-name)
          n (:dadysql.core/name m)
          ns [parent-ns n]
          w (:dadysql.core/param-spec m)
          w (->> (namespace-key ns w)
                 (build-sepc ns))
          k (as-key [parent-ns n :spec])]
      (eval w)
      (assoc m :dadysql.core/spec k))
    m))


(defn find-registry [f-name]
  (w/postwalk (fn [v]
                (if (map? v)
                  (into {} (filter (fn [[k v]]
                                     (clojure.string/includes? (str k) (str f-name))
                                     ) v))
                  v)
                ) (s/registry)))



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

  (find-registry :tie3)


  (s/explain :tie3.get-dept-by-id/spec {:id [1 2 3 "asdf"]} )

  )
