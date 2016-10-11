(ns dadysql.compiler.spec
  (:require [clojure.spec :as s]))


(s/def :dadysql.core/dml #{:dadysql.core/dml-select
                           :dadysql.core/dml-insert
                           :dadysql.core/dml-update
                           :dadysql.core/dml-delete
                           :dadysql.core/dml-call})


(s/def :dadysql.core/commit #{:dadysql.core/commit-all
                              :dadysql.core/commit-none
                              :dadysql.core/commit-any})



(defn resolve? [v]
  (if (resolve v) true false))


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
      :one  (s/tuple keyword? keyword? #{:dadysql.core/join-one-one :dadysql.core/join-one-many :dadysql.core/join-many-one} keyword? keyword?)
      :many (s/tuple keyword? keyword? #{:dadysql.core/join-many-many} keyword? keyword? (s/tuple keyword? keyword? keyword?)))))



(s/def :dadysql.core/param-coll
  (clojure.spec/*
    (clojure.spec/alt
      :ref-con (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-con} any?)
      :ref-fn-key (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-fn-key} resolve? keyword?)
      :ref-gen (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-gen} keyword?)
      :ref-key (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-key} keyword?))))



(defn ns-keyword? [v]
  (if (namespace v) true false))

(s/def :dadysql.core/param-spec #_(s/map-of keyword? resolve? ) (s/and keyword? ns-keyword?))

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
        (throw (ex-info "Compile failed " (s/explain-data :dadysql.core/compiler-spec coll)))))))





(comment

  (s/valid? :dadysql.core/param {:name   [:get-employee-detail]
                                 :params {:id 1}})


  (s/explain :dadysql.core/param {:name                      [:get-employee-detail]
                                  :group                     :load-dept
                                  :dadysql.core/param-format :map
                                  :params                    {}})

  )

