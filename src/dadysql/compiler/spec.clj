(ns dadysql.compiler.spec
  (:require [clojure.spec :as s]
            [dadysql.constant :as c]))


(defn any? [_] true)

(s/def ::isolation #{:none :read-committed :read-uncommitted :repeatable-read :serializable})
(s/def ::read-only? #{true false })
(s/def ::tx-prop  any? #_(s/keys :opt-un [::isolation ::read-only]))
(s/def ::file-reload boolean?)
(s/def ::reserve-name (s/+ keyword?))

(s/def ::join (clojure.spec/*
                (clojure.spec/alt
                  :1-* (s/tuple keyword? keyword? #{:1-1 :1-n :n-1} keyword? keyword?)
                  :n-n (s/tuple keyword? keyword? #(= % :n-n) keyword? keyword? (s/tuple keyword? keyword? keyword?) ))))


(s/def ::doc string?)
(s/def ::timeout integer?)

(s/def ::name (s/or :one keyword? :many (s/* keyword?)))
(s/def ::gname keyword?)

(s/def ::sql   (s/and string? #(not (clojure.string/blank? %))))
(s/def ::model (s/or :one keyword? :many (s/* keyword?)))
(s/def ::skip (s/coll-of keyword? #{}) )
(s/def ::group keyword?)
(s/def ::commit #{c/commit-all-key c/commit-none-key c/commit-any-key})
(s/def ::column (s/map-of keyword? keyword?))
(s/def ::result (s/coll-of keyword? #{:array}) )




(s/def ::param-ref-con (clojure.spec/tuple keyword? #(= :ref-con %)  any?))
(s/def ::param-ref     (clojure.spec/tuple keyword? #(= :ref-key %) keyword?))
(s/def ::param-ref-fn  (clojure.spec/tuple keyword? #(= :ref-fn-key %) resolve keyword?))
(s/def ::param-ref-gen (clojure.spec/tuple keyword? #(= :ref-gen %) keyword?))

(s/def ::params (clojure.spec/* (clojure.spec/alt :param-ref-con ::param-ref-con
                                                  :param-ref-fn ::param-ref-fn
                                                  :param-ref-gen ::param-ref-gen
                                                  :param-ref ::param-ref)))



(s/def ::validation
  (clojure.spec/*
    (clojure.spec/alt
      :type (clojure.spec/tuple keyword? #(= :type %) resolve string?)
      :type-contain  (clojure.spec/tuple keyword? #(= :contain %) resolve string?)
      :range (clojure.spec/tuple keyword? #(= :range %) integer? integer? string?))))




(s/def ::extend (s/keys :opt-un [::timeout ::column ::result ::params ::validation]))

(s/def ::module (s/keys :req-un [::name ::sql]
                        :opt-un [::model ::skip ::group ::commit ::column ::result ::params ::validation ::extend] ))


(s/def ::global (s/keys :req-un [::name]
                        :opt-un [::read-only? ::tx-prop ::file-reload ::reserve-name ::join ]))


(s/def ::spec (clojure.spec/* (clojure.spec/alt :module ::module :global ::global )) )



(defn valid-module? [v]
  (if (s/valid? ::module v)
    v
    (do (clojure.pprint/pprint v)
        (throw (s/explain ::module v)))))


(defn valid-global? [v]
  (if (s/valid? ::global v)
    v
    (do
      (clojure.pprint/pprint v)
      (throw (s/explain ::global v)))))



(defn as-map [w]
  (reduce (fn [acc v]
            (update-in acc [(first v)] (fn [w]
                                         (if w
                                           (cons (second v) w)
                                           (list (second v)))))){} w))



(comment




  (map? {:a 3})



  (let [v [{:a 4} "list b"
           {:a 5} "list c"]
        w (s/*
            (s/or

              :op (s/cat :v map? :sql string?)))]
    (s/explain w v)
    (s/conform w v)))









