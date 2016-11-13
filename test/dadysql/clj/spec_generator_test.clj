(ns dadysql.clj.spec-generator-test
  (:require [clojure.spec :as s])
  (:use [clojure.test]
        [dadysql.clj.spec-generator]))



(deftest model->spec-test
  (testing "test model->spec"
    (let [v {:dept {:req {:name 'string?
                          :id   int?}}}
          sp (gen-spec :model v)]
      (clojure.pprint/pprint sp))

    ))



;(model->spec-test)
