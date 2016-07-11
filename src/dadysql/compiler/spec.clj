(ns dadysql.compiler.spec
  (:use [dadysql.constant])
  (:require [clojure.spec :as s]))


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


(comment

  (s/conform ::name :a )


  (s/explain ::name  [:a :b :b] )

  ;(s/conform ::name (list :a :b ))

  )



(s/def ::sql
  (s/with-gen (s/every string? :kind vector? )
              (fn [] (s/gen #{"select * from department LIMIT :limit OFFSET :offset;\nselect * from department where id in (:id) ;\nselect * from employee LIMIT :limit OFFSET :offset;\nselect * from meeting LIMIT :limit OFFSET :offset;\nselect * from employee_meeting LIMIT :limit OFFSET :offset;\n"
                              "select * from employee where id = :id;\nselect d.* from department d, employee e where e.id=:id and d.id = e.dept_id;\nselect ed.* from employee_detail ed where ed.employee_id=:id;\nselect m.*, em.employee_id from meeting m, employee_meeting em where em.employee_id=:id and em.meeting_id = m.meeting_id;\n"
                              "select * from meeting where  meeting_id = :id;\nselect e.*, em.employee_id from employee e, employee_meeting em where em.meeting_id = :id and em.employee_id = e.id;\n"
                              "insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name);\nupdate department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id;\ndelete from department where id in (:id);\n"}))))

(s/def ::model
  (s/with-gen (s/or :one keyword? :many (s/coll-of keyword? :kind vector? ))
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
  (s/with-gen (s/tuple keyword? keyword? (s/spec #{join-1-1-key join-1-n-key join-n-1-key}) keyword? keyword?)
              (fn []
                (s/gen #{[:department :id join-1-n-key :employee :dept_id]
                         [:employee :id join-1-1-key :employee-detail :employee_id]}))))

(s/def ::join-many
  (s/with-gen (s/tuple keyword? keyword? (s/spec #{join-n-n-key}) keyword? keyword? (s/tuple keyword? keyword? keyword?))
              (fn []
                (s/gen #{[:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]}))))

(s/def ::join
  (clojure.spec/*
    (clojure.spec/alt
      :join-one ::join-one
      :join-many ::join-many)))


(s/def ::param-ref-con
  (s/with-gen (clojure.spec/tuple keyword? #(= param-ref-con-key %) :clojure.spec/any)
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


(s/def ::params
  (clojure.spec/*
    (clojure.spec/alt
      :ref-con ::param-ref-con
      :ref-fn-key ::param-ref-fn
      :ref-gen ::param-ref-gen
      :ref-key ::param-ref)))



(s/def ::vali-type
  (s/with-gen (clojure.spec/tuple keyword? #(= validation-type-key %) resolve? string?)
              (fn []
                (s/gen #{[:id validation-type-key 'vector? "id witll be vector"]}))))
(s/def ::vali-type-contain
  (s/with-gen (clojure.spec/tuple keyword? #(= validation-contain-key %) resolve? string?)
              (fn []
                (s/gen #{[:id validation-contain-key 'int? "id witll be integer"]}))))
(s/def ::vali-range
  (s/with-gen (clojure.spec/tuple keyword? #(= validation-range-key %) integer? integer? string?)
              (fn []
                (s/gen #{[:id validation-range-key 10 11 "id witll be between 10 and 11"]}))))



(s/def ::validation
  (clojure.spec/*
    (clojure.spec/alt
      :type ::vali-type
      :contain ::vali-type-contain
      :range ::vali-range)))



(s/def ::extend
  (s/with-gen
    (s/every-kv keyword? (s/keys :opt-un [::timeout ::column ::result ::params ::validation]))
    (fn []
      (s/gen #{{:get-meeting-by-id {:model :meeting
                                    :result #{:single}
                                    :validation [[:id :type 'int? "Id will be sequence"]
                                                 [:id :range 10 11 "Id range will be between 10 and 11"]]}}
               {:get-employee-for-meeting {:model :employee-meeting}}}))))




(comment



  #_(s/explain ::extendv {:a {:timeout 456
                            :column {:a :b}}} )





  )



#_(s/def ::extend (s/* (s/cat
                       :name keyword?
                       :prop (s/keys :opt-un [::timeout ::column ::result ::params ::validation]))))

(s/def ::module (s/keys :req-un [::name ::sql]
                        :opt-un [::timeout ::model ::skip ::group ::commit ::column ::result ::params ::validation ::extend]))


(s/def ::global (s/keys :req-un [::name]
                        :opt-un [::timeout ::read-only? ::tx-prop ::file-reload ::reserve-name ::join]))


(s/def ::compiler-spec (clojure.spec/cat :global (s/? ::global) :module (s/* ::module)))






#_(defn conform-spec [w]
  (s/conform ::compiler-spec w))




#_(s/def ::comp (clojure.spec/tuple keyword? number?))
#_(s/def ::level (clojure.spec/* (clojure.spec/alt :l ::comp)))

#_(s/def ::extend (s/keys :opt-un [::level]))
#_(s/def ::model  (s/keys :opt-un [::level ::extend]))


#_(s/conform ::model {:level  [[:hello 1]]
                    :extend {:a {:level [[:hello ""]]}}})














