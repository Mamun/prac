(ns dadysql.compiler.test-data)



(def compile-one-data2
  {:doc                     "Modify department"
   :dadysql.core/name       [:insert-dept]
   :dadysql.core/model      :department
   :dadysql.core/param-spec {:id int?}
   :dadysql.core/sql        ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"]
   :dadysql.core/extend     {:insert-dept {:dadysql.core/default-param [[:transaction_id :dadysql.core/param-ref-con 0]
                                                                     [:transaction_id :dadysql.core/param-ref-con 0]]
                                           :dadysql.core/timeout    30}}})




(def compile-one-data
  {:doc                     "Modify department"
   :dadysql.core/name       [:insert-dept :update-dept :delete-dept]
   :dadysql.core/model      :department
   :dadysql.core/param-spec :tie-edn2/get-dept-by-id
   :dadysql.core/sql        ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"
                             "update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id"
                             "delete from department where id in (:id)"]
   :dadysql.core/extend     {:insert-dept {:dadysql.core/default-param [[:transaction_id :dadysql.core/param-ref-con 0]
                                                                     [:transaction_id :dadysql.core/param-ref-con 0]]
                                           :dadysql.core/timeout    30}
                             ;:update-dept {:dadysql.core/default-param [[:next_transaction_id :ref-fn-key 'inc :transaction_id]]}
                             :delete-dept {:dadysql.core/param-spec :tie-edn2/get-dept-by-id}}})



(def compile-one-expected-result
  [{:dadysql.core/timeout    30,
    :doc                     "Modify department"
    :dadysql.core/param-spec :tie-edn2/get-dept-by-id,
    :dadysql.core/default-param [[:transaction_id :dadysql.core/param-ref-con 0]],
    :dadysql.core/index      0,
    :dadysql.core/name       :insert-dept,
    :dadysql.core/sql        ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"
                              :id
                              :transaction_id
                              :dept_name],
    :dadysql.core/model      :department,
    :dadysql.core/dml        :dadysql.core/dml-insert}
   {:dadysql.core/timeout    1000,
    :doc                     "Modify department"
    :dadysql.core/param-spec :tie-edn2/get-dept-by-id,

    :dadysql.core/index      1,
    :dadysql.core/name       :update-dept,
    :dadysql.core/sql
                             ["update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id"
                              :dept_name
                              :next_transaction_id
                              :transaction_id
                              :id],
    :dadysql.core/model      :department,
    :dadysql.core/dml        :dadysql.core/dml-update}
   {:dadysql.core/timeout    1000,
    :doc                     "Modify department"
    :dadysql.core/param-spec :tie-edn2/get-dept-by-id,
    :dadysql.core/index      2,
    :dadysql.core/name       :delete-dept,
    :dadysql.core/sql        ["delete from department where id in (:id)" :id],
    :dadysql.core/model      :department,
    :dadysql.core/dml        :dadysql.core/dml-delete}]

  )


;(s/spec? integer?)




(def do-compile-input-data
  [{:dadysql.core/name         :_global_
    :dadysql.core/doc          "global."
    :dadysql.core/file-reload  true
    :dadysql.core/timeout      1000
    :dadysql.core/reserve-name #{:create-ddl :drop-ddl :init-data}
    :dadysql.core/tx-prop      [:isolation :serializable :read-only? true]
    :dadysql.core/join         [[:department :id :dadysql.core/join-one-many :employee :dept_id]
                                [:employee :id :dadysql.core/join-one-one :employee-detail :employee_id]
                                [:employee :id :dadysql.core/join-many-many :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]}
   {:dadysql.core/doc        "spec"
    :dadysql.core/name       [:get-dept-list :get-dept-by-ids :get-employee-list :get-meeting-list :get-employee-meeting-list]
    :dadysql.core/model      [:department :department :employee :meeting :employee-meeting]
    :dadysql.core/extend     {:get-dept-by-ids {:param-spec          :tie-edn2/get-dept-by-id
                                                :dadysql.core/result #{:dadysql.core/result-array}}
                              :get-dept-list   {:dadysql.core/result #{:dadysql.core/result-array}}}
    :dadysql.core/timeout    5000
    :dadysql.core/result     #{:dadysql.core/result-array}
    :dadysql.core/default-param [[:limit :dadysql.core/param-ref-con 10]
                              [:offset :dadysql.core/param-ref-con 0]]
    :dadysql.core/sql        ["select * from department LIMIT :limit OFFSET :offset"
                              "select * from department where id in (:id) "
                              "select * from employee LIMIT :limit OFFSET :offset"
                              "select * from meeting LIMIT :limit OFFSET :offset"
                              "select * from employee_meeting LIMIT :limit OFFSET :offset"]}])



(def do-compile-expected-result
  {:_global_
   {:dadysql.core/name         :_global_,
    :dadysql.core/doc          "global.",
    :dadysql.core/file-reload  true,
    :dadysql.core/timeout      1000,
    :dadysql.core/reserve-name #{:create-ddl :init-data :drop-ddl},
    :dadysql.core/tx-prop      [:isolation :serializable :read-only? true]},
   :get-dept-list
   {:dadysql.core/timeout    5000,
    :dadysql.core/result     #{:array},
    :dadysql.core/default-param [[:limit :ref-con 10] [:offset :ref-con 0]],
    :dadysql.core/join       [[:department :id :1-n :employee :dept_id]],
    :dadysql.core/name       :get-dept-list,
    :dadysql.core/index      0,
    :dadysql.core/sql
                             ["select * from department limit :limit offset :offset"
                              :limit
                              :offset],
    :dadysql.core/model      :department,
    :dadysql.core/dml        :dadysql.core/dml-select},
   :get-dept-by-ids
   {:dadysql.core/index      1,
    :dadysql.core/name       :get-dept-by-ids,
    :dadysql.core/default-param [[:limit :ref-con 10] [:offset :ref-con 0]],
    :dadysql.core/sql        ["select * from department where id in (:id) " :id],
    :dadysql.core/result     #{:array},
    :dadysql.core/timeout    5000,
    :dadysql.core/param-spec :tie-edn2/get-dept-by-id,
    :dadysql.core/dml        :dadysql.core/dml-select,
    :dadysql.core/join       [[:department :id :1-n :employee :dept_id]],
    :dadysql.core/model      :department},
   :get-employee-list
   {:dadysql.core/timeout    5000,
    :dadysql.core/result     #{:array},
    :dadysql.core/default-param [[:limit :ref-con 10] [:offset :ref-con 0]],
    :dadysql.core/join       [[:employee :id :1-1 :employee-detail :employee_id]
                              [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]
                              [:employee :dept_id :n-1 :department :id]],
    :dadysql.core/name       :get-employee-list,
    :dadysql.core/index      2,
    :dadysql.core/sql
                             ["select * from employee limit :limit offset :offset"
                              :limit
                              :offset],
    :dadysql.core/model      :employee,
    :dadysql.core/dml        :select},
   :get-meeting-list
   {:dadysql.core/timeout    5000,
    :dadysql.core/result     #{:array},
    :dadysql.core/default-param [[:limit :ref-con 10] [:offset :ref-con 0]],
    :dadysql.core/join       [[:meeting :meeting_id :n-n :employee :id [:employee-meeting :meeting_id :employee_id]]],
    :dadysql.core/name       :get-meeting-list,
    :dadysql.core/index      3,
    :dadysql.core/sql
                             ["select * from meeting limit :limit offset :offset" :limit :offset],
    :dadysql.core/model      :meeting,
    :dadysql.core/dml        :dadysql.core/dml-select},
   :get-employee-meeting-list
   {:dadysql.core/timeout    5000,
    :dadysql.core/result     #{:array},
    :dadysql.core/default-param [[:limit :ref-con 10] [:offset :ref-con 0]],
    :dadysql.core/name       :get-employee-meeting-list,
    :dadysql.core/index      4,
    :dadysql.core/sql
                             ["select * from employee_meeting limit :limit offset :offset"
                              :limit
                              :offset],
    :dadysql.core/model      :employee-meeting,
    :dadysql.core/dml        :dadysql.core/dml-select}})






