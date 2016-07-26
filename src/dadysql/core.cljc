(ns dadysql.core
  (:use [dady.common]
        [dady.fail])
  (:require [clojure.spec :as s]
            [clojure.spec :as s]))



(defonce global-key :_global_)
;(defonce module-key :_module_)
(defonce process-context-key :process-context)


;(defonce skip-key :skip)


(def nested-map-format :nested)
(def nested-array-format :nested-array)
(def nested-join-format :nested-join)
(def map-format :map)
(def array-format :array)
(def value-format :value)

(def all-oformat #{nested-join-format nested-map-format nested-array-format
                   map-format array-format value-format})
(def all-pformat #{nested-map-format map-format})



(defonce output-key :output)
(defonce input-key :input)


(s/def ::dml-select (s/spec #(= :select %)))
(s/def ::dml-insert any?)
(s/def ::dml-update any?)
(s/def ::dml-delete any?)
(s/def ::dml-call any?)
(s/def ::dml-type any?)


(s/def ::all (s/spec #(= :all %)))
(s/def ::any (s/spec #(= :any %)))
(s/def ::none (s/spec #(= :none %)))
(s/def ::commit #{:all :any :none})



(s/def ::exec-total-time int?)
(s/def ::exec-start-time int?)
(s/def ::query-exception string?)


(def alais-map {:doc          ::doc
                :timeout      ::timeout
                :reserve-name ::reserve-name
                :file-reload  ::file-reload
                :tx-prop      ::tx-prop



                :join         ::join
                :1-1          ::one-one
                :1-n          ::one-many
                :n-1          ::many-one
                :n-n          ::many-many

                :name         ::name
                :model        ::model
                :group        ::group
                :column       ::column
                :sql          ::sql

                :result       ::result
                :array        ::array
                :single       ::single

                :commit       ::commit
                :all          ::all
                :any          ::any
                :none         ::none

                :dml-type     ::dml-type
                :index        ::index

                :skip         ::skip
                :param        ::param
                :param-spec   ::param-spec
                :ref-con      ::ref-con
                :ref-key      ::ref-key
                :ref-fn-key   ::ref-fn-key
                :ref-gen      ::ref-gen


                :extend       ::extend
                :spec-file    ::spec-file
                })



(defn resolve? [v]
  (if (resolve v) true false))


(s/def ::tx-prop (s/cat :ck #{:isolation}
                        :cv (s/spec #{:none :read-committed :read-uncommitted :repeatable-read :serializable})
                        :rk #{:read-only?}
                        :rv (s/spec boolean?)))


(s/def ::file-reload boolean?)
(s/def ::reserve-name (s/with-gen (s/every keyword? :kind set?)
                                  (fn []
                                    (s/gen #{#{:create-ddl :drop-ddl :init-data}
                                             #{:init-data}}))))

(s/def ::doc string?)
(s/def ::timeout pos-int?)

(s/def ::name
  (s/with-gen (s/or :one keyword? :many (s/coll-of keyword? :kind vector? :distinct true))
              (fn [] (s/gen #{:get-dept-list :get-dept-by-ids :get-employee-list :get-meeting-list :get-employee-meeting-list}))))

(s/def ::index int?)


(s/def ::sql
  (s/with-gen (s/every string? :kind vector?)
              (fn [] (s/gen #{"select * from department LIMIT :limit OFFSET :offset;\nselect * from department where id in (:id) ;\nselect * from employee LIMIT :limit OFFSET :offset;\nselect * from meeting LIMIT :limit OFFSET :offset;\nselect * from employee_meeting LIMIT :limit OFFSET :offset;\n"
                              "select * from employee where id = :id;\nselect d.* from department d, employee e where e.id=:id and d.id = e.dept_id;\nselect ed.* from employee_detail ed where ed.employee_id=:id;\nselect m.*, em.employee_id from meeting m, employee_meeting em where em.employee_id=:id and em.meeting_id = m.meeting_id;\n"
                              "select * from meeting where  meeting_id = :id;\nselect e.*, em.employee_id from employee e, employee_meeting em where em.meeting_id = :id and em.employee_id = e.id;\n"
                              "insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name);\nupdate department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id;\ndelete from department where id in (:id);\n"}))))


(s/def ::model
  (s/with-gen (s/or :one keyword? :many (s/coll-of keyword? :kind vector?))
              (fn [] (s/gen #{:dept :employee :meeting}))))

(s/def ::skip
  (s/with-gen (s/every keyword? :kind set?)
              (fn [] (s/gen #{#{:join :column}
                              #{:join}}))))

(s/def ::group
  (s/with-gen keyword?
              #(s/gen #{:load-dept :load-employee})))



(s/def ::column
  (s/with-gen (s/every-kv keyword? keyword?)
              (fn [] (s/gen #{{:id :empl_id}
                              {:dept_id :id}}))))

;(defonce result-array-key :array)
;(defonce result-single-key :single)

(s/def ::array (s/spec #(= :array %)))
(s/def ::single (s/spec #(= :single %)))
(s/def ::result (s/every #{:array :single} :kind set?))


(s/def ::read-only? boolean?)


(s/def ::one-one (s/spec #(= :1-1 %)))
(s/def ::one-many (s/spec #(= :1-n %)))
(s/def ::many-one (s/spec #(= :n-1 %)))
(s/def ::many-many (s/spec #(= :n-n %)))

(s/def ::join-one
  (s/tuple keyword? keyword? (s/spec #{:1-1 :1-n :n-1}) keyword? keyword?))

(s/def ::join-many
  (s/tuple keyword? keyword? (s/spec #{:n-n}) keyword? keyword? (s/tuple keyword? keyword? keyword?)))

(s/def ::join
  (s/with-gen
    (clojure.spec/*
      (clojure.spec/alt
        :join-one ::join-one
        :join-many ::join-many))
    (fn []
      (s/gen
        #{[[:department :id ::one-many :employee :dept_id]
           [:employee :id ::one-one :employee-detail :employee_id]
           [:employee :id :many-many :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]
          }))))


(s/def ::ref-con (clojure.spec/tuple keyword? (s/spec #(= :ref-con %)) any?))
(s/def ::ref-key (clojure.spec/tuple keyword? (s/spec #(= :ref-key %)) keyword?))
(s/def ::ref-fn-key (clojure.spec/tuple keyword? (s/spec #(= :ref-fn-key %)) resolve? keyword?))
(s/def ::ref-gen (clojure.spec/tuple keyword? (s/spec #(= :ref-gen %)) keyword?))


(s/def ::param
  (clojure.spec/*
    (clojure.spec/alt
      :ref-con ::ref-con
      :ref-fn-key ::ref-fn-key
      :ref-gen ::ref-gen
      :ref-key ::ref-key)))



(defn ns-keyword? [v]
  (if (namespace v) true false))

(s/def ::param-spec (s/and keyword? ns-keyword?))

(s/def ::common (s/keys :opt-un [::timeout ::column ::result ::param ::param-spec]))


(s/def ::extend
  (s/every-kv keyword? (s/merge (s/keys :opt-un [::model]) ::common)))


(s/def ::module (s/merge
                  ::common
                  (s/keys :req-un [::name ::sql]
                          :opt-un [::model ::skip ::group ::commit ::extend])))


(s/def ::spec-file symbol?)

(s/def ::global (s/keys :req-un [::name]
                        :opt-un [::timeout ::read-only? ::tx-prop ::file-reload ::reserve-name ::join ::spec-file]))


(s/def ::compiler-input-spec (clojure.spec/cat :global (s/? ::global) :module (s/* ::module)))







(defn validate-input!
  [{:keys [gname name params oformat pformat] :as request-m}]
  (cond
    (and (nil? name)
         (nil? gname))
    (fail "Need value either name or gname")
    (and (not (nil? gname))
         (not (keyword? gname)))
    (fail "gname will be keyword")
    (and (not (nil? name))
         (not (sequential? name))
         (not (keyword? name)))
    (fail "name will be keyword")
    (and
      (sequential? name)
      (not (every? keyword? name)))
    (fail "name will be sequence of keyword")
    (and                                                    ;(= map-format out-format)
      (sequential? name)
      (contains? #{map-format array-format value-format} oformat))
    (fail #?(:clj  (format "only one name keyword is allowed for %s format " oformat)
             :cljs "Only one name keyword is allowed"))
    (and
      (not (nil? params))
      (not (map? params)))
    (fail "params will be map format ")
    (and
      (not (nil? pformat))
      (not (contains? all-pformat pformat)))
    (fail #?(:clj  (format "pformat is not correct, it will be %s ", (str all-pformat))
             :cljs "pformat is not correct"))
    (and
      (not (nil? oformat))
      (not (contains? all-oformat oformat)))
    (fail #?(:clj  (format "oformat is not correct, it will be %s" (str all-oformat))
             :cljs "oformat is not correct"))
    :else
    request-m))
