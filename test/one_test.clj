(ns one-test
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [one]))


;;;Not working
(gen/sample (s/gen :one/1))

:one/1
:one/a


;;working
(gen/sample (s/gen :one/a))