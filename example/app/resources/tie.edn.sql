/*
{:name :_global_
 :doc "Abstract configuration, timeout will be used to all sql statement if it is not defined of it owns."
 :file-reload true
 :timeout 1000
 :reserve-name #{:init-db :drop-ddl :init-data}
 :tx-prop [:isolation :serializable :read-only? true]
 :join [[:department :id :1-n :employee :dept_id]
        [:employee :id :1-1 :employee-detail :employee_id]
        [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]]

 }*/


/*
{:doc "It will return sequence value. It will extend all keys that are defined also in abstract"
 :name [:gen-dept :gen-empl :gen-meet]
 :result #{:single}
 }*/
call next value for seq_dept;
call next value for seq_empl;
call next value for seq_meet;


/*
{:doc "General select statement. Name is used to identify each query, Abstract timeout will override with timeout here  "
 :name  [:get-dept-list :get-dept-by-ids :get-employee-list :get-meeting-list :get-employee-meeting-list]
 :model [:department :department :employee :meeting :employee-meeting]
 :extend {:get-dept-by-ids {:validation [[:id :type [] "Id will be sequence"]
                                         [:id :contain Long "Id contain will be Long "]]
                                     :result #{:array}}}
 :timeout 5000
 :result #{:array}
 :params [[:limit :ref-con 10]
          [:offset :ref-con 0]]
 :skip #{:join}
  }*/
select * from department LIMIT :limit OFFSET :offset;
select * from department where id in (:id) ;
select * from employee LIMIT :limit OFFSET :offset;
select * from meeting LIMIT :limit OFFSET :offset;
select * from employee_meeting LIMIT :limit OFFSET :offset;



/*
{:doc "Load employee with dept, details and meeting  "
 :name [:get-employee-by-id :get-employee-dept :get-employee-detail :get-employee-meeting]
 :model [:employee :department :employee-detail :employee-meeting]
 :group :load-employee
 :extend {:get-employee-by-id  {:result #{:single}}
          :get-employee-dept   {:result #{:single}}
          :get-employee-detail {:result #{:single}}
          :get-employee-meeting {:result #{:array}}}
 :validation [[:id :type Long "Id will be Long" ]]
 }*/
select * from employee where id = :id;
select d.* from department d, employee e where e.id=:id and d.id = e.dept_id;
select ed.* from employee_detail ed where ed.employee_id=:id;
select m.*, em.employee_id from meeting m, employee_meeting em where em.employee_id=:id and em.meeting_id = m.meeting_id;


/*
{:doc "Get all meeting"
 :name [:get-meeting-by-id :get-employee-for-meeting ]
 :group :load-meeting
 :extend {:get-meeting-by-id {:model :meeting
                              :result #{:single}
                              :validation [[:id :type Long "Id will be sequence"]
                                           [:id :range 10 11 "Id range will be between 10 and 11"]]}
            :get-employee-for-meeting {:model :employee-meeting}}
 }*/
select * from meeting where  meeting_id = :id;
select e.*, em.employee_id from employee e, employee_meeting em where em.meeting_id = :id and em.employee_id = e.id;




/*
{:doc " General select statement with extend. "
 :name  [:get-dept-by-id :get-dept-employee ]
 :model [:department :employee :department]
 :group :load-dept
 :extend {:department {:timeout 2000
                      :validation [[ :id :type Long "Id will be Long "]]
                      :result #{:single}}
         :get-dept-by-id {:timeout 3000}}
 :timeout 5000
 :validation [[:id :type Long "Id will be Long"]]
 }*/
select * from department where id = :id ;
select * from employee where dept_id = :id;




/*
{:doc "Modify department"
 :name [:create-dept :update-dept :delete-dept]
 :model :department
 :extend {:create-dept {:params [[:id :ref-gen :gen-dept]
                                [:transaction_id :ref-con 0]]}
         :update-dept {:params [[:next_transaction_id :ref-fn-key inc :transaction_id]]}
         :delete-dept {:validation [[:id :type [] "Id will be sequence"]
                                    [:id :contain Long "Id contain will be Long "]]}}
 :validation [[:id :type Long "Id will be Long"]]
 :commit :all
 }*/
insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name);
update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id;
delete from department where id in (:id);


/*
{:doc "Modify employee with dept, details and meeting  "
 :name [:create-employee :create-employee-detail ]
 :group :create-employee
 :extend {:create-employee {:model :employee
                            :params [[:transaction_id :ref-con 0]
                                     [:id :ref-gen :gen-dept ]]}
           :create-employee-detail {:model :employee-detail
                                    :params [[:city :ref-con 0]
                                             [:id :ref-gen :gen-dept ]]}}
 :validation [[:id :type Long "Id will be Long"]]
 :commit :all
 }*/
insert into employee (id,  transaction_id,  firstname,  lastname,  dept_id)
             values (:id, :transaction_id, :firstname, :lastname, :dept_id);
insert into employee_detail (employee_id, street,   city,  state,  country )
                    values (:employee_id, :street, :city, :state, :country);



/*
{:doc "Add new meeting"
 :name [:create-meeting :create-employee-meeting ]
 :group :create-meeting
 :extend {:create-meeting {:model :meeting
                           :params [[:meeting_id :ref-gen :gen-meet]]}
          :create-employee-meeting {:model :employee-meeting}}
 :commit :all
 }*/
insert into meeting (meeting_id, subject) values (:meeting_id, :subject);
insert into employee_meeting (employee_id, meeting_id) values (:employee_id, :meeting_id);

/*
{:doc ""
:name [:update-employee-dept]
:extend {:update-employee-dept {:model :employee
                                :params [[:next_transaction_id :ref-fn-key inc :transaction_id]]}}
}
*/
update employee set dept_id=:dept_id, transaction_id=:next_transaction_id where transaction_id=:transaction_id and id=:id;


/*
{:name :init-db
 :doc "It is reserve name as defined in _config_. Nothing will be process here during compile time. "
 }*/
create table if not exists department (
    id integer primary key,
    transaction_id integer NOT NULL,
    dept_name varchar(50) NOT NULL
);

create table if not exists employee (
    id integer primary key,
    transaction_id integer NOT NULL,
    firstname varchar(50) NOT NULL,
    lastname varchar(50) NOT NULL,
    dept_id integer NOT NULL
);


create table if not exists employee_detail (
    employee_id integer primary key,
    street varchar(50) NOT NULL ,
    city varchar(50) NOT NULL ,
    state varchar(50) NOT NULL ,
    country varchar(50) NOT NULL
);


create table if not exists meeting (
    meeting_id integer primary key,
    subject VARCHAR(50) NOT NULL
);


create table if not exists employee_meeting (
    employee_id integer not null,
    meeting_id integer not null
);

create sequence if not exists seq_meet start with 100 increment by 1;
create sequence if not exists seq_dept start with 100 increment by 1;
create sequence if not exists seq_empl start with 100 increment by 1;


/*
{:name :drop-ddl
 :doc " drop database schema  "
 }*/
drop table employee_meeting;
drop table meeting;
drop table employee;
drop table department;
drop sequence seq_empl;
drop sequence seq_dept;
drop sequence seq_meet;


/*
{:name :init-data
 :doc " add data, need for testing, change data may need to fix test cases    "
 }*/
insert into department (id, transaction_id, dept_name) values (1, 0, 'Business' );
insert into department (id, transaction_id, dept_name) values (2, 0, 'Marketing' );
insert into department (id, transaction_id, dept_name) values (3, 0, 'HR' );
insert into employee   (id, transaction_id, firstname, lastname, dept_id) values (1, 0, 'Abba', 'Zoma', 1 );
insert into employee   (id, transaction_id, firstname, lastname, dept_id) values (2, 0, 'Bala', 'Zen', 2 );
insert into employee   (id, transaction_id, firstname, lastname, dept_id) values (3, 0, 'Jal', 'Kan', 3 );
insert into employee_detail (employee_id, street, city, state, country) values (1, 'Schwan', 'Munich','Bayern', 'GRE');
insert into employee_detail (employee_id, street, city, state, country) values (2, 'Schwan2', 'Munich','Bayern', 'GRE');
insert into employee_detail (employee_id, street, city, state, country) values (3, 'Schwan3', 'Munich','Bayern', 'GRE');
insert into meeting (meeting_id, subject) values (1, 'Hello');
insert into meeting (meeting_id, subject) values (2, 'Hello Friday');
insert into employee_meeting (employee_id, meeting_id) values (1, 1);
insert into employee_meeting (employee_id, meeting_id) values (1, 2);
