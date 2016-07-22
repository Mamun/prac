(ns dadysql.core
  (:use [dady.common]
        [dady.fail])
  (:require [clojure.spec :as s]
            [clojure.walk :as w]))


(defonce global-key :_global_)
(defonce module-key :_module_)
(defonce process-context-key :process-context)
;(defonce reserve-name-key :reserve-name)
;(defonce file-name-key :file-name)
;(defonce file-reload-key :file-reload)
(defonce ds-key :datasource)
(defonce ds-conn-key :datasource-conn)
;(defonce tx-prop :tx-prop)


;(defonce name-key :name)
;(defonce column-key :column)
(defonce doc-key :doc)
;(defonce model-key :model)
(defonce skip-key :skip)
;(defonce timeout-key :timeout)
;(defonce group-key :group)
;(defonce index :index)
;(defonce sql-key :sql)


;(def root-meta :meta)
(defonce extend-meta-key :extend)

;(def meta-with-extend #{root-meta extend-meta-key})

(def nested-map-format :nested)
(def nested-array-format :nested-array)
(def nested-join-format :nested-join)
(def map-format :map)
(def array-format :array)
(def value-format :value)


(defonce output-key :output)
(defonce input-key :input)


;(defonce result-key :result)
(defonce result-array-key :array)
(defonce result-single-key :single)

;(def in-type :input-type)
;(def out-type :output-type)


;(defonce param-key :param)
(defonce param-ref-con-key :ref-con)
(defonce param-ref-key :ref-key)
(defonce param-ref-fn-key :ref-fn-key)
(defonce param-ref-gen-key :ref-gen)


;(defonce param-spec-key :param-spec)
#_(defonce validation-type-key :type)
#_(defonce validation-range-key :range)
#_(defonce validation-contain-key :contain)


;(defonce join-key :join)
(defonce join-1-1-key :1-1)
(defonce join-1-n-key :1-n)
(defonce join-n-1-key :n-1)
(defonce join-n-n-key :n-n)


;(defonce dml-key :dml-type)
(defonce dml-select-key :select)
(defonce dml-insert-key :insert)
(defonce dml-update-key :update)
(defonce dml-delete-key :delete)
(defonce dml-call-key :call)



;(defonce commit-key :commit)
(defonce commit-all-key :all)
(defonce commit-any-key :any)
(defonce commit-none-key :none)


;(defonce error-key :error)
(defonce exec-time-total-key :exec-total-time)
(defonce exec-time-start-key :exec-start-time)
(defonce query-exception-key :query-exception)


(def all-oformat #{nested-join-format nested-map-format nested-array-format
                   map-format array-format value-format})
(def all-pformat #{nested-map-format map-format})


(def namespace-key {:name ::name
                    :group ::group
                    :model ::model
                    :reserve-name ::reserve-name
                    :file-reload ::file-reload
                    :tx-prop ::tx-prop
                    :join ::join
                    :timeout ::timeout
                    :skip ::skip
                    :param ::param
                    :param-spec ::param-spec
                    :result ::result
                    :column ::column
                    :sql ::sql
                    :commit ::commit
                    :dml-type ::dml-type
                    :index ::index

                    :extend ::extend
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
              (fn [] (s/gen #{global-key :get-dept-list :get-dept-by-ids :get-employee-list :get-meeting-list :get-employee-meeting-list}))))

(s/def ::index int?)


(s/def ::sql
  (s/with-gen (s/every string? :kind vector?)
              (fn [] (s/gen #{"select * from department LIMIT :limit OFFSET :offset;\nselect * from department where id in (:id) ;\nselect * from employee LIMIT :limit OFFSET :offset;\nselect * from meeting LIMIT :limit OFFSET :offset;\nselect * from employee_meeting LIMIT :limit OFFSET :offset;\n"
                              "select * from employee where id = :id;\nselect d.* from department d, employee e where e.id=:id and d.id = e.dept_id;\nselect ed.* from employee_detail ed where ed.employee_id=:id;\nselect m.*, em.employee_id from meeting m, employee_meeting em where em.employee_id=:id and em.meeting_id = m.meeting_id;\n"
                              "select * from meeting where  meeting_id = :id;\nselect e.*, em.employee_id from employee e, employee_meeting em where em.meeting_id = :id and em.employee_id = e.id;\n"
                              "insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name);\nupdate department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id;\ndelete from department where id in (:id);\n"}))))

(s/def ::dml-type any?)


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

(s/def ::commit #{:all :any :none})

(s/def ::column
  (s/with-gen (s/every-kv keyword? keyword?)
              (fn [] (s/gen #{{:id :empl_id}
                              {:dept_id :id}}))))


(s/def ::result (s/every #{result-array-key result-single-key} :kind set?))
(s/def ::read-only? boolean?)

(s/def ::join-one
  (s/tuple keyword? keyword? (s/spec #{join-1-1-key join-1-n-key join-n-1-key}) keyword? keyword?))

(s/def ::join-many
  (s/tuple keyword? keyword? (s/spec #{join-n-n-key}) keyword? keyword? (s/tuple keyword? keyword? keyword?)))

(s/def ::join
  (s/with-gen
    (clojure.spec/*
      (clojure.spec/alt
        :join-one ::join-one
        :join-many ::join-many))
    (fn []
      (s/gen
        #{[[:department :id join-1-n-key :employee :dept_id]
           [:employee :id join-1-1-key :employee-detail :employee_id]
           [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]
          }))
    ))


(s/def ::param-ref-con
  (s/with-gen (clojure.spec/tuple keyword? #(= param-ref-con-key %) any?)
              (fn []
                (s/gen #{[:id param-ref-con-key 23]
                         [:name param-ref-con-key "Hello"]}))))
(s/def ::param-ref
  (s/with-gen (clojure.spec/tuple keyword? #(= param-ref-key %) keyword?)
              (fn []
                (s/gen #{[:id param-ref-key :rid]
                         [:name param-ref-key :rname]}))))
(s/def ::param-ref-fn
  (s/with-gen (clojure.spec/tuple keyword? #(= param-ref-fn-key %) resolve? keyword?)
              (fn []
                (s/gen #{[:id param-ref-fn-key 'inc :rid]
                         [:name param-ref-fn-key 'inc :rname]}))))

(s/def ::param-ref-gen
  (s/with-gen (clojure.spec/tuple keyword? #(= param-ref-gen-key %) keyword?)
              (fn []
                (s/gen #{[:id param-ref-gen-key :gen-id]
                         [:name param-ref-gen-key :gen-name]}))))


(s/def ::param
  (clojure.spec/*
    (clojure.spec/alt
      :ref-con ::param-ref-con
      :ref-fn-key ::param-ref-fn
      :ref-gen ::param-ref-gen
      :ref-key ::param-ref)))



(defn ns-keyword? [v]
  (if (namespace v) true false ))


#_(defn resolve? [v]
    )

#_(comment
    (ns-keyword? :av)
    (ns-keyword? :acom/v)

    ;(s/regex? s/*)
    (s/spec? int?)

    (s/regex? int?)

    integer?

    number?

    )


(s/def ::param-spec (s/and keyword? ns-keyword?))



(s/def ::common (s/keys :opt-un [::timeout ::column ::result ::param ::param-spec]))


(s/def ::extend
  (s/every-kv keyword? (s/merge (s/keys :opt-un [::model]) ::common)))


(s/def ::module (s/merge
                  ::common
                  (s/keys :req-un [::name ::sql]
                          :opt-un [::model ::skip ::group ::commit ::extend])))


(s/def ::global (s/keys :req-un [::name]
                        :opt-un [::timeout ::read-only? ::tx-prop ::file-reload ::reserve-name ::join]))


(s/def ::compiler-input-spec (clojure.spec/cat :global (s/? ::global) :module (s/* ::module)))





(comment

  (namespace :b)


  #_(->
      {::name "Hello"}
      (::name))


  ;{:name ::name}


  (clojure.walk)

  (clojure.set/rename-keys {:fname "Musterman"} {:fname :person/fname})


  ;;backend core part come as lib
  (s/def ::person-spec (s/keys ::req-un [::id ::fname ::lname]))

  ;;web side
  (s/def ::person-ui-spec (s/merge (s/keys ::req-un [::channel])
                                   ::person-spec
                                   ))




  (s/merge (s/keys ::req-un [:dob]))




  (s/form (s/spec boolean?))

  ;(s/form one? )

  ;(s/form (s/spec int?))

  (s/form ::extend)

  (s/form ::module)

  (require :reload 'clojure.spec.gen)
  (in-ns 'clojure.spec.gen)

  ;; combinators, see call to lazy-combinators above for complete list

  (generate (gen ::param-ref))

  )




(defn replace-mk
  [m rm ]
  (let [f (fn [[k v]]
            (do
              (println k v)
              [(or (k rm) k) v]))]
    (into {} (map f m))))





(comment

  #_(get
    (clojure.set/rename-keys {:name 1} {:name ::name})
    ::name)

  (replace-mk {:name 1} {:name ::name1})

  #_(::name
    (assoc {} ::name 1))

  (into {} [[:name 2]])

  (into {} [[::name "asdf"]  ])


  ;::name
  ;(:name {:name ::name})

  (postwalk-rename-keys {:name 1} {:name ::name} )
  )









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
