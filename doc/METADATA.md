## WORK IN PROGRESS

Define meta data based for each sql statement. 

## Meta data definition for sql
* Metadata is just like annotation in OOP.
* Execute SQL statement based on meta data definition.
* Inheritance base meta data definition. 
* Plugin support to define new meta data.  


## Supported meta data 

* _:name_ -  Identifier of sql statement. Value will be **_keyword_** or **seq of _keyword_**.
* :doc -  About your sql statement. value will be **_string_**
* :timeout - Define timeout to execute sql statement. Value will be **_Long_** 
* :reserve-name - Name that are not process during read time. Sql statement will stay as it is defined. Good for db init script like create table or init table. Value will be seq of _:name_ 
* :tx-prop - Define jdbc tx-proc. Value will be **_[:isolation :serializable :read-only? true]_**. Default value is **_read-only?_**
* :join - How result data will be assoc or disssoc for pull batch or push batch. Value will be seq of _single join_
* :result - About sql statment result. Value will be **_#{:single :array}_**. _:single_ will return single value and array will be clojure.jdbc array option.
  
## About global, group and specific sql based meta data
* global definition   - For database level as an example tx property, sql statement timeout, joining
* group  definition   - For group of sql statement like pagination for all table, general validation etc
* specific definition - For specific sql statement like type, validation etc
* 

## Meta data inheritance 
