(ns dadysql.compiler.spec-test
  (:use [clojure.test]
        [dadysql.compiler.core]
        [dadysql.compiler.spec])
  (:require [clojure.spec :as s]
            [dadysql.compiler.file-reader :as f]))






;(s/explain :dadysql.compiler.spec/gname [:_global_])
;(s/explain :dadysql.compiler.spec/gname :_global_)


(deftest do-compile-test2
  (testing "test do-compile "
    (let [w [{:name         :_global_
                   :file-reload  true
                   :timeout      3000
                   :reserve-name #{:create-ddl :drop-ddl :init-data}}]

          r (s/conform :dadysql.compiler.spec/spec w)]
      (println (s/explain :dadysql.compiler.spec/spec r))
      (is (not= :clojure.spec/invalid r)))))


;(do-compile-test2)



(deftest do-compile-test
  (testing "test do-compile "
    (let [config {:name         :_config_
                  :file-reload  true
                  :timeout      3000
                  :reserve-name #{:create-ddl :drop-ddl :init-data}}
          module {:doc        "Modify department"
                  :name       [:insert-dept :update-dept :delete-dept]
                  :model      :department
                  :validation [[:id :type 'long? "Id will be Long"]]
                  :sql        "insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name);update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id;delete from department where id in (:id);"
                  :extend     {:insert-dept {:params  [[:transaction_id :ref-con 0]
                                                       [:transaction_id :ref-con 0]]
                                             :timeout 30}
                               :update-dept {:params [[:next_transaction_id :ref-fn-key 'inc :transaction_id]]}
                               :delete-dept {:validation [[:id :type 'vector? "Id will be sequence"]
                                                          [:id :contain 'long? "Id contain will be Long "]]}}}

          w [config module]
          r (s/conform :dadysql.compiler.spec/spec w)]
 ;     (clojure.pprint/pprint r)
      ;(clojure.pprint/pprint (as-map r))
      (is (not= :clojure.spec/invalid r)))))



;(do-compile-test)



(deftest do-compile-test3
  (testing "test do-compile "
    (let [module {:doc        "Modify department"
                  :name       [:insert-dept :update-dept :delete-dept]
                  :model      :department
                  :validation [[:id :type 'long? "Id will be Long"]]
                  :sql        "insert into department (id, transaction_id, dept_name) values (:id, :transaction_id, :dept_name);update department set dept_name=:dept_name, transaction_id=:next_transaction_id  where transaction_id=:transaction_id and id=:id;delete from department where id in (:id);"
                  :extend     {:insert-dept {:params  [[:transaction_id :ref-con 0]
                                                       [:transaction_id :ref-con 0]]
                                             :timeout 30}
                               :update-dept {:params [[:next_transaction_id :ref-fn-key 'inc :transaction_id]]}
                               :delete-dept {:validation [[:id :type 'vector? "Id will be sequence"]
                                                          [:id :contain 'long? "Id contain will be Long "]]}}}

          w [ module]
          r (s/conform :dadysql.compiler.spec/spec w)]
    ;  (clojure.pprint/pprint r)
      ;(clojure.pprint/pprint (as-map r))
      (is (not= :clojure.spec/invalid r)))))


;(do-compile-test3)






(deftest do-compile-file-test4
  (testing "test do compile file "
    (let [w (-> "tie.edn.sql"
                (f/tie-file-reader)
                (f/map-sql-tag)
                )
          r (s/conform :dadysql.compiler.spec/spec w)
          w1 (do-grouping w)
          config (-> w1
                (emit-config)
                (:config))]
      (is (not-empty config))
      #_(clojure.pprint/pprint w1)
      #_(clojure.pprint/pprint config))))




(do-compile-file-test4)





#_(->> "tie.edn.sql"
    (f/tie-file-reader)
     (f/map-sql-tag)
     (s/conform :dadysql.compiler.spec/spec )
     (as-map)
     )


#_(do-compile-file-test)



(comment




  (defn as-spec-keys [m]
    (->> (apply concat (seq m))
         (cons 's/keys)))


  ;(s/valid? :dadysql.compiler.spec/doc "adsfas")
  #_(defn defined-key [coll]
      (s/def ::hello (fn [v]

                       ))
      )



  (let [s1 {:req [::doc]}
        s3 {:opt [::timeout]}
        s (merge-with concat s1 s3)]

    (println (as-spec-keys s))

    (s/valid? (eval (as-spec-keys s)) {::timeout 455
                                       ::doc     "Hello"}))




  (defprotocol Spec
    (-get-spec [this] ""))


  (defrecord AB []
    Spec
    (-get-spec [this] string?))


  (defrecord BC []
    Spec
    (-get-spec [this] string?))


  (defrecord ABC [coll]
    Spec
    (-get-spec [this] string?))




  (-get-spec (AB.))

  )





