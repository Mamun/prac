(ns dadysql.compiler.test-data
  (:require [clojure.spec :as s]))



(def compile-one-data2
  {:doc                     "Modify department"
   :dadysql.spec/name       [:insert-dept ]
   :dadysql.spec/model      :department
   :dadysql.spec/param-spec {:id int?}
   :dadysql.spec/sql        ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"]
   :dadysql.spec/extend     {:insert-dept {:dadysql.spec/param   [[:transaction_id :ref-con 0]
                                                                  [:transaction_id :ref-con 0]]
                                           :dadysql.spec/timeout 30}}})




(def compile-one-data
  {:doc                     "Modify department"
   :dadysql.spec/name       [:insert-dept :update-dept :delete-dept]
   :dadysql.spec/model      :department
   :dadysql.spec/param-spec :tie-edn2/get-dept-by-id
   :dadysql.spec/sql        ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"
                             "update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id"
                             "delete from department where id in (:id)"]
   :dadysql.spec/extend     {:insert-dept {:dadysql.spec/param   [[:transaction_id :ref-con 0]
                                                                  [:transaction_id :ref-con 0]]
                                           :dadysql.spec/timeout 30}
                             ;:update-dept {:dadysql.spec/param [[:next_transaction_id :ref-fn-key 'inc :transaction_id]]}
                             :delete-dept {:dadysql.spec/param-spec :tie-edn2/get-dept-by-id}}}
  )


(def compile-one-expected-result
  [{:dadysql.spec/timeout    30,
    :doc  "Modify department"
    :dadysql.spec/param-spec :tie-edn2/get-dept-by-id,
    :dadysql.spec/param      [[:transaction_id :ref-con 0]],
    :dadysql.spec/index      0,
    :dadysql.spec/name       :insert-dept,
    :dadysql.spec/sql
                             ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"
                              :id
                              :transaction_id
                              :dept_name],
    :dadysql.spec/model      :department,
    :dadysql.spec/dml-key   :dadysql.spec/dml-insert}
   {:dadysql.spec/timeout    1000,
    :doc  "Modify department"
    :dadysql.spec/param-spec :tie-edn2/get-dept-by-id,

    :dadysql.spec/index      1,
    :dadysql.spec/name       :update-dept,
    :dadysql.spec/sql
                             ["update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id"
                              :dept_name
                              :next_transaction_id
                              :transaction_id
                              :id],
    :dadysql.spec/model      :department,
    :dadysql.spec/dml-key   :dadysql.spec/dml-update}
   {:dadysql.spec/timeout    1000,
    :doc  "Modify department"
    :dadysql.spec/param-spec :tie-edn2/get-dept-by-id,
    :dadysql.spec/index      2,
    :dadysql.spec/name       :delete-dept,
    :dadysql.spec/sql        ["delete from department where id in (:id)" :id],
    :dadysql.spec/model      :department,
    :dadysql.spec/dml-key   :dadysql.spec/dml-delete}]

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
    :param   [[:limit :ref-con 10]
              [:offset :ref-con 0]]
    :sql     ["select * from department LIMIT :limit OFFSET :offset"
              "select * from department where id in (:id) "
              "select * from employee LIMIT :limit OFFSET :offset"
              "select * from meeting LIMIT :limit OFFSET :offset"
              "select * from employee_meeting LIMIT :limit OFFSET :offset"]}])



(def do-compile-expected-result
  {:_global_
   {:dadysql.spec/name         :_global_,
    :dadysql.spec/doc          "global.",
    :dadysql.spec/file-reload  true,
    :dadysql.spec/timeout      1000,
    :dadysql.spec/reserve-name #{:create-ddl :init-data :drop-ddl},
    :dadysql.spec/tx-prop      [:isolation :serializable :read-only? true]},
   :get-dept-list
   {:dadysql.spec/timeout  5000,
    :dadysql.spec/result   #{:array},
    :dadysql.spec/param    [[:limit :ref-con 10] [:offset :ref-con 0]],
    :dadysql.spec/join     [[:department :id :1-n :employee :dept_id]],
    :dadysql.spec/name     :get-dept-list,
    :dadysql.spec/index    0,
    :dadysql.spec/sql
                           ["select * from department limit :limit offset :offset"
                            :limit
                            :offset],
    :dadysql.spec/model    :department,
    :dadysql.spec/dml-key :dadysql.spec/dml-select},
   :get-dept-by-ids
   {:dadysql.spec/index      1,
    :dadysql.spec/name       :get-dept-by-ids,
    :dadysql.spec/param      [[:limit :ref-con 10] [:offset :ref-con 0]],
    :dadysql.spec/sql        ["select * from department where id in (:id) " :id],
    :dadysql.spec/result     #{:array},
    :dadysql.spec/timeout    5000,
    :dadysql.spec/param-spec :tie-edn2/get-dept-by-id,
    :dadysql.spec/dml-key   :dadysql.spec/dml-select,
    :dadysql.spec/join       [[:department :id :1-n :employee :dept_id]],
    :dadysql.spec/model      :department},
   :get-employee-list
   {:dadysql.spec/timeout  5000,
    :dadysql.spec/result   #{:array},
    :dadysql.spec/param    [[:limit :ref-con 10] [:offset :ref-con 0]],
    :dadysql.spec/join     [[:employee :id :1-1 :employee-detail :employee_id]
                            [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]
                            [:employee :dept_id :n-1 :department :id]],
    :dadysql.spec/name     :get-employee-list,
    :dadysql.spec/index    2,
    :dadysql.spec/sql
                           ["select * from employee limit :limit offset :offset"
                            :limit
                            :offset],
    :dadysql.spec/model    :employee,
    :dadysql.spec/dml-key :select},
   :get-meeting-list
   {:dadysql.spec/timeout  5000,
    :dadysql.spec/result   #{:array},
    :dadysql.spec/param    [[:limit :ref-con 10] [:offset :ref-con 0]],
    :dadysql.spec/join     [[:meeting :meeting_id :n-n :employee :id [:employee-meeting :meeting_id :employee_id]]],
    :dadysql.spec/name     :get-meeting-list,
    :dadysql.spec/index    3,
    :dadysql.spec/sql
                           ["select * from meeting limit :limit offset :offset" :limit :offset],
    :dadysql.spec/model    :meeting,
    :dadysql.spec/dml-key :dadysql.spec/dml-select},
   :get-employee-meeting-list
   {:dadysql.spec/timeout  5000,
    :dadysql.spec/result   #{:array},
    :dadysql.spec/param    [[:limit :ref-con 10] [:offset :ref-con 0]],
    :dadysql.spec/name     :get-employee-meeting-list,
    :dadysql.spec/index    4,
    :dadysql.spec/sql
                           ["select * from employee_meeting limit :limit offset :offset"
                            :limit
                            :offset],
    :dadysql.spec/model    :employee-meeting,
    :dadysql.spec/dml-key :dadysql.spec/dml-select}})




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
   {:doc        "spec"
    :name       [:get-dept-list :get-dept-by-ids :get-employee-list :get-meeting-list :get-employee-meeting-list]
    :model      [:department :department :employee :meeting :employee-meeting]
    :extend     {:get-dept-by-ids {:param-spec :tie-edn/get-dept-by-id
                                   :result     #{:array}}
                 :get-dept-list   {:result #{:array}}}
    :timeout    5000
    :result     #{:array}
    :param-spec :tie-edn/get-dept-by-id2
    :param      [[:limit :ref-con 10]
                 [:offset :ref-con 0]]
    :sql        ["select * from department LIMIT :limit OFFSET :offset"
                 "select * from department where id in (:id) "
                 "select * from employee LIMIT :limit OFFSET :offset"
                 "select * from meeting LIMIT :limit OFFSET :offset"
                 "select * from employee_meeting LIMIT :limit OFFSET :offset"]}])

