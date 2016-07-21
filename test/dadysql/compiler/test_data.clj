(ns dadysql.compiler.test-data
  (:require [clojure.spec :as s]))


(def compile-one-data
  {:doc        "Modify department"
   :name       [:insert-dept :update-dept :delete-dept]
   :model      :department
   :param-spec :tie-edn2/get-dept-by-id
   :sql        ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"
                "update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id"
                "delete from department where id in (:id)"]
   :extend     {:insert-dept {:param  [[:transaction_id :ref-con 0]
                                        [:transaction_id :ref-con 0]]
                              :timeout 30}
                :update-dept {:param [[:next_transaction_id :ref-fn-key 'inc :transaction_id]]}
                :delete-dept {:param-spec :tie-edn2/get-dept-by-id}}}
  )


(def compile-one-expected-result
  [{:timeout    30,
    :param-spec :tie-edn2/get-dept-by-id,
    :param     [[:transaction_id :ref-con 0]],
    :index      0,
    :name       :insert-dept,
    :sql
                ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"
                 :id
                 :transaction_id
                 :dept_name],
    :model      :department,
    :dml-type   :insert}
   {:timeout    1000,
    :param-spec :tie-edn2/get-dept-by-id,
    :param
                [[:next_transaction_id
                  :ref-fn-key
                  #'clojure.core/inc
                  :transaction_id]],
    :index      1,
    :name       :update-dept,
    :sql
                ["update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id"
                 :dept_name
                 :next_transaction_id
                 :transaction_id
                 :id],
    :model      :department,
    :dml-type   :update}
   {:timeout  1000,
    :param-spec :tie-edn2/get-dept-by-id,
    :index    2,
    :name     :delete-dept,
    :sql      ["delete from department where id in (:id)" :id],
    :model    :department,
    :dml-type :delete}]

  )


;(s/spec? integer?)



(def do-compile-input-data
  [{:name         :_global_
    :doc          "global."
    :file-reload  true
    :timeout      1000
    :reserve-name #{:create-ddl :drop-ddl :init-data}
    :tx-prop      [:isolation :serializable :read-only? true]
    :join         [[:department :id :1-n :employee :dept_id]
                   [:employee :id :1-1 :employee-detail :employee_id]
                   [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]}
   {:doc     "spec"
    :name    [:get-dept-list :get-dept-by-ids :get-employee-list :get-meeting-list :get-employee-meeting-list]
    :model   [:department :department :employee :meeting :employee-meeting]
    :extend  {:get-dept-by-ids {:param-spec :tie-edn2/get-dept-by-id
                                :result     #{:array}}
              :get-dept-list   {:result #{:array}}}
    :timeout 5000
    :result  #{:array}
    :param  [[:limit :ref-con 10]
              [:offset :ref-con 0]]
    :sql     ["select * from department LIMIT :limit OFFSET :offset"
              "select * from department where id in (:id) "
              "select * from employee LIMIT :limit OFFSET :offset"
              "select * from meeting LIMIT :limit OFFSET :offset"
              "select * from employee_meeting LIMIT :limit OFFSET :offset"]}])



(def do-compile-expected-result
  {:_global_
   {:name         :_global_,
    :doc          "global.",
    :file-reload  true,
    :timeout      1000,
    :reserve-name #{:create-ddl :init-data :drop-ddl},
    :tx-prop      [:isolation :serializable :read-only? true]},
   :get-dept-list
   {:timeout  5000,
    :result   #{:array},
    :param   [[:limit :ref-con 10] [:offset :ref-con 0]],
    :join     [[:department :id :1-n :employee :dept_id]],
    :name     :get-dept-list,
    :index    0,
    :sql
              ["select * from department limit :limit offset :offset"
               :limit
               :offset],
    :model    :department,
    :dml-type :select},
   :get-dept-by-ids
   {:index    1,
    :name     :get-dept-by-ids,
    :param   [[:limit :ref-con 10] [:offset :ref-con 0]],
    :sql      ["select * from department where id in (:id) " :id],
    :result   #{:array},
    :timeout  5000,
    :param-spec :tie-edn2/get-dept-by-id,
    :dml-type :select,
    :join     [[:department :id :1-n :employee :dept_id]],
    :model    :department},
   :get-employee-list
   {:timeout  5000,
    :result   #{:array},
    :param   [[:limit :ref-con 10] [:offset :ref-con 0]],
    :join     [[:employee :id :1-1 :employee-detail :employee_id]
               [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]
               [:employee :dept_id :n-1 :department :id]],
    :name     :get-employee-list,
    :index    2,
    :sql
              ["select * from employee limit :limit offset :offset"
               :limit
               :offset],
    :model    :employee,
    :dml-type :select},
   :get-meeting-list
   {:timeout  5000,
    :result   #{:array},
    :param   [[:limit :ref-con 10] [:offset :ref-con 0]],
    :join     [[:meeting :meeting_id :n-n :employee :id [:employee-meeting :meeting_id :employee_id]]],
    :name     :get-meeting-list,
    :index    3,
    :sql
              ["select * from meeting limit :limit offset :offset" :limit :offset],
    :model    :meeting,
    :dml-type :select},
   :get-employee-meeting-list
   {:timeout  5000,
    :result   #{:array},
    :param   [[:limit :ref-con 10] [:offset :ref-con 0]],
    :name     :get-employee-meeting-list,
    :index    4,
    :sql
              ["select * from employee_meeting limit :limit offset :offset"
               :limit
               :offset],
    :model    :employee-meeting,
    :dml-type :select}})




(def do-compile-input-data2
  [{:name         :_global_
    :doc          "global."
    :file-reload  true
    :timeout      1000
    :reserve-name #{:create-ddl :drop-ddl :init-data}
    :tx-prop      [:isolation :serializable :read-only? true]
    :join         [[:department :id :1-n :employee :dept_id]
                   [:employee :id :1-1 :employee-detail :employee_id]
                   [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]}
   {:doc     "spec"
    :name    [:get-dept-list :get-dept-by-ids :get-employee-list :get-meeting-list :get-employee-meeting-list]
    :model   [:department :department :employee :meeting :employee-meeting]
    :extend  {:get-dept-by-ids {:param-spec :tie-edn/get-dept-by-id
                                :result     #{:array}}
              :get-dept-list   {:result #{:array}}}
    :timeout 5000
    :result  #{:array}
    :param-spec :tie-edn/get-dept-by-id2
    :param  [[:limit :ref-con 10]
              [:offset :ref-con 0]]
    :sql     ["select * from department LIMIT :limit OFFSET :offset"
              "select * from department where id in (:id) "
              "select * from employee LIMIT :limit OFFSET :offset"
              "select * from meeting LIMIT :limit OFFSET :offset"
              "select * from employee_meeting LIMIT :limit OFFSET :offset"]}])

