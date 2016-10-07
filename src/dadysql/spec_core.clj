(ns dadysql.spec-core
  (:require [clojure.spec :as s]
            [dady.fail :as f]))


(s/def :dadysql.core/output any?)
;(defonce output-key :output)
;(defonce input-key :input)

(s/def :dadysql.core/input map?)

(s/def :dadysql.core/format-nested any?)
(s/def :dadysql.core/format-nested-array any?)
(s/def :dadysql.core/format-nested-join any?)
;(def nested-map-format :nested)
;(def nested-array-format :nested-array)
;(def nested-join-format :nested-join)

(s/def :dadysql.core/format-map any?)
;(def map-format :map)
(s/def :dadysql.core/format-array any?)
;(def array-format :array)
;(def value-format :value)
(s/def :dadysql.core/format-value any?)


(s/def :dadysql.core/output-format #{:dadysql.core/format-nested :dadysql.core/format-nested-array :dadysql.core/format-nested-join
                                     :dadysql.core/format-map :dadysql.core/format-array :dadysql.core/format-value})
(s/def :dadysql.core/input-format #{:dadysql.core/format-nested :dadysql.core/format-map})

#_(s/def :dadysql.core/input map?)

(s/def :dadysql.core/user-input (s/keys :req [(or :dadysql.core/name :dadysql.core/group)]
                                        :opt [:dadysql.core/input :dadysql.core/input-format :dadysql.core/output-format]))



(defn validate-input!
  [req-m]
  (if (s/valid? :dadysql.core/user-input req-m)
    req-m
    (f/fail (s/explain-data :dadysql.core/user-input req-m))))


(comment

  (s/valid? :dadysql.core/user-input {:dadysql.core/name [:get-employee-detail]
                                      :params            {:id 1}})


  (s/explain :dadysql.core/user-input {:dadysql.core/name         [:get-employee-detail]
                                       :group                     :load-dept
                                       :dadysql.core/param-format :map
                                       :params                    {}})

  )

