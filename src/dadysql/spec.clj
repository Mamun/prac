(ns dadysql.spec
  (:require [clojure.spec :as s]))

(def dml #{:dadysql.core/dml-select
           :dadysql.core/dml-insert
           :dadysql.core/dml-update
           :dadysql.core/dml-delete
           :dadysql.core/dml-call})


(def commit #{:dadysql.core/commit-all
              :dadysql.core/commit-none
              :dadysql.core/commit-any})


(s/def :dadysql.core/dml dml)
(s/def :dadysql.core/commit commit)

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


(s/def ::params-v fn?)
(s/def :dadysql.core/default-param (s/* (s/cat :k keyword? :v ::params-v ) ) #_(s/map-of keyword? ::params-v))

#_(s/def :dadysql.core/default-param
  (clojure.spec/*
    (clojure.spec/alt
      :ref-con (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-con} any?)
      :ref-fn-key (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-fn-key} ifn? keyword?)
      :ref-gen (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-gen} keyword?)
      :ref-key (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-key} keyword?))))



#_(s/conform
  (s/* (s/cat :k keyword? :v ::params-v ) )
  [:a :b :a '(inc :b) :c :b]
  )

#_(group-by (fn [[k v]]
            (keyword? v)
            ) (partition 2   [:a :b :a '(inc :b) :c :b]) )





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


(s/def ::req (s/map-of keyword? clj-spec?))
(s/def ::opt (s/map-of keyword? clj-spec?))
(s/def :dadysql.core/param-spec (s/merge (s/or :req (s/keys :req [::req])
                                   :opt (s/keys :opt [::opt]))
                             (s/map-of #{:req :opt} any?)))


#_(s/def :dadysql.core/param-spec (s/map-of keyword? clj-spec?))



;;;;;;;;;;;;;;;;;;;; FOr input


(s/def :dadysql.core/exec-total-time int?)
(s/def :dadysql.core/exec-start-time int?)
(s/def :dadysql.core/query-exception string?)


(s/def :dadysql.core/output any?)

(s/def :dadysql.core/param map?)

(s/def :dadysql.core/format-nested any?)
(s/def :dadysql.core/format-nested-array any?)
(s/def :dadysql.core/format-nested-join any?)

(s/def :dadysql.core/format-map any?)
(s/def :dadysql.core/format-array any?)
(s/def :dadysql.core/format-value any?)

(s/def :dadysql.core/output-format #{:dadysql.core/format-nested :dadysql.core/format-nested-array :dadysql.core/format-nested-join
                                     :dadysql.core/format-map :dadysql.core/format-array :dadysql.core/format-value})
(s/def :dadysql.core/param-format #{:dadysql.core/format-nested :dadysql.core/format-map})


(s/def :dadysql.core/op #{:dadysql.core/op-db-seq :dadysql.core/op-pull :dadysql.core/op-push})
;(def input-key )

(s/def :dadysql.core/user-input
  (s/merge
    (s/keys :req [(or :dadysql.core/name :dadysql.core/group)]
            :opt [:dadysql.core/param :dadysql.core/param-format :dadysql.core/output-format])
    (s/map-of #{:dadysql.core/name :dadysql.core/group :dadysql.core/op
                :dadysql.core/param :dadysql.core/param-format :dadysql.core/output-format}
              any?)))
