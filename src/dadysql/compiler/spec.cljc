(ns dadysql.compiler.spec
  (:require [clojure.spec :as s]))

;(defonce global-key :_global_)

;(defonce process-context-key :process-context)


(s/def :dadysql.core/dml-select (s/spec #(= :select %)))
(s/def :dadysql.core/dml-insert any?)
(s/def :dadysql.core/dml-update any?)
(s/def :dadysql.core/dml-delete any?)
(s/def :dadysql.core/dml-call any?)
(s/def :dadysql.core/dml-type any?)


(s/def :dadysql.core/commit #{:dadysql.core/commit-all
                              :dadysql.core/commit-none
                              :dadysql.core/commit-any})



(s/def :dadysql.core/exec-total-time int?)
(s/def :dadysql.core/exec-start-time int?)
(s/def :dadysql.core/query-exception string?)



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

(s/def :dadysql.core/name (s/or :one keyword? :many (s/coll-of keyword? :kind vector? :distinct true)))

(s/def :dadysql.core/index int?)


(s/def :dadysql.core/sql (s/every string? :kind vector?))

(s/def :dadysql.core/model (s/or :one keyword? :many (s/coll-of keyword? :kind vector?)))

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
      :ref-fn-key (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-fn-key} resolve? keyword?)
      :ref-gen (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-gen} keyword?)
      :ref-key (clojure.spec/tuple keyword? #{:dadysql.core/param-ref-key} keyword?))))



(defn ns-keyword? [v]
  (if (namespace v) true false))

(s/def :dadysql.core/param-spec (s/and keyword? ns-keyword?))

(s/def :dadysql.core/common (s/keys :opt [:dadysql.core/timeout :dadysql.core/column :dadysql.core/result :dadysql.core/param-coll :dadysql.core/param-spec]))


(s/def :dadysql.core/extend
  (s/every-kv keyword? (s/merge (s/keys :opt [:dadysql.core/model]) :dadysql.core/common)))


(s/def :dadysql.core/module (s/merge
                              :dadysql.core/common
                              (s/keys :req [:dadysql.core/name :dadysql.core/sql]
                                      :opt [:dadysql.core/model :dadysql.core/skip :dadysql.core/group :dadysql.core/commit :dadysql.core/extend])))


(s/def :dadysql.core/spec-file symbol?)

(s/def :dadysql.core/global (s/keys :req [:dadysql.core/name]
                                    :opt [:dadysql.core/timeout :dadysql.core/read-only? :dadysql.core/tx-prop :dadysql.core/file-reload :dadysql.core/reserve-name :dadysql.core/join :dadysql.core/spec-file]))


(s/def :dadysql.core/compiler-input-spec (clojure.spec/cat :global (s/? :dadysql.core/global) :module (s/* :dadysql.core/module)))





(def alais-map {:doc          :dadysql.core/doc
                :timeout      :dadysql.core/timeout
                :reserve-name :dadysql.core/reserve-name
                :file-reload  :dadysql.core/file-reload
                :tx-prop      :dadysql.core/tx-prop



                :join         :dadysql.core/join
                :1-1          :dadysql.core/join-one-one
                :1-n          :dadysql.core/join-one-many
                :n-1          :dadysql.core/join-many-one
                :n-n          :dadysql.core/join-many-many

                :name         :dadysql.core/name
                :model        :dadysql.core/model
                :group        :dadysql.core/group
                :column       :dadysql.core/column
                :sql          :dadysql.core/sql

                :result       :dadysql.core/result
                :array        :dadysql.core/result-array
                :single       :dadysql.core/result-single

                :commit       :dadysql.core/commit
                :all          :dadysql.core/commit-all
                :any          :dadysql.core/commit-any
                :none         :dadysql.core/commit-none

                :dml-type     :dadysql.core/dml-type
                :index        :dadysql.core/index

                :skip         :dadysql.core/skip
                :param        :dadysql.core/param-coll
                :ref-con      :dadysql.core/param-ref-con
                :ref-key      :dadysql.core/param-ref-key
                :ref-fn-key   :dadysql.core/param-ref-fn-key
                :ref-gen      :dadysql.core/param-ref-gen
                :param-spec   :dadysql.core/param-spec

                :extend       :dadysql.core/extend
                :spec-file    :dadysql.core/spec-file
                })





(comment

  (s/valid? :dadysql.core/input {:name   [:get-employee-detail]
                                 :params {:id 1}})


  (s/explain :dadysql.core/input {:name                      [:get-employee-detail]
                                  :group                     :load-dept
                                  :dadysql.core/input-format :map
                                  :params                    {}})

  )

