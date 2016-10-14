(ns dadysql.compiler.spec
  (:require [clojure.spec :as s]
            [clojure.walk :as w]))

(s/def :dadysql.core/dml #{:dadysql.core/dml-select
                           :dadysql.core/dml-insert
                           :dadysql.core/dml-update
                           :dadysql.core/dml-delete
                           :dadysql.core/dml-call})


(s/def :dadysql.core/commit #{:dadysql.core/commit-all
                              :dadysql.core/commit-none
                              :dadysql.core/commit-any})

(s/def :dadysql.core/tx-prop (s/cat :ck #{:isolation}
                                    :cv (s/spec #{:none :read-committed :read-uncommitted :repeatable-read :serializable})
                                    :rk #{:read-only?}
                                    :rv (s/spec boolean?)))


(s/def :dadysql.core/file-reload boolean?)
(s/def :dadysql.core/reserve-name (s/with-gen (s/every keyword? :kind set?)
                                              (fn []
                                                (s/gen #{#{:create-ddl :drop-ddl :init-data}
                                                         #{:init-data}}))))

(s/def :dadysql.core/doc string?)
(s/def :dadysql.core/timeout pos-int?)

(s/def :dadysql.core/name
  (s/or :one keyword?
        :many (s/coll-of keyword? :kind vector? :distinct true)))

(s/def :dadysql.core/index int?)


(s/def :dadysql.core/sql (s/every string? :kind vector?))

(s/def :dadysql.core/model
  (s/or :one keyword?
        :many (s/coll-of keyword? :kind vector?)))

(s/def :dadysql.core/skip (s/every keyword? :kind set?))

(s/def :dadysql.core/group keyword?)
(s/def :dadysql.core/column (s/every-kv keyword? keyword?))
(s/def :dadysql.core/result (s/every #{:dadysql.core/result-array :dadysql.core/result-single} :kind set?))
(s/def :dadysql.core/read-only? boolean?)


(s/def :dadysql.core/join
  (clojure.spec/*
    (clojure.spec/alt
      :one (s/tuple keyword? keyword? #{:dadysql.core/join-one-one :dadysql.core/join-one-many :dadysql.core/join-many-one} keyword? keyword?)
      :many (s/tuple keyword? keyword? #{:dadysql.core/join-many-many} keyword? keyword? (s/tuple keyword? keyword? keyword?)))))



(s/def :dadysql.core/param-coll
  (clojure.spec/*
    (clojure.spec/alt
      :ref-con (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-con} any?)
      :ref-fn-key (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-fn-key} ifn? keyword?)
      :ref-gen (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-gen} keyword?)
      :ref-key (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-key} keyword?))))



(defn ns-keyword? [v]
  (if (namespace v) true false))


(defn clj-spec? [v]
  (let [v (eval v)]
    (if (and (not (keyword? v))
             (or (ifn? v)
                 (clojure.spec/spec? v)
                 (clojure.spec/regex? v)))
      true
      false)))


(s/def :dadysql.core/param-spec (s/map-of keyword? clj-spec?))



(comment

  (s/exercise :dadysql.core/param-spec 1)

  )



(s/def :dadysql.core/common
  (s/merge
    (s/keys :opt [:dadysql.core/timeout
                  :dadysql.core/column
                  :dadysql.core/result
                  :dadysql.core/param-coll
                  :dadysql.core/param-spec])))



(s/def :dadysql.core/extend
  (s/every-kv keyword? (s/merge (s/keys :opt [:dadysql.core/model]) :dadysql.core/common)))



(comment

  (s/exercise :dadysql.core/common)

  (clojure.pprint/pprint
    (s/exercise :dadysql.core/extend 1))

  )



(s/def :dadysql.core/module (s/merge
                              :dadysql.core/common
                              (s/keys :req [:dadysql.core/name :dadysql.core/sql]
                                      :opt [:dadysql.core/model :dadysql.core/skip :dadysql.core/group :dadysql.core/commit :dadysql.core/extend])))


(s/def :dadysql.core/spec-file symbol?)

(s/def :dadysql.core/global (s/keys :req [:dadysql.core/name]
                                    :opt [:dadysql.core/timeout :dadysql.core/read-only? :dadysql.core/tx-prop :dadysql.core/file-reload :dadysql.core/reserve-name :dadysql.core/join :dadysql.core/spec-file]))


(s/def :dadysql.core/compiler-spec
  (clojure.spec/cat
    :global (s/? :dadysql.core/global)
    :module (s/* :dadysql.core/module)))




(defn validate-input-spec! [coll]
  (let [w (s/conform :dadysql.core/compiler-spec coll)]
    (if (= w :clojure.spec/invalid)
      (do
        (println (s/explain-str :dadysql.core/compiler-spec coll))
        (throw (ex-info "Compile failed " (s/explain-data :dadysql.core/compiler-spec coll)))))))







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


(defn file->ns [file-name]
  (-> (clojure.string/split file-name #"\.")
      (first)
      (keyword)))


(defn eval-param-spec [file-name m]
  (if (contains? m :dadysql.core/param-spec)
    (let [parent-ns (file->ns file-name)
          n (:dadysql.core/name m)
          ns [parent-ns n]
          k (as-key [parent-ns n :spec])]

      (->> (:dadysql.core/param-spec m)
           (namespace-key ns )
           (build-sepc ns)
           (eval ))
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
