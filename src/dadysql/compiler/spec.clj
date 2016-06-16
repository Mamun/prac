(ns dadysql.compiler.spec
  (:use [dadysql.constant])
  (:require [clojure.spec :as s]))


(defn any? [_] true)

(defn resolve? [v]
  (if (resolve v) true false))



(s/def ::isolation #{:none :read-committed :read-uncommitted :repeatable-read :serializable})
(s/def ::read-only? #{true false})
(s/def ::tx-prop any? #_(s/keys :opt-un [::isolation ::read-only]))
(s/def ::file-reload boolean?)
(s/def ::reserve-name (s/+ keyword?))



(s/def ::doc string?)
(s/def ::timeout integer?)

(s/def ::name (s/or :one keyword? :many (s/* keyword?)))
(s/def ::gname keyword?)

(s/def ::sql (s/and string? #(not (clojure.string/blank? %))))
(s/def ::model (s/or :one keyword? :many (s/* keyword?)))
(s/def ::skip (s/coll-of keyword? #{}))
(s/def ::group keyword?)
(s/def ::commit #{:all :any :none})
(s/def ::column (s/map-of keyword? keyword?))
(s/def ::result (s/coll-of keyword? #{:array}))



(s/def ::1-* (s/tuple keyword? keyword? #{join-1-1-key join-1-n-key join-n-1-key} keyword? keyword?))
(s/def ::n-n (s/tuple keyword? keyword? #(= % join-n-n-key) keyword? keyword? (s/tuple keyword? keyword? keyword?)))

(s/def ::join
  (clojure.spec/*
    (clojure.spec/alt
      :1-* ::1-*
      :n-n ::n-n)))



(s/def ::param-ref-con (clojure.spec/tuple keyword? #(= param-ref-con-key %) any?))
(s/def ::param-ref     (clojure.spec/tuple keyword? #(= param-ref-key %) keyword?))
(s/def ::param-ref-fn  (clojure.spec/tuple keyword? #(= param-ref-fn-key %) resolve? keyword?))
(s/def ::param-ref-gen (clojure.spec/tuple keyword? #(= param-ref-gen-key %) keyword?))




(s/def ::params
  (clojure.spec/*
    (clojure.spec/alt
      :param-ref-con ::param-ref-con
      :param-ref-fn ::param-ref-fn
      :param-ref-gen ::param-ref-gen
      :param-ref ::param-ref)))



(s/def ::vali-type         (clojure.spec/tuple keyword? #(= validation-type-key %) resolve? string?))
(s/def ::vali-type-contain (clojure.spec/tuple keyword? #(= validation-contain-key %) resolve? string?))
(s/def ::vali-range        (clojure.spec/tuple keyword? #(= validation-range-key %) integer? integer? string?))



(s/def ::validation
  (clojure.spec/*
    (clojure.spec/alt
      :type ::vali-type
      :type-contain ::vali-type-contain
      :range ::vali-range)))




(s/def ::extend (s/keys :opt-un [::timeout ::column ::result ::params ::validation]))

(s/def ::module (s/keys :req-un [::name ::sql]
                        :opt-un [::model ::skip ::group ::commit ::column ::result ::params ::validation ::extend]))


(s/def ::global (s/keys :req-un [::name]
                        :opt-un [::read-only? ::tx-prop ::file-reload ::reserve-name ::join]))


(s/def ::spec (clojure.spec/* (clojure.spec/alt :module ::module :global ::global)))



















