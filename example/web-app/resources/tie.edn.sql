/*
{:name :_global_
 :doc "Abstract configuration, timeout will be used to all sql statement if it is not defined of it owns."
 :file-reload true
 :timeout 1000
 :reserve-name #{:create-ddl :drop-ddl :init-data}
 :tx-prop [:isolation :serializable :read-only? false]
 :join [[:deal :id :1-1 :user :dept_id]]

 }*/


/*
{:doc "It will return sequence value. It will extend all keys that are defined also in abstract"
 :name [:gen-deal ]
 :result #{:single}
 }*/
call next value for seq_deal;


/*
{:doc "General select statement. Name is used to identify each query, Abstract timeout will override with timeout here  "
 :name  [:get-deal-list]
 :model [:deal]
 :timeout 5000
 :params [[:limit :ref-con 10]
          [:offset :ref-con 0]]
 :skip #{:join}
  }*/
select * from deal LIMIT :limit OFFSET :offset;



/*
{:name :create-ddl
 :doc "It is reserve name as defined in _config_. Nothing will be process here during compile time. "
 }*/
create table if not exists deal (
    id integer primary key,
    transaction_id integer NOT NULL,
    deal_owner_id integer NOT NULL,
    orginal_price integer NOT NULL,
    deal_price integer NOT NULL,
    deal_percent integer NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(200) NOT NULL,
    image_link VARCHAR(200) NOT NULL
);
create sequence if not exists seq_deal start with 100 increment by 1;


/*
{:name :drop-ddl
 :doc " drop database schema  "
 }*/
drop table deal;
drop sequence seq_deal;



/*
{:name :init-data
 :doc " add data, need for testing, change data may need to fix test cases    "
 }*/
insert into deal (id, transaction_id, deal_owner_id, orginal_price, deal_price, deal_percent, title, description, image_link) values (1, 0, 1, 10, 5, 50, 'Pizza in 50 %',  'Nimble-handed chefs cook traditional Japanese dishes before diners eyes, sending up aromas of filet mignon, scallops, and hibachi noodles', 'images/donner.jpg');
insert into deal (id, transaction_id, deal_owner_id, orginal_price, deal_price, deal_percent, title, description, image_link) values (2, 0, 1, 10, 5, 50, 'Burger in 50 %', 'Nimble-handed chefs cook traditional Japanese dishes before diners eyes, sending up aromas of filet mignon, scallops, and hibachi noodles', 'images/donner.jpg' );
insert into deal (id, transaction_id, deal_owner_id, orginal_price, deal_price, deal_percent, title, description, image_link) values (3, 0, 1, 10, 5, 50, 'Donner in 50 %', 'Nimble-handed chefs cook traditional Japanese dishes before diners eyes, sending up aromas of filet mignon, scallops, and hibachi noodles', 'images/donner.jpg' );
