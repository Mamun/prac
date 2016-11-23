(ns dadymodel.xtype-test
  (:use [dadymodel.xtype]
        [clojure.test])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))


(comment


  (s/exercise :dadymodel.core-xtype/x-int)
  (s/exercise :dadymodel.core-xtype/x-inst)
  (s/exercise :dadymodel.core-xtype/x-double)
  (s/exercise :dadymodel.core-xtype/x-keyword)


  (count "asfd")
  )