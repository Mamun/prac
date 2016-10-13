/*
{:name :_global_
 :doc "Abstract configuration, timeout will be used to all sql statement if it is not defined of it owns."
 :file-reload true
 :timeout 1000
 :reserve-name #{:create-ddl :drop-ddl :init-data}
 :tx-prop [:isolation :serializable :read-only? true]
 :spec-file tie-edn
 :join [[:department :id :1-n :employee :dept_id]
        [:employee :id :1-1 :employee-detail :employee_id]
        [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]

 }*/




/*
{:doc " General select statement with extend. "
 :name  [:get-dept-by-id :get-dept-employee ]
 :model [:department :employee ]
 :group :load-dept
 :extend {:get-dept-by-id {:timeout 2000
                           :param [[:next_transaction_id :ref-fn-key inc :transaction_id]]
                           :param-spec {:id (s/coll-of int? :kind vector?)}
                           :result #{:single}}
         }
 :timeout 5000
 :param-spec {:id int?}
 }*/
select * from department where id = :id ;
select * from employee where dept_id = :id;


