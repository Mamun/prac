(ns dadysql.compiler.schema
  (:require [schema.core :as s]
            [clojure.spec :as sp]
            [clojure.tools.logging :as log]))


(defn valid-spec [spec v]
  (try
    (s/validate (eval spec) v)
    (catch Exception e
      (log/error (pr-str spec))
      (log/error (pr-str v))
      (log/error (ex-data e))
      (throw e))))



(comment

  (let [sch {(schema.core/optional-key :skip)
             #{(schema.core/enum :validation :column :join)},
             (schema.core/optional-key :model)
             (schema.core/pred (clojure.core/fn [v__2041__auto__]
                                 (if (clojure.core/keyword? v__2041__auto__)
                                   true
                                   (clojure.core/every? clojure.core/keyword?
                                                        v__2041__auto__)))
                               (quote dadysql.plugin.common-impl/resolve-model?)),
             (schema.core/optional-key :join) [[(schema.core/one schema.core/Keyword "Source Data Model") (schema.core/one schema.core/Keyword "Source Model Id") (schema.core/one (schema.core/enum :1-n :1-1 :n-1 :n-n) "Relationship") (schema.core/one schema.core/Keyword "Dest Model") (schema.core/one schema.core/Keyword "Dest Model Id") (schema.core/optional [(schema.core/one schema.core/Keyword "Join Model ") (schema.core/one schema.core/Keyword "Join Model Id1") (schema.core/one schema.core/Keyword "Join Model Id2")] "JoinSingleNTNSchema")]], (schema.core/required-key :sql) (schema.core/both schema.core/Str (schema.core/pred (clojure.core/fn [v__2986__auto__] (clojure.core/not (clojure.string/blank? v__2986__auto__))) (quote dadysql.plugin.sql.bind-impl/not-blank?))), (schema.core/optional-key dadysql.constant/extend-meta-key) {schema.core/Keyword {(schema.core/optional-key :skip) #{(schema.core/enum :validation :column :join)}, (schema.core/optional-key :model) schema.core/Keyword, (schema.core/optional-key :timeout) schema.core/Int, (schema.core/optional-key :params) (schema.core/pred (clojure.core/fn [v__2391__auto__] (clojure.spec/valid? (clojure.core/eval (quote (clojure.spec/* (clojure.spec/alt :ref-gen (clojure.spec/tuple keyword? keyword? keyword?) :ref-fn-key (clojure.spec/tuple keyword? keyword? resolve keyword?) :ref-key (clojure.spec/tuple keyword? keyword? keyword?) :ref-con (clojure.spec/tuple keyword? keyword? number?))))) v__2391__auto__)) (quote dadysql.plugin.params.core/k-spec-spec-valid?)), (schema.core/optional-key :column) {schema.core/Keyword schema.core/Keyword}, (schema.core/optional-key :result) #{(schema.core/enum :array :single)}, (schema.core/optional-key :validation) (schema.core/pred (clojure.core/fn [v__2667__auto__] (clojure.spec/valid? (clojure.core/eval (quote (clojure.spec/* (clojure.spec/alt :type (clojure.spec/tuple keyword? keyword? resolve string?) :contain (clojure.spec/tuple keyword? keyword? resolve string?) :range (clojure.spec/tuple keyword? keyword? integer? integer? string?))))) v__2667__auto__)) (quote dadysql.plugin.validation.core/k-spec-spec-valid?))}}, (schema.core/optional-key :timeout) schema.core/Int, (schema.core/required-key :name) (schema.core/pred (clojure.core/fn [v__2040__auto__] (if (clojure.core/keyword? v__2040__auto__) true (clojure.core/every? clojure.core/keyword? v__2040__auto__))) (quote dadysql.plugin.common-impl/resolve-model?)), (schema.core/optional-key :params) (schema.core/pred (clojure.core/fn [v__2391__auto__] (clojure.spec/valid? (clojure.core/eval (quote (clojure.spec/* (clojure.spec/alt :ref-gen (clojure.spec/tuple keyword? keyword? keyword?) :ref-fn-key (clojure.spec/tuple keyword? keyword? resolve keyword?) :ref-key (clojure.spec/tuple keyword? keyword? keyword?) :ref-con (clojure.spec/tuple keyword? keyword? number?))))) v__2391__auto__)) (quote dadysql.plugin.params.core/k-spec-spec-valid?)), (schema.core/optional-key :column) {schema.core/Keyword schema.core/Keyword}, (schema.core/optional-key :doc) schema.core/Str, (schema.core/optional-key :commit) (schema.core/enum :all :any :none), (schema.core/optional-key :group) schema.core/Keyword, (schema.core/optional-key :result) #{(schema.core/enum :array :single)}, (schema.core/optional-key :validation) (schema.core/pred (clojure.core/fn [v__2667__auto__] (clojure.spec/valid? (clojure.core/eval (quote (clojure.spec/* (clojure.spec/alt :type (clojure.spec/tuple keyword? keyword? resolve string?) :contain (clojure.spec/tuple keyword? keyword? resolve string?) :range (clojure.spec/tuple keyword? keyword? integer? integer? string?))))) v__2667__auto__)) (quote dadysql.plugin.validation.core/k-spec-spec-valid?))}
        v {:doc "General select statement. Name is used to identify each query, Abstract timeout will override with timeout here  ", :name1 [:get-dept-list :get-dept-by-ids :get-employee-list :get-meeting-list :get-employee-meeting-list], :model [:department :department :employee :meeting :employee-meeting], :extend {:get-dept-by-ids {:validation [[:id :type vector? "Id will be sequence"] [:id :contain long? "Id contain will be Long "]], :result #{:array}}}, :timeout 5000, :result #{:array}, :params [[:limit :ref-con 10] [:offset :ref-con 0]], :skip #{:join}, :sql " select * from department LIMIT :limit OFFSET :offset; select * from department where id in (:id) ; select * from employee LIMIT :limit OFFSET :offset; select * from meeting LIMIT :limit OFFSET :offset; select * from employee_meeting LIMIT :limit OFFSET :offset;    "}
        ]
    (valid-spec sch v)
    )

  )