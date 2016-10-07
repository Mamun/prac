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


(s/def :dadysql.core/all (s/spec #(= :all %)))
(s/def :dadysql.core/any (s/spec #(= :any %)))
(s/def :dadysql.core/none (s/spec #(= :none %)))
(s/def :dadysql.core/commit #{:all :any :none})



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

(s/def :dadysql.core/name
  (s/with-gen (s/or :one keyword? :many (s/coll-of keyword? :kind vector? :distinct true))
              (fn [] (s/gen #{:get-dept-list :get-dept-by-ids :get-employee-list :get-meeting-list :get-employee-meeting-list}))))

(s/def :dadysql.core/index int?)


(s/def :dadysql.core/sql
  (s/with-gen (s/every string? :kind vector?)
              (fn [] (s/gen #{"select * from department LIMIT :limit OFFSET :offset;\nselect * from department where id in (:id) ;\nselect * from employee LIMIT :limit OFFSET :offset;\nselect * from meeting LIMIT :limit OFFSET :offset;\nselect * from employee_meeting LIMIT :limit OFFSET :offset;\n"
                              "select * from employee where id = :id;\nselect d.* from department d, employee e where e.id=:id and d.id = e.dept_id;\nselect ed.* from employee_detail ed where ed.employee_id=:id;\nselect m.*, em.employee_id from meeting m, employee_meeting em where em.employee_id=:id and em.meeting_id = m.meeting_id;\n"
                              "select * from meeting where  meeting_id = :id;\nselect e.*, em.employee_id from employee e, employee_meeting em where em.meeting_id = :id and em.employee_id = e.id;\n"
                              "insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name);\nupdate department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id;\ndelete from department where id in (:id);\n"}))))


(s/def :dadysql.core/model
  (s/with-gen (s/or :one keyword? :many (s/coll-of keyword? :kind vector?))
              (fn [] (s/gen #{:dept :employee :meeting}))))

(s/def :dadysql.core/skip
  (s/with-gen (s/every keyword? :kind set?)
              (fn [] (s/gen #{#{:join :column}
                              #{:join}}))))

(s/def :dadysql.core/group
  (s/with-gen keyword?
              #(s/gen #{:load-dept :load-employee})))



(s/def :dadysql.core/column
  (s/with-gen (s/every-kv keyword? keyword?)
              (fn [] (s/gen #{{:id :empl_id}
                              {:dept_id :id}}))))

(s/def :dadysql.core/result (s/every #{:dadysql.core/array :dadysql.core/single} :kind set?))


(s/def :dadysql.core/read-only? boolean?)


(def one-* #{:dadysql.core/one-one
             :dadysql.core/one-many
             :dadysql.core/many-one})

(s/def :dadysql.core/join-one
  (s/tuple keyword? keyword? (s/spec one-*) keyword? keyword?))

(s/def :dadysql.core/join-many
  (s/tuple keyword? keyword? (s/spec #{:dadysql.core/many-many}) keyword? keyword? (s/tuple keyword? keyword? keyword?)))

(s/def :dadysql.core/join
  (s/with-gen
    (clojure.spec/*
      (clojure.spec/alt
        :join-one :dadysql.core/join-one
        :join-many :dadysql.core/join-many))
    (fn []
      (s/gen
        #{[[:department :id :dadysql.core/one-many :employee :dept_id]
           [:employee :id :dadysql.core/one-one :employee-detail :employee_id]
           [:employee :id :many-many :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]
          }))))


(s/def :dadysql.core/ref-con (clojure.spec/tuple keyword? (s/spec #(= :ref-con %)) any?))
(s/def :dadysql.core/ref-key (clojure.spec/tuple keyword? (s/spec #(= :ref-key %)) keyword?))
(s/def :dadysql.core/ref-fn-key (clojure.spec/tuple keyword? (s/spec #(= :ref-fn-key %)) resolve? keyword?))
(s/def :dadysql.core/ref-gen (clojure.spec/tuple keyword? (s/spec #(= :ref-gen %)) keyword?))


(s/def :dadysql.core/param
  (clojure.spec/*
    (clojure.spec/alt
      :ref-con :dadysql.core/ref-con
      :ref-fn-key :dadysql.core/ref-fn-key
      :ref-gen :dadysql.core/ref-gen
      :ref-key :dadysql.core/ref-key)))



(defn ns-keyword? [v]
  (if (namespace v) true false))

(s/def :dadysql.core/param-spec (s/and keyword? ns-keyword?))

(s/def :dadysql.core/common (s/keys :opt [:dadysql.core/timeout :dadysql.core/column :dadysql.core/result :dadysql.core/param :dadysql.core/param-spec]))


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
                :1-1          :dadysql.core/one-one
                :1-n          :dadysql.core/one-many
                :n-1          :dadysql.core/many-one
                :n-n          :dadysql.core/many-many

                :name         :dadysql.core/name
                :model        :dadysql.core/model
                :group        :dadysql.core/group
                :column       :dadysql.core/column
                :sql          :dadysql.core/sql

                :result       :dadysql.core/result
                :array        :dadysql.core/array
                :single       :dadysql.core/single

                :commit       :dadysql.core/commit
                :all          :dadysql.core/all
                :any          :dadysql.core/any
                :none         :dadysql.core/none

                :dml-type     :dadysql.core/dml-type
                :index        :dadysql.core/index

                :skip         :dadysql.core/skip
                :param        :dadysql.core/param
                :param-spec   :dadysql.core/param-spec
                :ref-con      :dadysql.core/ref-con
                :ref-key      :dadysql.core/ref-key
                :ref-fn-key   :dadysql.core/ref-fn-key
                :ref-gen      :dadysql.core/ref-gen


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

