(ns dady.spec-test
  (:use [dady.spec]
        )
  (:require [clojure.spec :as s])
  )




(comment

  (comment


    (build-ns-keyword :a :b)
    (add-ns-to-keyword :hello :t :p)
    ;(add-ns-to-keyword :hello )


    (clojure.pprint/pprint
      (registry-by-namespace :tie))

    ;(add-fixed-key :spec :a )

    ;(eval 'int?)

    (->> {:get-by-id         {:id   (var int?)
                              :name (var string?)}
          :get-details-by-id {:id (var int?)}}
         (as-ns-key-format :tie)
         (build-spec-batch))


    (->> {:get-by-id   {:id (var int?) :id2 (var int?) :id3 (var int?) :id4 (var int?)}
          :get-by-name {:name (var string?)}
          }
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





    (->> (list (clojure.spec/def :emp.get-by-name/name #'clojure.core/string?)
               (clojure.spec/def :emp.get-by-id/id     #'clojure.core/int?)
               (clojure.spec/def :emp.get-by-id/pid     #'clojure.core/int?)
               ;(clojure.spec/def :emp.get-by-id/pid3     #'clojure.core/int?)
               #_(clojure.spec/def :emp.get-by-id/check   (clojure.spec/keys :req-un [:emp.get-by-id/id]))
               #_(clojure.spec/def :emp.get-by-name/check (clojure.spec/keys :req-un [:emp.get-by-name/name]))
               )
         (eval))



    (->> {:get-by-id         {:id (var int?) :name (var string?)}
          :get-details-by-id {:id (var int?)}}
         (map->spec :hello3)
         (eval-spec))

    (clojure.pprint/pprint
      (registry-by-namespace :hello3))

    (s/valid?
      (merge-spec (list :hello3.get-by-id/spec :hello3.get-details-by-id/spec))
      {:id 3 :name "asdf"}
      )

    (s/valid?
      (merge-spec (list :hello3.get-by-id/spec :hello3.get-details-by-id/spec))
      {:id 3 :name 3}
      )

    )

  )
