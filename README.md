# tiesql

It is clojure lib to execute sql statement. It is data relational mapping (DRM).

## Features

* SQL statement is first class DSL
* _Meta Data_ Model SQL statement based on meta data definition.
* _Key generator_ Primary key or other key using database seq or clojure fn
* _Nested data_ using data join    
* _Pagination:_ using default param key  Default param key for pagination 
* _Optimistic concurrency:_ using param version
* _Fast data load:_ Parallel query execution to pull data from database 


## API naming and convention 

API are like git pull and push as SQL database are consider as remote part for application.
 
* _pull_  Query something from database, same as clojure.jdbc/query 
* _push!_ Add or modify something to database with transaction if there is no outer transaction. Same as clojure.jdbc/execute!


## More 
[tiesql metadata](doc/METADATA.md)

## Installation

Add this to your [Leiningen](https://github.com/technomancy/leiningen) `:dependencies`: 

[![Clojars Project](http://clojars.org/tiesql/latest-version.svg)](http://clojars.org/tiesql)

And setup your namespace imports:

```clj
(ns app
 (:require [tiesql.jdbc :as tj]))
```

## Usage

* Create sql file with name e.g  _tie.edn.sql_.
* Define meta data in comment block 
* After comment block add sql statement

```sql
/*
{:name :get-dept-by-id}
*/
select * from department where id = :id ;
```

```clj
(tj/pull ds (tj/read-file "tie.edn.sql") :name :get-dept-by-id :params {:id 1})
;; Output
[{:id 1, :transaction_id 0, :dept_name "Business"}]
```
**add validation meta data, **
```sql
/*
{:name :get-dept-by-id
 :validation [[:id :type Long "Id will be Long"]]}
*/
select * from department where id = :id ;
```
```clj
(tj/pull ds (tj/read-file "tie.edn.sql") :name :get-dept-by-id)
;; Output with error as params is not provided
{:error "Input is missing for :get-dept-by-id "}

(tj/pull ds (tj/read-file "tie.edn.sql") :name :get-dept-by-id :params {:id "1")
;; Outout with error as id type is wrong
{:error
 {:msg "Id will be Long ", :value " 1", :type "class java.lang.String"}}
```
*Add more sql statement *

```sql
/*
{:doc " Load department and employee. "
 :name  [:get-dept-by-id :get-dept-employee]
 :model [:department :employee ]
 :validation [[:id :type Long "Id will be Long"]]
 }*/
select * from department where id = :id ;
select * from employee where dept_id = :id;
```

```clj
(tj/pull ds (tj/read-file "tie.edn.sql") :name [:get-dept-by-id] :params {:id 1})
;; Output with model name
{:department [{:id 1, :transaction_id 0, :dept_name "Business"}]}
```

```clj
(tj/pull ds (tj/read-file "tie.edn.sql") :name [:get-dept-by-id :get-dept-employee] :params {:id 1})
;; Output with model name
{:department [{:id 1, :transaction_id 0, :dept_name "Business"}]
 :employee   [{:id 1,   :transaction_id 0, :firstname "Abba", :lastname "Zoma", :dept_id 1}]}
```
Update sql file, define join with _global_ in the top of the file. It is palce for all abstract defination that will be extened to other module.
```sql
/*
{:name :_global_
 :doc "Abstract configuration, timeout will be used to all sql statement if it is not defined of it owns."
 :join [[:department :id :1-n :employee :dept_id]]
 }*/
```
With join
```clj
(tj/pull ds (tj/read-file "tie.edn.sql") :name [:get-dept-by-id :get-dept-employee] :params {:id 1})
;; Output
{:department
 {:id 1,
  :transaction_id 0,
  :dept_name "Business",
  :employee
  [{:id 1,
    :transaction_id 0,
    :firstname "Abba",
    :lastname "Zoma",
    :dept_id 1}]}}
```

Write in database

```clj
(let [employee {:employee {:firstname       "Schwan"
                           :lastname        "Ragg"
                           :dept_id         1
                           :employee-detail {:street  "Schwan",
                                             :city    "Munich",
                                             :state   "Bayern",
                                             :country "Germany"}}}]
    (t/push! ds
             (tj/read-file "tie.edn.sql")
             :name [:insert-employee :insert-employee-detail]
             :params employee))
```
Output
```clj
{:employee [1], :employee-detail [1]}
```

## See complete example 

[tie.edn.sql](test-i/tie.edn.sql "Example")

[walkthrough](test-i/tiesql/jdbc_test.clj "Test file")




## License

Copyright © 2015 Abdullah Al Mamun

Distributed under the Eclipse Public License, the same as Clojure.
