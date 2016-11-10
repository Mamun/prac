/*
{:name :_global_
 :doc "Abstract configuration, timeout will be used to all sql statement if it is not defined of it owns."
 :file-reload true
 :timeout 1000
 :reserve-name #{:init-db :drop-ddl :init-data}
 :tx-prop [:isolation :serializable :read-only? false]
 :join [[:department :id :1-n :employee :dept_id]
        [:employee :id :1-1 :employee-detail :employee_id]
        [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]
 }*/


/*
{:doc "General select statement. Name is used to identify each query, Abstract timeout will override with timeout here  "
 :name  [:get-dept-list :get-dept-by-ids :get-employee-list :get-meeting-list :get-employee-meeting-list]
 :model [:department :department :employee :meeting :employee-meeting]
 :extend {:get-dept-by-ids {:param-spec {:req {:id (clojure.spec/coll-of int? :kind vector?) }}
                            :result #{:array}}}
 :timeout 5000
 :result #{:array}
 :param [:limit 10
         :offset 0 ]
 :skip #{:join}
  }*/
select * from department LIMIT :limit OFFSET :offset;
select * from department where id in (:id) ;
select * from employee LIMIT :limit OFFSET :offset;
select * from meeting LIMIT :limit OFFSET :offset;
select * from employee_meeting LIMIT :limit OFFSET :offset;

