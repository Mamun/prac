(ns dadyspec.walkthrough
  (:use [dadyspec.core])
  (:require [cheshire.core :as ch]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]))


(comment

  (gen-spec :app '{:dept    {:req {:id   int?
                                   :name string?}
                             :opt {:note string?}}
                   :student {:req {:name string?
                                   :id   int}}}
            {:dadyspec.core/join     [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]]
             :dadyspec.core/gen-type #{:dadyspec.core/un-qualified }})


  (defentity app {:dept {:req {:id  int?
                              :name string?}
                        :opt  {:note string?}}
              :student  {:req {:name string?
                              :id   int?}}}
             :dadyspec.core/join
             [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]]
             :dadyspec.core/gen-type #{:dadyspec.core/qualified
                                   :dadyspec.core/un-qualified
                                   :dadyspec.core/ex})


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :app/dept 1)))


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :app/dept-list 1)))


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :entity.app/dept 2)))


  (binding [s/*recursion-limit* 1]
    (clojure.pprint/pprint
      (s/exercise :entity.un-app/dept 1)))


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :entity.un-app/dept-list 1)))


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :entity.un-app/student-list 1)))



  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :app/entity 1)))


  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :ex-app/dept 1)))

  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (s/exercise :ex-app/dept-list 1)))



  (binding [s/*recursion-limit* 0]
    (clojure.pprint/pprint
      (let [w (gen/sample (s/gen :entity.un-app/dept) 1)]
        (clojure.pprint/pprint w)
        (->> w
             (first)
             (do-disjoin [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]])))))


  (let [w (->> {:dept
                {:id -1,
                 :name "",
                 :note "",
                 :student-list
                 [{:name "", :id -1}
                  {:name "", :id -1}]}})
        j-value  (do-disjoin [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]] w)
        dj-value (do-join [[:dept :id :dadyspec.core/rel-1-n :student :dept-id]] j-value)
        ]
    ;(clojure.pprint/pprint j-value)
    (clojure.pprint/pprint dj-value)
    )

  )


