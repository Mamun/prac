(ns dadysql.compiler.core-test
  (:use [clojure.test]
        [dadysql.compiler.core :as r]
        [dadysql.core :as c]
        [dadysql.constant]
        [dady.common])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))



(deftest compiler-merge-test
  (testing "test compiler-merge "
    (let [v [{param-key      [[1 2 3]]
              column-key     {:p 1}
              timeout-key    4
              result-key     #{result-single-key}
              validation-key [[:id :type 'int? "id will be long"]]}
             {param-key   [[8 9 0]]
              timeout-key 6}
             {param-key      [[5 6 7]]
              column-key     {:p  4
                              :p1 :p}
              :p             9
              result-key     #{result-array-key}
              validation-key [[:id :type 'vector? "id will be sequence"]
                              [:id :contain 'int? "id contain will be int "]]}]
          expected-result {param-key   [[5 6 7] [8 9 0] [1 2 3]],
                           column-key  {:p 4, :p1 :p}
                           timeout-key 6
                           result-key  #{result-array-key}
                           :p          9}
          actual-result (apply merge-with compiler-merge v)]
      ;(clojure.pprint/pprint actual-result)
      (is (= expected-result
             actual-result)))))


;(compiler-merge-test)


;(apply-compile-test)

;(resolve 'long?)

;(map-name-model-sql-test)

(declare compile-one-data)
(declare compile-one-expected-result)




(deftest compile-one-test
  (testing "test compile-one "
    (let [config (r/default-config)


          actual-result (r/compile-one compile-one-data config)]

      (is (= actual-result
             compile-one-expected-result))))
  (testing "test compile-one"
    (let [config (r/default-config)
          w {:doc  "Modify department"
             :name [:insert-dept :update-dept :delete-dept]
             :sql  ["call next value for seq_dept"
                    "call next value for seq_empl"
                    "call next value for seq_meet"]}
          actual-result (->> (compile-one w config))]
      (is (not (empty? actual-result)))))
  )



;(compile-one-test)


(declare do-compile-input-data)
(declare do-compile-expected-result)


(deftest do-compile-test
  (testing "test do-compile "
    (let [actual-result (r/do-compile do-compile-input-data)]
      ; (clojure.pprint/pprint actual-result)
      (is (not-empty actual-result))
      (is (= do-compile-expected-result actual-result))
      (is (not-empty (:get-dept-list actual-result)))
      (is (not-empty (:get-dept-by-ids actual-result)))
      (is (not-empty (:get-employee-list actual-result))))))



;(do-compile-test)




;(do-compile-test)

;(run-tests)




;(do-compile2-test)



;(do-compile-test2)




;(run-tests)


;(compilter-test)



#_(deftest do-compile4-test
  (testing "test do -compile"
    (let [w (r/read-file "tie.edn2.sql")]
      ;(clojure.pprint/pprint w)
      )
    ))


(comment

  (do
    (do-compile4-test)
    nil)

  )




(def compile-one-data
  {:doc        "Modify department"
   :name       [:insert-dept :update-dept :delete-dept]
   :model      :department
   :validation [[:id :type 'int? "Id will be Long"]]
   :sql        ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"
                "update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id"
                "delete from department where id in (:id)"]
   :extend     {:insert-dept {:params  [[:transaction_id :ref-con 0]
                                        [:transaction_id :ref-con 0]]
                              :timeout 30}
                :update-dept {:params [[:next_transaction_id :ref-fn-key 'inc :transaction_id]]}
                :delete-dept {:validation [[:id :type 'vector? "Id will be sequence"]
                                           [:id :contain 'int? "Id contain will be Long "]]}}}
  )


(def compile-one-expected-result
  [{:timeout    30,
    :validation [[:id :type #'clojure.core/int? "id will be long"]],
    :params     [[:transaction_id :ref-con 0]],
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
    :validation [[:id :type #'clojure.core/int? "id will be long"]],
    :params
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
    :validation
              [[:id :type #'clojure.core/vector? "id will be sequence"]
               [:id :contain #'clojure.core/int? "id contain will be long "]],
    :index    2,
    :name     :delete-dept,
    :sql      ["delete from department where id in (:id)" :id],
    :model    :department,
    :dml-type :delete}]

  )




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
    :extend  {:get-dept-by-ids {:validation [[:id :type 'vector? "Id will be sequence"]
                                             [:id :contain 'int? "Id contain will be Long "]]
                                :result     #{:array}}
              :get-dept-list   {:result #{:array}}}
    :timeout 5000
    :result  #{:array}
    :params  [[:limit :ref-con 10]
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
    :params   [[:limit :ref-con 10] [:offset :ref-con 0]],
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
    :params   [[:limit :ref-con 10] [:offset :ref-con 0]],
    :sql      ["select * from department where id in (:id) " :id],
    :result   #{:array},
    :timeout  5000,
    :validation
              [[:id :type #'clojure.core/vector? "id will be sequence"]
               [:id :contain #'clojure.core/int? "id contain will be long "]],
    :dml-type :select,
    :join     [[:department :id :1-n :employee :dept_id]],
    :model    :department},
   :get-employee-list
   {:timeout  5000,
    :result   #{:array},
    :params   [[:limit :ref-con 10] [:offset :ref-con 0]],
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
    :params   [[:limit :ref-con 10] [:offset :ref-con 0]],
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
    :params   [[:limit :ref-con 10] [:offset :ref-con 0]],
    :name     :get-employee-meeting-list,
    :index    4,
    :sql
              ["select * from employee_meeting limit :limit offset :offset"
               :limit
               :offset],
    :model    :employee-meeting,
    :dml-type :select}})
