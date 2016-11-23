(ns dadymodel.walkthrough
  (:use [dadymodel.core])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))


(comment

  ;;Generate qualified, unqalified, string value spec
  ;;Generate
  (gen-spec :app '{:dept    {:req {:id   int?
                                   :name string?}
                             :opt {:note string?}}
                   :student {:req {:name string?
                                   :id   int?}}}
            {:dadymodel.core/join [[:dept :id :dadymodel.core/rel-1-n :student :dept-id]]})


  (defmodel app {:dept    {:req {:id   int?
                                 :name string?}
                           :opt {:note string?}}
                 :student {:req {:name string?
                                 :id   int?}}}
            :dadymodel.core/join
            [[:dept :id :dadymodel.core/rel-1-n :student :dept-id]])



  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :app/dept 1)))


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :app/dept-list 1)))

  ;;as entity map
  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :entity.app/dept 2)))


  (binding [s/*recursion-limit* 1]
    (clojure.pprint/pprint
      (s/exercise :unq.entity.app/dept 1)))


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :unq.entity.app/dept-list 1)))


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :unq.entity.app/student-list 1)))



  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :app/entity 1)))


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :ex.app/dept 1)))


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :ex.app/dept-list 1)))


  (binding [s/*recursion-limit* 0]
    (let [w (gen/sample (s/gen :unq.entity.app/dept) 1)]
      (clojure.pprint/pprint w)
      (-> w
           (first)
           (do-disjoin [[:dept :id :dadymodel.core/rel-1-n :student :dept-id]])
           (do-join [[:dept :id :dadymodel.core/rel-1-n :student :dept-id]])
           (clojure.pprint/pprint)
           ))
    )


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (let [w (gen/sample (s/gen :unq.entity.app/dept) 1)]
        (clojure.pprint/pprint w)
        (-> w
             (first)
             (do-disjoin [[:dept :id :dadymodel.core/rel-1-n :student :dept-id]])
             #_(do-join [[:dept :id :dadymodel.core/rel-1-n :student :dept-id]])
             ))))



  (let [w (->> {:dept
                {:id   -1,
                 :name "",
                 :note "",
                 :student-list
                       [{:name "", :id -1}
                        {:name "", :id -1}]}})
        j-value (do-disjoin w [[:dept :id :dadymodel.core/rel-1-n :student :dept-id]])
        dj-value (do-join j-value [[:dept :id :dadymodel.core/rel-1-n :student :dept-id]] )]
    (clojure.pprint/pprint j-value)
    (clojure.pprint/pprint dj-value)
    )

  )


