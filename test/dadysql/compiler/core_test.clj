(ns dadysql.compiler.core-test
  (:use [clojure.test]
        [dadysql.compiler.core :as r]
        [dadysql.core :as c]
        [dadysql.constant]
        [dady.common])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen])
  )



(deftest compiler-merge-test
  (testing "test compiler-merge "
    (let [v [{param-key   [[1 2 3]]
              column-key  {:p 1}
              timeout-key 4
              result-key  #{result-single-key}
              validation-key [[:id :type 'int? "id will be long"]] }
             {param-key   [[8 9 0]]
              timeout-key 6}
             {param-key  [[5 6 7]]
              column-key {:p  4
                          :p1 :p}
              :p         9
              result-key #{result-array-key}
              validation-key [[:id :type 'vector? "id will be sequence"]
                              [:id :contain 'int? "id contain will be int "]]
              }]
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

(comment
  )


;(apply-compile-test)



;(map-name-model-sql-test)


(deftest compile-one-test
  (testing "test compile-one "
    (let [config (r/default-config)
          w {:doc        "Modify department"
             :name       [:insert-dept :update-dept :delete-dept]
             :model      :department
             :validation [[:id :type 'long? "Id will be Long"]]
             :sql        ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"
                          "update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id"
                          "delete from department where id in (:id)"]
             :extend     {:insert-dept {:params  [[:transaction_id :ref-con 0]
                                                  [:transaction_id :ref-con 0]]
                                        :timeout 30}
                          :update-dept {:params [[:next_transaction_id :ref-fn-key 'inc :transaction_id]]}
                          :delete-dept {:validation [[:id :type 'vector? "Id will be sequence"]
                                                     [:id :contain 'long? "Id contain will be Long "]]}}}

          expected-result {:name :insert-dept
                           :sql  ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"
                                  :id
                                  :transaction_id
                                  :dept_name]}
          actual-result (->> (r/compile-one w config)
                             (map #(select-keys %1 [:name :sql]))
                             (doall)
                             (first))]
      (is (= actual-result
             expected-result))))
  (testing "test compile-one"
    (let [config (r/default-config)
          w {:doc  "Modify department"
             :name [:insert-dept :update-dept :delete-dept]
             :sql  ["call next value for seq_dept"
                    "call next value for seq_empl"
                    "call next value for seq_meet"]}
          actual-result (->> (compile-one w config))]
      (is (not (empty? actual-result)))))
  (testing "test compile-one with config join "
    (let [config (-> (r/default-config)
                     (assoc join-key [[:department :id :1-n :employee :dept_id]
                                      [:employee :id :1-1 :employee-detail :employee_id]
                                      [:employee :id :n-n :meeting :meeting_id [:employee-meeting :employee_id :meeting_id]]])
                     (r/compile-one-config))
          ;_ (clojure.pprint/pprint config)

          w {:doc        "Modify department"
             :name       [:insert-employee :insert-employee-detail]
             :validation [[:id :type 'int? "Id will be Long"]]
             :sql        ["insert into employee (id,  transaction_id,  firstname,  lastname,  dept_id) values (:id, :transaction_id, :firstname, :lastname, :dept_id) "
                          "insert into employee_detail (employee_id, street,   city,  state,  country ) values (:employee_id, :street, :city, :state, :country)"]
             :extend     {:insert-employee {:model  :employee
                                            :params [[:transaction_id :ref-con 0]
                                                     [:id :ref-gen :gen-dept]]}
                          :insert-employee-detail {:model  :employee-detail
                                                   :params [[:city :ref-con 0]
                                                            [:id :ref-gen :gen-dept]]}}}
          c-result (r/compile-one w config)
          j-key (get-in c-result [0 join-key])
          ]
      ;(clojure.pprint/pprint c-result)
      (is (not (empty? j-key)))
      #_(-> (r/compile-one pc config w)
            (clojure.pprint/pprint))
      )))



#_(let [config (r/default-config)
      w {:doc        "Modify department"
         :name       [:insert-dept :update-dept :delete-dept]
         :model      :department
         :validation [[:id :Type 'long? "Id will be Long"]]
         :sql        ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"
                      "update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id"
                      "delete from department where id in (:id)"]
         :extend     {:insert-dept {:params  [[:transaction_id :ref-con 0]
                                              [:transaction_id :ref-con 0]]
                                    :timeout 30}
                      :update-dept {:params [[:next_transaction_id :ref-fn-key 'inc :transaction_id]]}
                      :delete-dept {:validation [[:id :type 'vector? "Id will be sequence"]
                                                 [:id :contain 'long? "Id contain will be Long "]]}}}


      actual-result (->> (r/compile-one w config)


                         )]
  (clojure.pprint/pprint actual-result)
  )



;(compile-one-test)

#_(deftest compile-one-test3
    (testing "test compile-one with Upper case  "
      (let [config (r/default-config)
            w {:doc    "Modify department"
               :name   [:insert-dept :update-dept]
               :sql    ["insert into department (id, transaction_id, dept_name) values (:Id, :transaction_id, :dept_name)"
                        "call next value for seq_meet"]
               :extend [:insert-dept {:params     [[:transaction_id :ref-con 0]
                                                   [:transaction_id :ref-con 0]]
                                      :validation [[:id :contain 'int? "Id contain will be Long "]]
                                      :timeout    30}]}
            expected-result [{:timeout    30,
                              :params     [[:transaction_id :ref-con 0]],
                              :validation [[:id :contain #'clojure.core/int? "Id contain will be Long "]],
                              :sql        ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"
                                           :id :transaction_id :dept_name],
                              :dml-type   :insert,
                              :index      0,
                              :name       :insert-dept,
                              :model      :insert-dept,
                              :group      nil}
                             {:timeout  1000,
                              :sql      ["call next value for seq_meet"],
                              :dml-type :call,
                              :index    1, :name :update-dept,
                              :model    :update-dept,
                              :group    nil}]
            actual-result (r/compile-one w config)]



        (is (= (select-keys (get-in actual-result [0]) [sql-key timeout-key])
               (select-keys (get-in expected-result [0]) [sql-key timeout-key]))))))


;(compile-one-test3)



(deftest do-compile-test
  (testing "test do-compile "
    (let [w [{:name         :_config_
              :file-reload  true
              :timeout      3000
              :reserve-name #{:create-ddl :drop-ddl :init-data}}
             {:doc        "Modify department"
              :name       [:insert-dept :update-dept :delete-dept]
              :model      :department
              :validation [[:id :type 'int? "Id will be Long"]]
              :sql        ["insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name)"
                           "update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id"
                           "delete from department where id in (:id)"
                           ]
              :extend     {:insert-dept {:params  [[:transaction_id :ref-con 0]
                                                   [:transaction_id :ref-con 0]]
                                         :timeout 30}
                           :update-dept {:params [[:next_transaction_id :ref-fn-key 'inc :transaction_id]]}
                           :delete-dept {:validation [[:id :type 'vector? "Id will be sequence"]
                                                      [:id :contain 'int? "Id contain will be Long "]]}}}]
          actual-result (r/do-compile w)]
      (is (not-empty (:insert-dept actual-result)))
      (is (not-empty (:update-dept actual-result)))
      (is (not-empty (:delete-dept actual-result)))
      (is (not-empty actual-result)))))


;(do-compile-test)

;(run-tests)

(deftest do-compile2-test
  (testing "test do-compile "
    (let [w [{:name         :_config_
              :file-reload  true
              :timeout      3000
              :reserve-name #{:create-ddl :drop-ddl :init-data}}
             {:doc        "Modify department"
              :name       [:insert-dept :update-dept :delete-dept ]
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
                                                      [:id :contain 'int? "Id contain will be Long "]]}}}]
          actual-result (r/do-compile w)]

      ;      (clojure.pprint/pprint (s/conform :dadysql.compiler.spec/spec w))

      (is (not-empty actual-result)))

    ))


;(do-compile2-test)



;(do-compile-test2)




;(run-tests)


;(compilter-test)



(deftest do-compile4-test
  (testing "test do -compile"
    (let [w (r/read-file "tie.edn2.sql" )]
      (clojure.pprint/pprint w)
      )
    ))


(comment

  (do
    (do-compile4-test)
    nil)

  )


