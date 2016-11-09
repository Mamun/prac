/*
{:name :_global_
 :doc "Abstract configuration, timeout will be used to all sql statement if it is not defined of it owns."
 :file-reload true
 :timeout 1000
 :reserve-name #{:create-ddl :drop-ddl :init-data}
 :tx-prop [:isolation :serializable :read-only? true]
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
                           :param {:next_transaction_id (inc :id)
                                   :next_transaction_id2 (constantly 10)
                           }
                           :param-spec {:req {:id int?}}
                           :result #{:single}}}
 :timeout 5000
 :param-spec {:req {:id int?}}
 }*/
select * from department where id = :id ;
select * from employee where dept_id = :id;



/*
{:name :create-ddl
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
