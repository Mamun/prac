(ns dady.spec-util-test
  (:use [dady.spec-util]
        [clojure.test])
  (:require [clojure.spec :as s]
            [clojure.walk :as w]))


(deftest create-ns-key-test
  (testing "testing add-ns-to-keyword "
    (are [a e] (= a e)
               (create-ns-key :a/b :hello) :a.b/hello
               (create-ns-key :hello :t) :hello/t
               (create-ns-key :t :p) :t/p)))

;(create-ns-key-test)




(deftest update-ns-key-test
  (testing "testing as-ns-key-format"
    (are [a e] (= a e)
               (convert-key-to-ns-key {:get-by-id {:id :a
                                          :name        :name}})
               {:get-by-id {:get-by-id/id   :a,
                            :get-by-id/name :name}}

               (convert-key-to-ns-key {:get-by-id {:id  :id
                                                  :name :name}
                              :get-details-by-id  {:id :id}})
               {:get-by-id
                {:get-by-id/id   :id
                 :get-by-id/name :name}
                :get-details-by-id
                {:get-details-by-id/id :id}})))


;(update-ns-key-test)


(deftest map->spec-test
  (testing "testing map->spec "
    (let [v {:get-by-id   {:id :id}
             :get-by-name {:name :name}}
          e-result '((clojure.spec/def
                       :emp/get-by-id
                       (clojure.spec/keys :req-un [:emp.get-by-id/id]))
                      (clojure.spec/def :emp.get-by-id/id :id)
                      (clojure.spec/def
                        :emp/get-by-name
                        (clojure.spec/keys :req-un [:emp.get-by-name/name]))
                      (clojure.spec/def :emp.get-by-name/name :name))
          a-result (map->spec :emp v)]
      (is (= e-result a-result)))))









;(map->spec-test)



(comment








    (=
      (list (clojure.spec/def :emp.get-by-id/spec (clojure.spec/keys :req-un [:emp.get-by-id/id]))
            (clojure.spec/def :emp.get-by-id/id #'clojure.core/int?)
            (clojure.spec/def :emp.get-by-name/spec (clojure.spec/keys :req-un [:emp.get-by-name/name]))
            (clojure.spec/def :emp.get-by-name/name #'clojure.core/string?))

      (list (clojure.spec/def :emp.get-by-id/spec (clojure.spec/keys :req-un [:emp.get-by-id/id]))
            (clojure.spec/def :emp.get-by-id/id #'clojure.core/int?)
            (clojure.spec/def :emp.get-by-name/spec (clojure.spec/keys :req-un [:emp.get-by-name/name]))
            (clojure.spec/def :emp.get-by-name/name #'clojure.core/string?)))




    (build-ns-keyword :a :b :c)


                                        ;(add-ns-to-keyword :hello



    (clojure.pprint/pprint
      (registry-by-namespace :work))


    (->> {:get-by-id         {:id   (var int?)
                              :name (var string?)}
          :get-details-by-id {:id (var int?)}}
         (map->spec :work)
         #_(eval-spec))


    (merge-spec2 (list :tie.get-details-by-id/spec
                       :tie.get-by-id/spec))


    (s/explain
      (eval
        (merge-spec2 (list :work/get-details-by-id
                           :work/get-by-id)))
      {:id 3})






    (s/explain
      (clojure.spec/merge
        :work/get-by-id
        (clojure.spec/keys :req-un [:work/get-details-by-id  ]))
      {:id   3
       :name "asdf"
       :get-details-by-id  {:name "sadf" :id 5}}
      )



    (s/explain
      (clojure.spec/merge
        :work/get-by-id
        (clojure.spec/keys :req-un [:work/get-details-by-id  ]))
      {:id   3
       :name "asdf"
       :get-details-by-id  {:name "sadf" :id 5}}
      )








    (->> {:get-by-id   {:id (var int?)}
          :get-by-name {:name (var string?)}}
         (map->spec :emp)
         ;(apply concat)
         ;(eval-spec)
         ;(eval)
         )

    (->>
      '(list (clojure.spec/def :emp.get-by-id/spec (clojure.spec/keys :req-un [:emp.get-by-id/id]))
             (clojure.spec/def :emp.get-by-id/id #'clojure.core/int?)
             (clojure.spec/def :emp.get-by-name/spec (clojure.spec/keys :req-un [:emp.get-by-name/name]))
             (clojure.spec/def :emp.get-by-name/name #'clojure.core/string?))
      (eval)
      )









    (s/valid?
      (merge-spec (list :hello3.get-by-id/spec :hello3.get-details-by-id/spec))
      {:id 3 :name "asdf"}
      )

    (s/valid?
      (merge-spec (list :hello3.get-by-id/spec :hello3.get-details-by-id/spec))
      {:id 3 :name 3}
      )

    )


