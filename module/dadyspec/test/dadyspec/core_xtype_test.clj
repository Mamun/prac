(ns dadyspec.core-xtype-test
  (:use [dadyspec.core-xtype]
        [clojure.test])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))


(comment


  (s/exercise :dadyspec.core-xtype/x-int)
  (s/exercise :dadyspec.core-xtype/x-inst)
  (s/exercise :dadyspec.core-xtype/x-double)
  (s/exercise :dadyspec.core-xtype/x-keyword)


  (count "asfd")
  )