(ns one
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))


(s/def ::a string?)
(s/def ::1 int?)

;;both are working
(gen/sample (s/gen ::1))
(gen/sample (s/gen ::a))

;(keyword "1")
;::1

;;Invalid token error from different namespace when specs are registered with number

;one.clj

(s/def ::a string?)
(s/def ::1 int?)

::1  ;Ok
::a  ;Ok



;one-test.clj

:one/1  ;; Error
:one/a  ;;Ok


;(gen/sample (s/gen ::1))
;(gen/sample (s/gen ::a))

